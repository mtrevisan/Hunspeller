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
import unit731.hunspeller.Backbone;
import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;


public class CompoundRulesWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Compound rules extractions";


	public CompoundRulesWorker(String compoundRule, Backbone backbone){
		Objects.requireNonNull(backbone);


		Map<String, Set<RuleProductionEntry>> compounds = new HashMap<>();
		BiConsumer<String, Integer> body = (line, row) -> {
			//collect words belonging to a compound rule
			List<RuleProductionEntry> productions = backbone.applyRules(line);
			for(RuleProductionEntry production : productions){
				Map<String, Set<RuleProductionEntry>> c = Arrays.stream(production.getContinuationFlags())
					.filter(backbone::isManagedByCompoundRule)
					.collect(Collectors.groupingBy(flag -> flag, Collectors.mapping(x -> production, Collectors.toSet())));

				for(Map.Entry<String, Set<RuleProductionEntry>> entry: c.entrySet()){
					String affix = entry.getKey();
					Set<RuleProductionEntry> prods = entry.getValue();
					Set<RuleProductionEntry> sub = compounds.get(affix);
					if(sub == null)
						compounds.put(affix, prods);
					else
						sub.addAll(prods);
				}
			}
//			for(RuleProductionEntry production : productions)
//				for(String affix : production.getContinuationFlags())
//					if(backbone.isManagedByCompoundRule(affix)){
//						if(!compounds.containsKey(affix)){
//							Set<RuleProductionEntry> list = new HashSet<>();
//							list.add(production);
//
//							compounds.put(affix, list);
//						}
//						else
//							compounds.get(affix).add(production);
//					}
		};
		Runnable done = () -> {
			if(!isCancelled()){
				//TODO extract compounds
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
