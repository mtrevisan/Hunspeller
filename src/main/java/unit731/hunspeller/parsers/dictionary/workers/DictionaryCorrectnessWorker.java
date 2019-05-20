package unit731.hunspeller.parsers.dictionary.workers;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.languages.DictionaryCorrectnessChecker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;


public class DictionaryCorrectnessWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Dictionary correctness checking";


	public DictionaryCorrectnessWorker(final DictionaryParser dicParser, final DictionaryCorrectnessChecker checker,
			final WordGenerator wordGenerator){
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(checker);

		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final List<Production> productions = wordGenerator.applyAffixRules(line);

			for(final Production production : productions){
				try{
					checker.checkProduction(production);
				}
				catch(final Exception e){
					throw wrapException(e, production);
				}
			}
		};
		final WorkerData data = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME, dicParser);
		createReadWorker(data, lineProcessor);
	}

	private IllegalArgumentException wrapException(final Exception e, final Production production){
		final StringBuffer sb = new StringBuffer(e.getMessage());
		if(production.hasProductionRules())
			sb.append(" (via ").append(production.getRulesSequence()).append(")");
		return new IllegalArgumentException(sb.toString());
	}

}
