package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.languages.DictionaryBaseData;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class DictionaryInclusionTestWorker extends WorkerDictionaryReadBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryInclusionTestWorker.class);

	public static final String WORKER_NAME = "Dictionary inclusion test";

	private final BloomFilterInterface<String> dictionary;


	public DictionaryInclusionTestWorker(DictionaryParser dicParser, WordGenerator wordGenerator, DictionaryBaseData dictionaryBaseData,
			ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);

		dictionary = new ScalableInMemoryBloomFilter<>(dictionaryBaseData.getExpectedNumberOfElements(),
			dictionaryBaseData.getFalsePositiveProbability(), dictionaryBaseData.getGrowRatioWhenFull());
		dictionary.setCharset(dicParser.getCharset());

		BiConsumer<String, Integer> lineReader = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			productions.forEach(production -> dictionary.add(production.getWord()));
		};
		Runnable completed = () -> {
			dictionary.close();

			int totalUniqueProductions = dictionary.getAddedElements();
			double falsePositiveProbability = dictionary.getTrueFalsePositiveProbability();
			int falsePositiveCount = (int)Math.ceil(totalUniqueProductions * falsePositiveProbability);
			LOGGER.info(Backbone.MARKER_APPLICATION, "Total unique productions: {} ± {} ({})",
				DictionaryParser.COUNTER_FORMATTER.format(totalUniqueProductions),
				DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
				falsePositiveCount);
		};
		Runnable cancelled = () -> {
			dictionary.close();
		};
		createWorker(WORKER_NAME, dicParser, lineReader, completed, cancelled, lockable);
	}

	@Override
	public void clear(){
		dictionary.clear();
	}

	public boolean isInDictionary(String word){
		return dictionary.contains(word);
	}

}
