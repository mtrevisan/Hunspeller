package unit731.hunspeller.parsers.dictionary.workers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.services.regexgenerator.HunspellRegexWordGenerator;


@Slf4j
public class CompoundRulesWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Compound rules extractions";


	public CompoundRulesWorker(String compoundRule, Backbone backbone){
		Objects.requireNonNull(backbone);


		Map<String, Set<String>> compounds = new HashMap<>();
		BiConsumer<String, Integer> body = (line, row) -> {
			//collect words belonging to a compound rule
			List<RuleProductionEntry> productions = backbone.applyRules(line);
			for(RuleProductionEntry production : productions){
				Map<String, Set<String>> c = Arrays.stream(production.getContinuationFlags())
					.filter(backbone::isManagedByCompoundRule)
					.collect(Collectors.groupingBy(flag -> flag, Collectors.mapping(x -> production.getWord(), Collectors.toSet())));

				for(Map.Entry<String, Set<String>> entry: c.entrySet()){
					String affix = entry.getKey();
					Set<String> prods = entry.getValue();

					Set<String> sub = compounds.get(affix);
					if(sub == null)
						compounds.put(affix, prods);
					else
						sub.addAll(prods);
				}
			}
		};
		Runnable done = () -> {
			if(!isCancelled()){
				//extract values for the given compound rule
				Map<String, String> rule = compounds.entrySet().stream()
					.filter(entry -> backbone.isManagedByCompoundRule(compoundRule, entry.getKey()))
					.collect(Collectors.toMap(entry -> entry.getKey(), entry -> "(" + StringUtils.join(entry.getValue(), "|") + ")"));

				//compose compound rule
				String expandedCompoundRule = StringUtils.replaceEach(compoundRule, rule.keySet().toArray(new String[rule.size()]),
					rule.values().toArray(new String[rule.size()]));
				expandedCompoundRule = StringUtils.replaceEach(expandedCompoundRule, new String[]{"((", "))"}, new String[]{"(", ")"});
				System.out.println(expandedCompoundRule);

				HunspellRegexWordGenerator regexWordGenerator = new HunspellRegexWordGenerator(expandedCompoundRule);
				long wordCount = regexWordGenerator.wordCount();
				log.info(Backbone.MARKER_APPLICATION, "Total compounds: {}", (wordCount == HunspellRegexWordGenerator.INFINITY? '\u221E': wordCount));
				//generate all the words that matches the given regex
				long wordPrintedCount = (wordCount == HunspellRegexWordGenerator.INFINITY? 20l: Math.min(wordCount, 20l));
				List<String> words = regexWordGenerator.generateAll(wordPrintedCount);
				for(String word : words)
					log.info(Backbone.MARKER_APPLICATION, word);
				if(wordPrintedCount != wordCount)
					log.info(Backbone.MARKER_APPLICATION, "\u2026");
			}
		};
		createWorker(WORKER_NAME, backbone, body, done);
	}

	private Set<String> extractCompoundRuleAffixes(Backbone backbone, Productable productable){
		String[] affixes = productable.getContinuationFlags();

		Set<String> applyAffixes = new HashSet<>();
		if(affixes != null)
			for(String affix : affixes)
				if(backbone.isManagedByCompoundRule(affix))
					applyAffixes.add(affix);
		return applyAffixes;
	}

}
