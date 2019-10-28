package unit731.hunspeller.parsers.workers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.LineEntry;
import unit731.hunspeller.parsers.dictionary.RulesReducer;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.enums.AffixType;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.RuleEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.workers.core.WorkerData;
import unit731.hunspeller.parsers.workers.core.WorkerDictionaryBase;


public class RulesReducerWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducerWorker.class);

	private static final MessageFormat NON_EXISTENT_RULE = new MessageFormat("Non–existent rule ''{0}'', cannot reduce");

	public static final String WORKER_NAME = "Rules reducer";


	private final RulesReducer rulesReducer;


	public RulesReducerWorker(final String flag, final boolean keepLongestCommonAffix, final AffixData affixData,
			final DictionaryParser dicParser, final WordGenerator wordGenerator){
		Objects.requireNonNull(flag);
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(wordGenerator);

		rulesReducer = new RulesReducer(affixData, wordGenerator);

		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new IllegalArgumentException(NON_EXISTENT_RULE.format(new Object[]{flag}));

		final AffixType type = ruleToBeReduced.getType();

		final List<String> originalLines = new ArrayList<>();
		final List<LineEntry> originalRules = new ArrayList<>();
		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			final List<LineEntry> filteredRules = rulesReducer.collectProductionsByFlag(productions, flag, type);
			if(!filteredRules.isEmpty()){
				originalLines.add(line);
				originalRules.addAll(filteredRules);
			}
		};
		final Runnable completed = () -> {
			try{
				final List<LineEntry> compactedRules = rulesReducer.reduceRules(originalRules);

				final List<String> reducedRules = rulesReducer.convertFormat(flag, keepLongestCommonAffix, compactedRules);

				rulesReducer.checkReductionCorrectness(flag, reducedRules, originalLines);

				for(final String rule : reducedRules)
					LOGGER.info(Backbone.MARKER_RULE_REDUCER, rule);
			}
			catch(final Exception e){
				LOGGER.info(Backbone.MARKER_RULE_REDUCER, e.getMessage());

				e.printStackTrace();
			}
		};
		final WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

}