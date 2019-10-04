package unit731.hunspeller.services;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class ZipManager{

	public void zipDirectory(final File dir, final int compressionLevel, final String zipFilename) throws IOException{
		Files.deleteIfExists((new File(zipFilename)).toPath());

		final List<String> filesListInDir = extractFilesList(dir);

		//now zip files one by one
		final int startIndex = dir.getAbsolutePath().length() + 1;
		try(final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilename))){
			zos.setLevel(compressionLevel);

			for(final String filePath : filesListInDir){
				//for ZipEntry we need to keep only relative file path, so we used substring on absolute path
				zos.putNextEntry(new ZipEntry(filePath.substring(startIndex)));

				//read the file and write to ZipOutputStream
				try(final InputStream is = new FileInputStream(filePath)){
					IOUtils.copy(is, zos);

					//close the zip entry to write to zip file
					zos.closeEntry();
				}
			}
		}
	}

	private List<String> extractFilesList(final File dir){
		final List<String> filesListInDir = new ArrayList<>();

		final File[] files = dir.listFiles();
		for(final File file : files){
			if(file.isFile())
				filesListInDir.add(StringUtils.replace(file.getAbsolutePath(), "\\", "/"));
			else
				filesListInDir.addAll(extractFilesList(file));
		}

		return filesListInDir;
	}

	public static void zipFile(final File file, final int compressionLevel, final String zipFilename) throws IOException{
		zipStream(new FileInputStream(file), file.getName(), compressionLevel, zipFilename);
	}

	public static void zipStream(final InputStream entry, final String entryName, final int compressionLevel, final String zipFilename) throws IOException{
		//create ZipOutputStream to write to the zip file
		try(final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilename))){
			zos.setLevel(compressionLevel);

			//add a new Zip Entry to the ZipOutputStream
			zos.putNextEntry(new ZipEntry(entryName));

			//read the file and write to ZipOutputStream
			try(final InputStream is = new BufferedInputStream(entry)){
				IOUtils.copy(is, zos);
				
				//close the zip entry to write to zip file
				zos.closeEntry();
			}
		}
	}

}
