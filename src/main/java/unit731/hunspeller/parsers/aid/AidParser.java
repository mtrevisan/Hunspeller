package unit731.hunspeller.parsers.aid;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import unit731.hunspeller.services.FileHelper;


public class AidParser{

	private final List<String> lines = new ArrayList<>();


	/**
	 * Parse the rows out from a .aid file.
	 *
	 * @param aidFile	The content of the dictionary file
	 * @throws IOException	If an I/O error occurs
	 */
	public void parse(final File aidFile) throws IOException{
		lines.clear();

		final Path path = aidFile.toPath();
		final Charset charset = FileHelper.determineCharset(path);
		try(final LineNumberReader br = FileHelper.createReader(path, charset)){
			br.lines()
				.filter(Predicate.not(String::isEmpty))
				.forEach(lines::add);
		}
	}

	public void clear(){
		lines.clear();
	}

	public List<String> getLines(){
		return lines;
	}

}
