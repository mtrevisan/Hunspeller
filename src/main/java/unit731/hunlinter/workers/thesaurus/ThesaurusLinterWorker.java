package unit731.hunlinter.workers.thesaurus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.thesaurus.SynonymsEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusDictionary;
import unit731.hunlinter.parsers.thesaurus.ThesaurusEntry;
import unit731.hunlinter.parsers.thesaurus.ThesaurusParser;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerThesaurus;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;


public class ThesaurusLinterWorker extends WorkerThesaurus{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusLinterWorker.class);

	public static final String WORKER_NAME = "Thesaurus linter";

	private static final MessageFormat MISSING_ENTRY = new MessageFormat("Thesaurus doesn''t contain definition {0} with part-of-speech {1} (from entry {2})");


	public ThesaurusLinterWorker(final ThesaurusParser theParser){
		super(new WorkerDataParser<>(WORKER_NAME, theParser)
			.withParallelProcessing(true)
		);

		final Consumer<IndexDataPair<ThesaurusEntry>> dataProcessor = indexData -> {
			final String originalDefinition = indexData.getData().getDefinition();
			//check if each part of `entry`, with appropriate PoS, exists
			final List<SynonymsEntry> syns = indexData.getData().getSynonyms();
			for(final SynonymsEntry syn : syns){
				final List<String> definitions = syn.getSynonyms();
				final String[] partOfSpeeches = syn.getPartOfSpeeches();
				for(String definition : definitions){
					definition = ThesaurusDictionary.removeSynonymUse(definition);
					//check also that the found PoS has `originalDefinition` among its synonyms
					if(!theParser.contains(definition, partOfSpeeches, originalDefinition))
						LOGGER.info(ParserManager.MARKER_APPLICATION, MISSING_ENTRY.format(
							new Object[]{definition, Arrays.toString(partOfSpeeches), originalDefinition}));
				}
			}
		};

		final Function<Void, List<IndexDataPair<ThesaurusEntry>>> step1 = ignored -> {
			prepareProcessing("Reading thesaurus file (step 1/2)");

			return readEntries();
		};
		final Function<List<IndexDataPair<ThesaurusEntry>>, Void> step2 = entries -> {
			prepareProcessing("Start processing " + workerData.getWorkerName());

			executeReadProcess(dataProcessor, entries);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

}
