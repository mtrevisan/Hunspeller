/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.services.system;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.services.downloader.DownloaderHelper;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public final class FileHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);

	private static final MessageFormat WRONG_FILE_FORMAT_CHARSET = new MessageFormat("The file is not in an allowable charset ({0})");


	private static final List<Charset> HUNSPELL_CHARSETS;
	static{
		HUNSPELL_CHARSETS = Stream.of(
				"UTF-8", "ISO_8859_1", "ISO_8859_2", "ISO_8859_3", "ISO_8859_4", "ISO_8859_5",
				"ISO_8859_6", "ISO_8859_7", "ISO_8859_8", "ISO_8859_9", "ISO_8859_10", "ISO_8859_13", "ISO_8859_14", "ISO_8859_15",
				"KOI8_R", "KOI8_U", "MICROSOFT_CP1251", "ISCII_DEVANAGARI")
			.map(name -> {
				Charset cs = null;
				try{
					cs = Charset.forName(name);
				}
				catch(final Exception ignored){}
				return cs;
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}


	private FileHelper(){}

	public static byte[] compressData(final byte[] bytes, final int level) throws IOException{
		if(bytes == null || bytes.length== 0)
			return new byte[0];

		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		try(final GZIPOutputStream gzip = new GZIPOutputStream(os, 2048){
			{
				def.setLevel(level);
			}
		}){
			gzip.write(bytes);
			os.close();
		}
		return os.toByteArray();
	}

	public static byte[] decompressData(final byte[] bytes) throws IOException{
		if(bytes == null || bytes.length == 0)
			return new byte[0];

		try(final GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(bytes))){
			return IOUtils.toByteArray(is);
		}
	}

	public static Charset readCharset(final String charsetName){
		try{
			final Charset cs = Charset.forName(charsetName);

			//line should be a valid charset
			if(!HUNSPELL_CHARSETS.contains(cs))
				throw new Exception();

			return cs;
		}
		catch(final Exception e){
			throw new LinterException(WRONG_FILE_FORMAT_CHARSET.format(new Object[]{charsetName}));
		}
	}

	public static Charset determineCharset(final Path path){
		for(final Charset cs : HUNSPELL_CHARSETS){
			try(final BufferedReader reader = Files.newBufferedReader(path, cs)){
				reader.read();
				return cs;
			}
			catch(final IOException ignored){}
		}

		final StringJoiner sj = new StringJoiner(", ");
		forEach(HUNSPELL_CHARSETS, charset -> sj.add(charset.name()));
		final String charsets = sj.toString();
		throw new IllegalArgumentException(WRONG_FILE_FORMAT_CHARSET.format(new Object[]{charsets}));
	}


	public static File createDeleteOnExitFile(final String filename, final String extension) throws IOException{
		final File file = File.createTempFile(filename, extension);
		file.deleteOnExit();
		return file;
	}

	public static File createDeleteOnExitFile(final String filename, final String extension, final byte[] bytes)
			throws IOException{
		final File file = createDeleteOnExitFile(filename, extension);
		Files.write(file.toPath(), bytes);
		return file;
	}

	public static File createDeleteOnExitFile(final String filename, final String extension, final String... lines)
			throws IOException{
		final StringJoiner sj = new StringJoiner(StringUtils.LF);
		for(final String line : lines)
			sj.add(line);
		final String content = sj.toString();

		final File file = createDeleteOnExitFile((filename != null? filename: "hunlinter-test"), extension);
		Files.writeString(file.toPath(), content);
		return file;
	}


	public static Scanner createScanner(final Path path, final Charset charset) throws IOException{
		return createScanner(path, charset, 2048);
	}

	public static Scanner createScanner(final Path path, final Charset charset, final int inputBufferSize)
			throws IOException{
		InputStream is = Files.newInputStream(path);
		if(isGZipped(path.toFile()))
			is = new GZIPInputStream(is, inputBufferSize);
		return createScanner(is, charset);
	}

	private static Scanner createScanner(final InputStream is, final Charset charset){
		final BOMInputStream bomis = new BOMInputStream(is, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE,
			ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE);
		return new Scanner(bomis, charset);
	}

	public static int getLinesCount(final File file, final Charset charset){
		int lines = 0;
		try(final LineNumberReader reader = new LineNumberReader(new FileReader(file, charset))){
			//skip to the end of file
			reader.skip(Integer.MAX_VALUE);
			lines = reader.getLineNumber() + 1;
		}
		catch(final IOException e){
			e.printStackTrace();
		}
		return lines;
	}

	public static long getFileSize(final File file){
		long size = -1l;
		if(isGZipped(file)){
			try(final RandomAccessFile raf = new RandomAccessFile(file, "r")){
				raf.seek(raf.length() - Integer.BYTES);
				final int n = raf.readInt();
				size = Integer.toUnsignedLong(Integer.reverseBytes(n));
			}
			catch(final Exception ignored){}
		}
		else
			size = file.length();
		return size;
	}

	public static boolean isGZipped(final File file){
		int magic = 0;
		try(final RandomAccessFile raf = new RandomAccessFile(file, "r")){
			magic = (raf.read() & 0xFF | ((raf.read() << 8) & 0xFF00));
		}
		catch(final Throwable ignored){}
		return (magic == GZIPInputStream.GZIP_MAGIC);
	}

	public static List<String> readAllLines(final Path path, final Charset charset) throws IOException{
		return readAllLines(path, charset, 2048);
	}

	public static List<String> readAllLines(final Path path, final Charset charset, final int inputBufferSize) throws IOException{
		final List<String> lines;
		if(isGZipped(path.toFile())){
			lines = new ArrayList<>();
			final Scanner scanner = createScanner(path, charset, inputBufferSize);
			while(scanner.hasNextLine())
				lines.add(scanner.nextLine());
		}
		else
			lines = Files.readAllLines(path, charset);
		return lines;
	}


	public static void saveFile(final Path path, final String lineTerminator, final Charset charset, final Iterable<String> content)
		throws IOException{
		try(final BufferedWriter writer = Files.newBufferedWriter(path, charset)){
			for(final String line : content){
				writer.write(line);
				writer.write(lineTerminator);
			}
		}
	}


	//https://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform
	public static boolean browse(File file) throws IOException, InterruptedException{
		if(file.isFile())
			file = file.getParentFile();

		//try using Desktop first
		if(executeDesktopCommand(Desktop.Action.OPEN, file))
			return true;

		//backup to system-specific
		ProcessBuilder builder = null;
		final String absolutePath = file.getAbsolutePath();
		if(SystemUtils.IS_OS_WINDOWS)
			builder = new ProcessBuilder("explorer", absolutePath);
		else if(SystemUtils.IS_OS_LINUX){
			if(runOSCommand(new ProcessBuilder("kde-open", absolutePath))
					|| runOSCommand(new ProcessBuilder("gnome-open", absolutePath))
					|| runOSCommand(new ProcessBuilder("xdg-open", absolutePath))
				)
				return true;
		}
		else if(SystemUtils.IS_OS_MAC)
			builder = new ProcessBuilder("open", absolutePath);
		else
			LOGGER.warn("Cannot issue command to open file {}, OS not recognized ({})", file.getName(), SystemUtils.OS_NAME);

		return (builder != null && runOSCommand(builder));
	}

	//https://stackoverflow.com/questions/526037/how-to-open-user-system-preferred-editor-for-given-file
	public static boolean openFileWithChosenEditor(final File file) throws IOException, InterruptedException{
		//system-specific
		ProcessBuilder builder = null;
		if(SystemUtils.IS_OS_WINDOWS)
			builder = new ProcessBuilder("rundll32.exe", "shell32.dll,OpenAs_RunDLL", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_LINUX)
			builder = new ProcessBuilder("edit", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_MAC)
			builder = new ProcessBuilder("open", file.getAbsolutePath());
		else
			LOGGER.warn("Cannot issue command to open file {}, OS not recognized ({})", file.getName(), SystemUtils.OS_NAME);

		return (builder != null && runOSCommand(builder));
	}

	public static boolean sendEmail(final String mailTo){
		return executeDesktopCommand(Desktop.Action.MAIL, mailTo);
	}

	public static boolean browseURL(final String url){
		return executeDesktopCommand(Desktop.Action.BROWSE, url);
	}

	private static boolean executeDesktopCommand(final Desktop.Action action, final Object parameter){
		boolean done = false;
		final Desktop desktop = getDesktopFor(action);
		try{
			switch(action){
				case OPEN:
					desktop.open((File)parameter);
					done = true;
					break;

				case BROWSE:
					if(DownloaderHelper.hasInternetConnectivity()){
						desktop.browse(new URI((String)parameter));
						done = true;
					}
					break;

				case MAIL:
					if(DownloaderHelper.hasInternetConnectivity()){
						desktop.mail(new URI((String)parameter));
						done = true;
					}
			}
		}
		catch(final Exception e){
			LOGGER.error("Cannot execute {} command", action, e);
		}
		return done;
	}

	private static Desktop getDesktopFor(final Desktop.Action action){
		return (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action)? Desktop.getDesktop(): null);
	}

	private static boolean runOSCommand(final ProcessBuilder builder) throws IOException, InterruptedException{
		boolean accomplished = false;
		if(builder != null){
			final Process process = builder.start();
			accomplished = (process.waitFor() == 0);
		}
		return accomplished;
	}

	public static void moveFile(final Path source, final Path target) throws IOException{
		if(SystemUtils.IS_OS_WINDOWS || Files.notExists(target))
			//for windows we can't go wrong because the OS manages locking
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		else{
			//let's unlink file first so we don't run into file-busy errors
			final Path temp = Files.createTempFile(target.getParent(), null, null);
			Files.move(target, temp, StandardCopyOption.REPLACE_EXISTING);

			try{
				Files.move(source, target);
			}
			catch(final IOException e){
				Files.move(temp, target);

				throw e;
			}
			finally{
				Files.deleteIfExists(temp);
			}
		}
	}

}
