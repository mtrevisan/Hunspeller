package unit731.hunlinter.workers.dictionary;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.collections.bloomfilter.BloomFilterInterface;
import unit731.hunlinter.collections.bloomfilter.BloomFilterParameters;
import unit731.hunlinter.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;


public class DictionaryInclusionTestWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryInclusionTestWorker.class);

	public static final String WORKER_NAME = "Dictionary inclusion test";

	private final BloomFilterInterface<String> dictionary;


	public DictionaryInclusionTestWorker(final String language, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

		Objects.requireNonNull(language);
		Objects.requireNonNull(wordGenerator);


		final BloomFilterParameters dictionaryBaseData = BaseBuilder.getDictionaryBaseData(language);
		dictionary = new ScalableInMemoryBloomFilter<>(dicParser.getCharset(), dictionaryBaseData);

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Production[] productions = wordGenerator.applyAffixRules(dicEntry);

			Arrays.stream(productions)
				.map(DictionaryEntry::getWord)
				.forEach(dictionary::add);
		};
		final Runnable completed = () -> {
			dictionary.close();

			final int totalUniqueProductions = dictionary.getAddedElements();
			final double falsePositiveProbability = dictionary.getTrueFalsePositiveProbability();
			final int falsePositiveCount = (int)Math.ceil(totalUniqueProductions * falsePositiveProbability);
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Total unique productions: {} ± {} ({})",
				DictionaryParser.COUNTER_FORMATTER.format(totalUniqueProductions),
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
				falsePositiveCount);
		};
		final Consumer<Exception> cancelled = exception -> dictionary.close();

		getWorkerData()
			.withDataCompletedCallback(completed)
			.withDataCancelledCallback(cancelled);

		final Function<Void, List<IndexDataPair<String>>> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			processLines(lineProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1);
	}

	public boolean isInDictionary(final String word){
		return dictionary.contains(word);
	}

}
