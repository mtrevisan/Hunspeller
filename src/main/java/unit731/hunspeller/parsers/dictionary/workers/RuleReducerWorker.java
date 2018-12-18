package unit731.hunspeller.parsers.dictionary.workers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class RuleReducerWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Rule reducer";


	public RuleReducerWorker(AffixParser affParser, DictionaryParser dicParser, WordGenerator wordGenerator, ReadWriteLockable lockable){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(wordGenerator);

String flag = "&0";
		RuleEntry originalRuleEntry = (RuleEntry)affParser.getData(flag);
		boolean isSuffix = originalRuleEntry.isSuffix();
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		List<String> aliasesFlag = affParser.getData(AffixTag.ALIASES_FLAG);
		List<String> aliasesMorphologicalField = affParser.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
		Set<Pair<String, String>> newAffixEntries = new HashSet<>();
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag, aliasesMorphologicalField);
			dicEntry.applyConversionTable(affParser::applyInputConversionTable);

			if(dicEntry.hasContinuationFlag(flag)){
				String word = dicEntry.getWord();
				int wordLength = word.length();
				String lastLetter = word.substring(wordLength - 1);
				List<Production> productions = wordGenerator.applySingleAffixRule(word + "/" + flag);

				productions.forEach(production -> {
					String newAffixEntry;
					String condition;
					if(isSuffix){
						int lastCommonLetter;
						String producedWord = production.getWord();
						for(lastCommonLetter = 0; lastCommonLetter < Math.min(wordLength, producedWord.length()); lastCommonLetter ++)
							if(word.charAt(lastCommonLetter) != producedWord.charAt(lastCommonLetter))
								break;
						String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): AffixEntry.ZERO);
						String addition = producedWord.substring(lastCommonLetter);
						condition = (lastCommonLetter < wordLength? removal: lastLetter);
						newAffixEntry = composeLine(removal, addition);
					}
					else{
						int firstCommonLetter;
						String producedWord = production.getWord();
						for(firstCommonLetter = 0; firstCommonLetter < Math.min(wordLength, producedWord.length()); firstCommonLetter ++)
							if(word.charAt(firstCommonLetter) == producedWord.charAt(firstCommonLetter))
								break;
						String removal = (firstCommonLetter < wordLength? word.substring(0, firstCommonLetter): AffixEntry.ZERO);
						String addition = producedWord.substring(0, firstCommonLetter);
						condition = (firstCommonLetter < wordLength? removal: lastLetter);
						newAffixEntry = composeLine(removal, addition);
					}
					newAffixEntries.add(Pair.of(newAffixEntry, condition));
				});
			}
		};
		Runnable completed = () -> {
			//aggregate rules
			Set<Pair<String, String>> aggregatedAffixEntries = new HashSet<>();

			List<Pair<String, String>> entries = new ArrayList<>(newAffixEntries);
			//sort entries by shortest condition
			entries.sort((entry1, entry2) ->
				Integer.compare(entry1.getRight().length(), entry2.getRight().length())
			);
			while(!entries.isEmpty()){
				Pair<String, String> affixEntry = entries.get(0);
				String affixEntryCondition = affixEntry.getRight();

				//collect all the entries that have affixEntry as last part of the condition
				List<Pair<String, String>> collisions = new ArrayList<>();
				collisions.add(affixEntry);
				for(int i = 1; i < entries.size(); i ++){
					Pair<String, String> targetAffixEntry = entries.get(i);
					String targetAffixEntryCondition = targetAffixEntry.getRight();
					if(targetAffixEntryCondition.endsWith(affixEntryCondition))
						collisions.add(Pair.of(targetAffixEntry.getLeft(), targetAffixEntryCondition));
				}

				//remove matched entries
				collisions.forEach(entry -> entries.remove(entry));

				if(collisions.size() > 1){
					//generate regex from input
//					Map<Integer, List<Pair<String, String>>> bucket = bucketForLength(collisions);
//					int length = affixEntryCondition.length() + 1;
//					List<Pair<String, String>> list = bucket.get(length);
//					//strip affixEntry's condition and collect
//					String otherConditions = list.stream()
//						.map(Pair::getRight)
//						.map(condition -> condition.charAt(condition.length() - 2))
//						.map(String::valueOf)
//						.collect(Collectors.joining(StringUtils.EMPTY, "[^", "]"));
//					bucket.get(1).set(0, Pair.of(affixEntry.getLeft(), otherConditions + affixEntry.getRight()));

//TODO manage condition.length > 2 (òno with condition.charAt = n)
					Map<Integer, List<Pair<String, String>>> bucket = bucketForLength(collisions);
					Iterator<List<Pair<String, String>>> itr = bucket.values().iterator();
					List<Pair<String, String>> startingList = itr.next();
					while(itr.hasNext()){
						List<Pair<String, String>> nextList = itr.next();
						for(int i = 0; i < startingList.size(); i ++){
							String startingCondition = startingList.get(i).getRight();
							//strip affixEntry's condition and collect
							String otherConditions = nextList.stream()
								.map(Pair::getRight)
								.filter(condition -> condition.endsWith(startingCondition))
								.map(condition -> condition.charAt(condition.length() - 2))
								.map(String::valueOf)
								.collect(Collectors.joining(StringUtils.EMPTY, "[^", "]"));
							if(otherConditions.length() > 3)
								startingList.set(i, Pair.of(affixEntry.getLeft(), otherConditions + affixEntry.getRight()));
						}

						startingList = nextList;
					}
//TODO

System.out.print("collisions: ");
collisions.forEach(System.out::println);
//TODO
break;
				}
				else
					aggregatedAffixEntries.add(affixEntry);
				//TODO
				//if every one else does not ends with affixEntry, then accept, otherwise collect all that matches and modify accordingly

//				System.out.println(affixEntry);
			}
//System.out.println("--");
//aggregatedAffixEntries.forEach(System.out::println);
		};
		createReadParallelWorkerPreventExceptionRelaunch(WORKER_NAME, dicParser, lineProcessor, completed, null, lockable);
	}

	private Map<Integer, List<Pair<String, String>>> bucketForLength(List<Pair<String, String>> entries){
		Map<Integer, List<Pair<String, String>>> bucket = new HashMap<>();
		for(Pair<String, String> entry : entries)
			bucket.computeIfAbsent(entry.getRight().length(), k -> new ArrayList<>())
				.add(entry);
		return bucket;
	}

	public static String composeLine(String removal, String addition){
		StringBuilder sb = new StringBuilder();
		return sb.append(removal)
			.append(StringUtils.SPACE)
			.append(addition)
			.toString();
	}

	public static String composeLine(AffixEntry.Type type, String flag, String removal, String addition, String condition){
		StringBuilder sb = new StringBuilder();
		return sb.append(type.getFlag().getCode())
			.append(StringUtils.SPACE)
			.append(flag)
			.append(StringUtils.SPACE)
			.append(removal)
			.append(StringUtils.SPACE)
			.append(addition)
			.append(StringUtils.SPACE)
			.append(condition)
			.toString();
	}

}
