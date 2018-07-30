package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.externalsorter.ExternalSorterOptions;


@AllArgsConstructor
@Slf4j
public class SorterWorker extends SwingWorker<Void, String>{

	private final Backbone backbone;
	private final int lineIndex;


	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		try{
			publish("Sorting Dictionary file");
			setProgress(0);

			//extract boundaries from the file (from comment to comment, or blank line)
			backbone.calculateDictionaryBoundaries();

			setProgress(20);

			Map.Entry<Integer, Integer> boundary = backbone.getDictionaryBoundary(lineIndex);
			if(boundary != null){
				backbone.stopFileListener();

				//split dictionary isolating the sorted section
				List<File> chunks = splitDictionary(boundary);

				setProgress(40);

				//sort the chosen section
				File sortSection = chunks.get(1);
				ExternalSorterOptions options = ExternalSorterOptions.builder()
					.charset(backbone.getCharset())
					.comparator(ComparatorBuilder.getComparator(backbone.getLanguage()))
					.useZip(true)
					.removeDuplicates(true)
					.build();
				backbone.getDictionarySorter().sort(sortSection, options, sortSection);

				setProgress(60);

				//re-merge dictionary
				mergeDictionary(chunks);

				setProgress(80);

				//remove temporary files
				chunks.forEach(File::delete);

				publish("File sorted");

				backbone.clearDictionaryBoundaries();

				backbone.startFileListener();
			}
			else
				publish("File NOT sorted");

			setProgress(100);
		}
		catch(Exception e){
			stopped = true;

			if(e instanceof ClosedChannelException)
				publish("Duplicates thread interrupted");
			else{
				String message = ExceptionService.getMessage(e);
				publish(e.getClass().getSimpleName() + ": " + message);
			}
		}
		if(stopped)
			publish("Stopped reading Dictionary file");

		return null;
	}

	private List<File> splitDictionary(Map.Entry<Integer, Integer> boundary) throws IOException{
		int index = 0;
		List<File> files = new ArrayList<>();
		File file = File.createTempFile("split", ".out");
		try(BufferedReader br = Files.newBufferedReader(backbone.getDictionaryFile().toPath(), backbone.getCharset())){
			BufferedWriter writer = Files.newBufferedWriter(file.toPath(), backbone.getCharset());
			String line;
			while((line = br.readLine()) != null){
				if(index == boundary.getKey() || index == boundary.getValue() + 1){
					writer.close();

					files.add(file);

					file = File.createTempFile("split", ".out");
					writer = Files.newBufferedWriter(file.toPath(), backbone.getCharset());
				}

				writer.write(line);
				writer.newLine();

				index ++;
			}

			writer.close();

			files.add(file);
		}
		return files;
	}

	private void mergeDictionary(List<File> files) throws IOException{
		OpenOption option = StandardOpenOption.TRUNCATE_EXISTING;
		for(File file : files){
			Files.write(backbone.getDictionaryFile().toPath(), Files.readAllBytes(file.toPath()), option);

			option = StandardOpenOption.APPEND;
		}
	}

	@Override
	protected void process(List<String> chunks){
		for(String chunk : chunks)
			log.info(Backbone.MARKER_APPLICATION, chunk);
	}

}

