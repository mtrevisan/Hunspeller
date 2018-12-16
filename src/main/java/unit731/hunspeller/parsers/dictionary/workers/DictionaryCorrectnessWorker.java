package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.languages.DictionaryCorrectnessChecker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class DictionaryCorrectnessWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Dictionary correctness checking";


	public DictionaryCorrectnessWorker(DictionaryParser dicParser, DictionaryCorrectnessChecker checker, WordGenerator wordGenerator, ReadWriteLockable lockable){
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(checker);

		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			productions.forEach(production -> checker.checkProduction(production));
		};
		createReadParallelWorkerPreventExceptionRelaunch(WORKER_NAME, dicParser, lineProcessor, null, null, lockable);
	}

}