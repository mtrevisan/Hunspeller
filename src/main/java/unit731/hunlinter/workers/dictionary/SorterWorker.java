package unit731.hunlinter.workers.dictionary;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;


public class SorterWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(SorterWorker.class);

	public static final String WORKER_NAME = "Sorting";

	private static final byte[] NEW_LINE = {'\r', '\n'};

	private final DictionaryParser dicParser;
	private final File dicFile;

	private final Comparator<String> comparator;


	public SorterWorker(final Packager packager, final ParserManager parserManager, final int lineIndex){
		super(new WorkerDataParser<>(WORKER_NAME, parserManager.getDicParser()));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		dicFile = packager.getDictionaryFile();
		dicParser = parserManager.getDicParser();
		final Charset charset = dicParser.getCharset();

		comparator = BaseBuilder.getComparator(parserManager.getAffixData().getLanguage());
		final Map.Entry<Integer, Pair<Integer, Integer>> boundary = dicParser.getBoundary(lineIndex);

		final Function<Void, String[]> step1 = ignored -> {
			prepareProcessing("Splitting dictionary file (step 1/3)");

			String[] chunk = null;
			if(boundary != null){
				parserManager.stopFileListener();

				//split dictionary isolating the sorted section
				chunk = extractSection(boundary, charset);

				setProgress(33);
			}
			return chunk;
		};
		final Function<String[], String[]> step2 = chunk -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Sort selected section (step 2/3)");

			//sort the chosen section
			Arrays.sort(chunk, comparator);

			setProgress(67);

			return chunk;
		};
		final Function<String[], Void> step3 = chunk -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Merge sections (step 3/3)");

			//re-merge section
			mergeSectionsToDictionary(dicFile, chunk, boundary.getValue().getKey(), charset);

			dicParser.clear();

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3));
	}

	private String[] extractSection(final Map.Entry<Integer, Pair<Integer, Integer>> boundary, final Charset charset){
		try(final RandomAccessFile accessor = new RandomAccessFile(dicParser.getDicFile(), "r")){
			//skip to begin of chunk
			accessor.seek(boundary.getValue().getKey());

			//read lines
			final String[] chunk = new String[boundary.getValue().getValue() - boundary.getKey() + 1];
			for(int index = 0; index < chunk.length; index ++)
				chunk[index] = new String(accessor.readLine().getBytes(StandardCharsets.ISO_8859_1), charset);
			return chunk;
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	private void mergeSectionsToDictionary(final File dicFile, final String[] chunk, final int startIndex, final Charset charset){
		try(final RandomAccessFile accessor = new RandomAccessFile(dicFile, "rwd")){
			accessor.seek(startIndex);
			for(final String line : chunk){
				accessor.write(line.getBytes(charset));
				accessor.write(NEW_LINE);
			}
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

}

