package unit731.hunspeller.parsers.dictionary.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionaryReadWrite extends WorkerBase<BufferedWriter, String>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerDictionaryReadWrite.class);

	private final File dicFile;
	private final File outputFile;


	public WorkerDictionaryReadWrite(String workerName, File dicFile, File outputFile, Charset charset,
			BiConsumer<BufferedWriter, String> lineProcessor, Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(outputFile);
		Objects.requireNonNull(charset);
		Objects.requireNonNull(lineProcessor);
		Objects.requireNonNull(lockable);

		this.workerName = workerName;
		this.dicFile = dicFile;
		this.outputFile = outputFile;
		this.charset = charset;
		this.readLineProcessor = lineProcessor;
		this.completed = completed;
		this.cancelled = cancelled;
		this.lockable = lockable;
	}

	@Override
	protected Void doInBackground() throws IOException{
		LOGGER.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file"
			+ (workerName != null? StringUtils.SPACE + HyphenationParser.EM_DASH + StringUtils.SPACE + workerName: StringUtils.EMPTY));

		watch.reset();

		lockable.acquireReadLock();

		setProgress(0);
		long totalSize = dicFile.length();
		try(
				LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), charset));
				BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), charset);
			){
			String line = br.readLine();
			if(line == null)
				throw new IllegalArgumentException("Dictionary file empty");

			long readSoFar = line.getBytes(charset).length + 2;

			//ignore any BOM marker on first line
			if(br.getLineNumber() == 1)
				line = FileHelper.clearBOMMarker(line);
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

			while((line = br.readLine()) != null){
				readSoFar += line.getBytes(charset).length + 2;

				line = DictionaryParser.cleanLine(line);
				if(!line.isEmpty()){
					try{
						readLineProcessor.accept(writer, line);
					}
					catch(Exception e){
						LOGGER.info(Backbone.MARKER_APPLICATION, "{} on line {}: {}", e.getMessage(), br.getLineNumber(), line);
						
						throw e;
					}
				}

				setProgress(Math.min((int)Math.ceil((readSoFar * 100.) / totalSize), 100));
			}

			if(!isCancelled()){
				watch.stop();

				setProgress(100);

				LOGGER.info(Backbone.MARKER_APPLICATION, "Dictionary file read successfully (it takes {})", watch.toStringMinuteSeconds());
			}
		}
		catch(Exception e){
			if(e instanceof ClosedChannelException)
				LOGGER.warn("Thread interrupted");
			else{
				String message = ExceptionHelper.getMessage(e);
				LOGGER.error("{}: {}", e.getClass().getSimpleName(), message);
			}

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped reading Dictionary file");

			cancel(true);
		}
		finally{
			lockable.releaseReadLock();
		}

		return null;
	}

}
