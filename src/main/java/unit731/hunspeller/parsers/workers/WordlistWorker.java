package unit731.hunspeller.parsers.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.workers.core.WorkerData;
import unit731.hunspeller.parsers.workers.core.WorkerDictionaryBase;
import unit731.hunspeller.parsers.workers.exceptions.HunspellException;
import unit731.hunspeller.services.FileHelper;


public class WordlistWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordCountWorker.class);

	public static final String WORKER_NAME = "Wordlist";

	public enum WorkerType{COMPLETE, PLAN_WORDS, MORFOLOGIK}


	public WordlistWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final WorkerType type,
			final File outputFile){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		final Function<Production, String> toString;
		switch(type){
			case COMPLETE:
				toString = Production::toString;
				break;

			case MORFOLOGIK:
				toString = Production::toStringMorfologik;
				break;

			case PLAN_WORDS:
			default:
				toString = Production::getWord;
		}
		final BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor = (writer, line) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line.getValue());
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			try{
				for(final Production production : productions){
					writer.write(toString.apply(production));
					writer.newLine();
				}
			}
			catch(final IOException e){
				throw new HunspellException(e.getMessage());
			}
		};
		final Runnable completed = () -> {
			LOGGER.info(Backbone.MARKER_APPLICATION, "File written: {}", outputFile.getAbsolutePath());

			try{
				FileHelper.openFileWithChosenEditor(outputFile);
			}
			catch(final IOException | InterruptedException e){
				LOGGER.warn("Exception while opening the resulting file", e);
			}
		};
		final WorkerData data = WorkerData.create(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createWriteWorker(data, lineProcessor, outputFile);
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

}
