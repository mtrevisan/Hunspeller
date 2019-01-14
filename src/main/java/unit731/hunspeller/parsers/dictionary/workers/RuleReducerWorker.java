package unit731.hunspeller.parsers.dictionary.workers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;


public class RuleReducerWorker extends WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(RuleReducerWorker.class);

	public static final String WORKER_NAME = "Rule reducer";

	private static final String TAB = "\t";

	private static final String NOT_GROUP_STARTING = "[^";
	private static final String GROUP_STARTING = "[";
	private static final String GROUP_ENDING = "]";

	private static final RegExpSequencer SEQUENCER = new RegExpSequencer();


	private class LineEntry{
		private final List<String> originalWords;
		private final String removal;
		private final String addition;
		private String condition;

		LineEntry(String removal, String addition, String condition){
			this(removal, addition, condition, null);
		}

		LineEntry(String removal, String addition, String condition, String originalWord){
			originalWords = new ArrayList<>();
			originalWords.add(originalWord);
			this.removal = removal;
			this.addition = addition;
			this.condition = condition;
		}

		@Override
		public int hashCode(){
			return new HashCodeBuilder()
				.append(removal)
				.append(addition)
				.append(condition)
				.toHashCode();
		}

		@Override
		public boolean equals(Object obj){
			if(this == obj)
				return true;
			if(obj == null || getClass() != obj.getClass())
				return false;

			final LineEntry other = (LineEntry)obj;
			return new EqualsBuilder()
				.append(removal, other.removal)
				.append(addition, other.addition)
				.append(condition, other.condition)
				.isEquals();
		}
	}


	public RuleReducerWorker(AffixData affixData, DictionaryParser dicParser, WordGenerator wordGenerator){
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(wordGenerator);

String flag = "v1";
		RuleEntry originalRuleEntry = (RuleEntry)affixData.getData(flag);
		if(originalRuleEntry == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		List<LineEntry> flaggedEntries = new ArrayList<>();
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			collectFlagProductions(productions, flag, flaggedEntries);
		};
		Runnable completed = () -> {
			Map<String, List<LineEntry>> bucketedEntries = bucketByConditionEndingWithPlain(flaggedEntries);

			removeOverlappingRules(bucketedEntries);

//			List<LineEntry> reducedEntries = reduceEntriesToRules(bucketedEntries);

//			manageCollision(affixEntry, newAffixEntries);
/*
problem:
SFX v1 o ista/A2 [^i]o
SFX v1 o sta/A2 io
would be reduced to
SFX v1 o ista/A2 o
SFX v1 o sta/A2 o
how do I know the -o condition is not enough but it is necessary another character (-[^i]o, -io)?
if conditions are equals and the adding parts are one inside the other, take the shorter (sta/A2), add another char to the condition (io),
add the negated char to the other rule (ista/A2 [^i]o)
*/

System.out.println("");
			//aggregate rules
//			List<LineEntry> aggregatedAffixEntries = new ArrayList<>();
//			Comparator<String> comparator = BaseBuilder.getComparator(affixData.getLanguage());
//			while(!entries.isEmpty()){
//				LineEntry affixEntry = entries.get(0);
//
//				List<LineEntry> collisions = collectEntries(affixEntry, entries);
//
//				//remove matched entries
//				collisions.forEach(entry -> entries.remove(entry));
//
//				//if there are more than one collisions
//				if(collisions.size() > 1){
//					//bucket collisions by length
//					Map<Integer, List<LineEntry>> bucket = bucketForLength(collisions, comparator);
//
//					//generate regex from input:
//					//perform a one-leap step through the buckets
//					Iterator<List<LineEntry>> itr = bucket.values().iterator();
//					List<LineEntry> startingList = itr.next();
//					while(itr.hasNext()){
//						List<LineEntry> nextList = itr.next();
//						if(!nextList.isEmpty())
//							joinCollisions(startingList, nextList, comparator);
//
//						startingList = nextList;
//					}
//					//perform a two-leaps step through the buckets
//					itr = bucket.values().iterator();
//					startingList = itr.next();
//					if(itr.hasNext()){
//						List<LineEntry> intermediateList = itr.next();
//						while(itr.hasNext()){
//							List<LineEntry> nextList = itr.next();
//							if(!nextList.isEmpty())
//								joinCollisions(startingList, nextList, comparator);
//
//							startingList = intermediateList;
//							intermediateList = nextList;
//						}
//					}
//					//three-leaps? n-leaps?
//					//TODO
//
//					//store aggregated entries
//					bucket.values()
//						.forEach(aggregatedAffixEntries::addAll);
//				}
//				//otherwise store entry and pass to the next
//				else
//					aggregatedAffixEntries.add(affixEntry);
//			}

//			AffixEntry.Type type = (originalRuleEntry.isSuffix()? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX);
//			LOGGER.info(Backbone.MARKER_APPLICATION, composeHeader(type, flag, originalRuleEntry.isCombineable(), aggregatedAffixEntries.size()));
//			aggregatedAffixEntries.stream()
//				.map(entry -> composeLine(type, flag, entry))
//				.forEach(entry -> LOGGER.info(Backbone.MARKER_APPLICATION, entry));
		};
		WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	private void collectFlagProductions(List<Production> productions, String flag, List<LineEntry> newAffixEntries){
		Iterator<Production> itr = productions.iterator();
		//skip base production
		itr.next();
		while(itr.hasNext()){
			Production production = itr.next();

			AffixEntry lastAppliedRule = production.getLastAppliedRule();
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				String word = lastAppliedRule.undoRule(production.getWord());
				LineEntry affixEntry = (lastAppliedRule.isSuffix()? createSuffixEntry(production, word): createPrefixEntry(production, word));

				int index = newAffixEntries.indexOf(affixEntry);
				if(index >= 0)
					newAffixEntries.get(index).originalWords.add(word);
				else
					newAffixEntries.add(affixEntry);
			}
		}
	}

	private LineEntry createSuffixEntry(Production production, String word){
		int lastCommonLetter;
		int wordLength = word.length();
		String producedWord = production.getWord();
		for(lastCommonLetter = 0; lastCommonLetter < Math.min(wordLength, producedWord.length()); lastCommonLetter ++)
			if(word.charAt(lastCommonLetter) != producedWord.charAt(lastCommonLetter))
				break;

		String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): AffixEntry.ZERO);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): AffixEntry.ZERO);
		String condition = (lastCommonLetter < wordLength? removal: word.substring(wordLength - 1));
		return new LineEntry(removal, addition, condition, word);
	}

	private LineEntry createPrefixEntry(Production production, String word){
		int firstCommonLetter;
		int wordLength = word.length();
		String producedWord = production.getWord();
		for(firstCommonLetter = 0; firstCommonLetter < Math.min(wordLength, producedWord.length()); firstCommonLetter ++)
			if(word.charAt(firstCommonLetter) == producedWord.charAt(firstCommonLetter))
				break;

		String removal = (firstCommonLetter < wordLength? word.substring(0, firstCommonLetter): AffixEntry.ZERO);
		String addition = (firstCommonLetter > 0? producedWord.substring(0, firstCommonLetter): AffixEntry.ZERO);
		String condition = (firstCommonLetter < wordLength? removal: word.substring(wordLength - 1));
		return new LineEntry(removal, addition, condition, word);
	}

	private Map<String, List<LineEntry>> bucketByConditionEndingWithPlain(List<LineEntry> entries){
		sortByShortestCondition(entries);

		Map<String, List<LineEntry>> bucket = new HashMap<>();
		while(!entries.isEmpty()){
			//collect all entries that has the condition that ends with `condition`
			String condition = entries.get(0).condition;
			List<LineEntry> list = collectByCondition(entries, a -> a.endsWith(condition));

			bucket.put(condition, list);
		}
		return bucket;
	}

	private Map<String, LineEntry> bucketByConditionEndingWith(List<LineEntry> entries){
		sortByShortestCondition(entries);

		Map<String, LineEntry> bucket = new HashMap<>();
		while(!entries.isEmpty()){
			//collect all entries that has the condition that ends with `condition`
			String condition = entries.get(0).condition;
			List<LineEntry> list = collectByCondition(entries, a -> a.endsWith(condition));

			if(list.size() > 1){
				//find same condition entries
				Map<String, List<LineEntry>> equalsBucket = bucketByConditionEqualsTo(list);

				//expand same condition entries
				boolean expansionHappened = expandOverlappingRules(equalsBucket);

				//expand again if needed
				while(expansionHappened){
					expansionHappened = false;
					for(List<LineEntry> set : equalsBucket.values()){
						equalsBucket = bucketByConditionEqualsTo(set);

						//expand same condition entries
						expansionHappened |= expandOverlappingRules(equalsBucket);
					}
				}
				for(List<LineEntry> set : equalsBucket.values())
					for(LineEntry le : set)
						bucket.put(le.condition, le);
			}
			else
				bucket.put(condition, list.get(0));
		}
		return bucket;
	}

	private Map<String, List<LineEntry>> bucketByConditionEqualsTo(List<LineEntry> entries){
		Map<String, List<LineEntry>> bucket = new HashMap<>();
		while(!entries.isEmpty()){
			//collect all entries that has the condition that is `condition`
			String condition = entries.get(0).condition;
			List<LineEntry> list = collectByCondition(entries, a -> a.equals(condition));

			bucket.put(condition, list);
		}
		return bucket;
	}

	private List<LineEntry> collectByCondition(List<LineEntry> entries, Function<String, Boolean> comparator){
		List<LineEntry> list = new ArrayList<>();
		Iterator<LineEntry> itr = entries.iterator();
		while(itr.hasNext()){
			LineEntry entry = itr.next();
			if(comparator.apply(entry.condition)){
				itr.remove();

				list.add(entry);
			}
		}
		return list;
	}

	private boolean expandOverlappingRules(Map<String, List<LineEntry>> bucket){
		boolean expanded = false;
		for(List<LineEntry> entries : bucket.values())
			if(entries.size() > 1){
				//expand condition by one letter
				List<LineEntry> expandedEntries = new ArrayList<>();
				for(LineEntry en : entries)
					for(String originalWord : en.originalWords){
						int startingIndex = originalWord.length() - en.condition.length() - 1;
						String newCondition = originalWord.substring(startingIndex);
						LineEntry newEntry = new LineEntry(en.removal, en.addition, newCondition, originalWord);
						int index = expandedEntries.indexOf(newEntry);
						if(index >= 0)
							expandedEntries.get(index).originalWords.add(originalWord);
						else
							expandedEntries.add(newEntry);
					}
				entries.clear();
				entries.addAll(expandedEntries);

				expanded = true;
			}
		return expanded;
	}

	private void sortByShortestCondition(List<LineEntry> entries){
		entries.sort((entry1, entry2) -> Integer.compare(entry1.condition.length(), entry2.condition.length()));
	}

	private void sortWell(List<LineEntry> entries){
		Comparator<LineEntry> comparator = Comparator.comparing(entry -> entry.condition.length());
		comparator = comparator.thenComparing(Comparator.comparing(entry -> entry.condition));
		comparator = comparator.thenComparing(Comparator.comparing(entry -> entry.removal.length()));
		comparator = comparator.thenComparing(Comparator.comparing(entry -> entry.removal));
		entries.sort(comparator);
	}

	private void removeOverlappingRules(Map<String, List<LineEntry>> bucketedEntries){
		for(List<LineEntry> aggregatedRules : bucketedEntries.values())
			if(aggregatedRules.size() > 1){
				List<LineEntry> nonOverlappingRules = removeOverlappingConditions(aggregatedRules);

				aggregatedRules.clear();
				aggregatedRules.addAll(nonOverlappingRules);
			}
	}

	private List<LineEntry> removeOverlappingConditions(List<LineEntry> aggregatedRules){
		Comparator<String> comparator = BaseBuilder.getComparator("vec");

		//extract letters prior to first condition
		LineEntry firstRule = aggregatedRules.get(0);
		String firstCondition = firstRule.condition;
		int firstConditionLength = firstCondition.length();
		Set<Character> letters = new HashSet<>();
		int size = aggregatedRules.size();
		for(int index = 1; index < size; index ++){
			LineEntry entry = aggregatedRules.get(index);

			char[] additionalCondition = entry.condition.substring(0, entry.condition.length() - firstConditionLength).toCharArray();
			ArrayUtils.reverse(additionalCondition);

			//add letter additionalCondition.charAt(0) to [^...] * firstCondition
			letters.add(additionalCondition[0]);

			//add another rule(s) with [^additionalCondition.charAt(2)] * additionalCondition.charAt(1) * additionalCondition.charAt(0) * firstCondition
			String ongoingCondition = firstCondition;
			for(int i = 0; i < additionalCondition.length - 1; i ++){
				ongoingCondition = additionalCondition[i] + ongoingCondition;
				aggregatedRules.add(new LineEntry(firstRule.removal, firstRule.addition, NOT_GROUP_STARTING + additionalCondition[i + 1] + GROUP_ENDING
					+ ongoingCondition));
			}
		}
		List<String> sortedLetters = letters.stream().map(String::valueOf).collect(Collectors.toList());
		Collections.sort(sortedLetters, comparator);
		String addedCondition = StringUtils.join(sortedLetters, StringUtils.EMPTY);
		firstRule.condition = NOT_GROUP_STARTING + addedCondition + GROUP_ENDING + firstRule.condition;

		//TODO


//		Map<String, List<LineEntry>> bucket = bucketByConditionEqualsTo(aggregatedRules);
//
//		//resolve overlapping rules
//		for(Map.Entry<String, List<LineEntry>> entry : bucket.entrySet()){
//			List<LineEntry> entries = entry.getValue();
//			if(entries.size() > 1){
//				//expand condition by one letter
//				List<LineEntry> expandedEntries = new ArrayList<>();
//				for(LineEntry en : entries)
//					for(String originalWord : en.originalWords){
//						int startingIndex = originalWord.length() - en.condition.length() - 1;
//						String newCondition = originalWord.substring(startingIndex);
//						LineEntry newEntry = new LineEntry(en.removal, en.addition, newCondition, originalWord);
//						int index = expandedEntries.indexOf(newEntry);
//						if(index >= 0)
//							expandedEntries.get(index).originalWords.add(originalWord);
//						else
//							expandedEntries.add(newEntry);
//					}
//				entries.clear();
//				entries.addAll(expandedEntries);
//				//TODO
//
//				//bucket original words by letter prior to condition part
//				Map<String, List<LineEntry>> bucketPriorToCondition = bucketByLetter(entries, 1);
//				//TODO
//				sortByFewerOriginalWords(entries);
//
//				entries = reduceRules(entries);
//
//				entry.setValue(entries);
//			}
//		}
//
//		//retrieve list with non-overlapping rules
//		return bucket.values().stream()
//			.flatMap(List::stream)
//			.collect(Collectors.toList());
return aggregatedRules;
	}

//	private Map<String, List<LineEntry>> bucketByLetter(List<LineEntry> entries, int indexFromLast){
//		//TODO
//		Map<String, List<LineEntry>> bucket = new HashMap<>();
//		while(!entries.isEmpty()){
//			//collect all entries that has the condition that ends with `condition`
//			String condition = entries.get(0).condition;
//			List<LineEntry> list = collectByCondition(entries, a -> a.endsWith(condition));
//			bucket.put(condition, list);
//		}
//		return bucket;
//	}

//	private List<LineEntry> reduceRules(List<LineEntry> entries){
//		//get entry with fewer original words
//		LineEntry fewest = entries.get(0);
//
//		//extract the next letter(s)
//		int size = 1;
//		String addedLetter = extractPreviousLetter(fewest, size);
//
//		//attach next letter(s) to condition
//		String newCondition = addedLetter + fewest.condition;
//
//		//find if there are other rules that starts with newCondition
//		boolean found = false;
//		Iterator<LineEntry> itr = entries.iterator();
//		itr.next();
//		while(itr.hasNext())
//			if(itr.next().condition.endsWith(newCondition)){
//				found = true;
//				break;
//			}
//
//		if(!found){
//			//no rules ends with current condion, accept it as discriminant
//			fewest.condition = newCondition;
//
//			//transfer added letter(s) to other conditions
//			addedLetter = NOT_GROUP_STARTING + addedLetter + GROUP_ENDING;
//			itr = entries.iterator();
//			itr.next();
//			while(itr.hasNext()){
//				LineEntry current = itr.next();
//				current.condition = addedLetter + current.condition;
//			}
//		}
/*
a,ía,òda,ònia
r,èr
o,o > io,[^i]o / [^gƚsŧtx]o,[gƚsŧtx]o
*/
//		return entries;
//	}

//	private void sortByFewerOriginalWords(List<LineEntry> entries){
//		entries.sort((entry1, entry2) -> Integer.compare(entry1.originalWords.size(), entry2.originalWords.size()));
//	}

//	private String extractPreviousLetter(LineEntry entry, int size){
//		//TODO
//		//what if there are more than one originalWords?
//		String word = entry.originalWords.get(0);
//
//		int endingIndex = word.length() - entry.condition.length();
//		return word.substring(endingIndex - size, endingIndex);
//	}

	private List<LineEntry> reduceEntriesToRules(Map<String, List<LineEntry>> aggregatedFlaggedEntries){
		//TODO
return null;
	}

//	private void manageCollision(LineEntry affixEntry, Set<LineEntry> newAffixEntries){
//		//search newAffixEntries for collisions on condition
//		for(LineEntry entry : newAffixEntries)
//			if(entry.condition.equals(affixEntry.condition)){
//				//find last different character from entry.originalWord and affixEntry.originalWord
//				int idx;
//				for(idx = 1; idx <= Math.min(entry.originalWord.length(), affixEntry.originalWord.length()); idx ++)
//					if(entry.originalWord.charAt(entry.originalWord.length() - idx) != affixEntry.originalWord.charAt(affixEntry.originalWord.length() - idx))
//						break;
//				
//				if(entry.addition.endsWith(affixEntry.addition)){
//					//add another letter to the condition of entry
//					entry.condition = entry.originalWord.substring(entry.originalWord.length() - idx);
//					affixEntry.condition = NOT_GROUP_STARTING + entry.condition.charAt(0) + GROUP_ENDING + entry.condition.substring(1);
//				}
//				else{
//					//add another letter to the condition of affixEntry
//					affixEntry.condition = affixEntry.originalWord.substring(affixEntry.originalWord.length() - idx);
//					entry.condition = NOT_GROUP_STARTING + entry.condition.charAt(0) + GROUP_ENDING + affixEntry.condition.substring(1);
//				}
//				break;
//			}
//	}

//	private List<LineEntry> collectEntries(LineEntry affixEntry, List<LineEntry> entries){
//		//collect all the entries that have affixEntry as last part of the condition
//		String affixEntryCondition = affixEntry.condition;
//		Set<LineEntry> collisions = new HashSet<>();
//		collisions.add(affixEntry);
//		for(int i = 1; i < entries.size(); i ++){
//			LineEntry targetAffixEntry = entries.get(i);
//			String targetAffixEntryCondition = targetAffixEntry.condition;
//			if(targetAffixEntryCondition.endsWith(affixEntryCondition))
//				collisions.add(new LineEntry(targetAffixEntry.removal, targetAffixEntry.addition, targetAffixEntryCondition));
//		}
//		return new ArrayList<>(collisions);
//	}

//	private void joinCollisions(List<LineEntry> startingList, List<LineEntry> nextList, Comparator<String> comparator){
//		//extract the prior-to-last letter
//		int size = startingList.size();
//		for(int i = 0; i < size; i ++){
//			LineEntry affixEntry = startingList.get(i);
//			String affixEntryCondition = affixEntry.condition;
//			String[] startingCondition = RegExpSequencer.splitSequence(affixEntryCondition);
//			int discriminatorIndex = startingCondition.length;
//			String affixEntryRemoval = affixEntry.removal;
//			String affixEntryAddition = affixEntry.addition;
//			//strip affixEntry's condition and collect
//			List<String> otherConditions = nextList.stream()
//				.map(entry -> entry.condition)
//				.map(RegExpSequencer::splitSequence)
//				.filter(condition -> SEQUENCER.endsWith(condition, startingCondition))
//				.map(condition -> condition[condition.length - discriminatorIndex])
//				.distinct()
//				.map(String::valueOf)
//				.collect(Collectors.toList());
//			if(!otherConditions.isEmpty()){
//				Stream<String> other;
//				//if this condition.length > startingCondition.length + 1, then add in-between rules
//				if(discriminatorIndex > 0 && discriminatorIndex + 1 == SEQUENCER.length(startingCondition)){
//					//collect intermediate letters
//					Collection<String> letterBucket = bucketForLetter(nextList, discriminatorIndex, comparator);
//
//					for(String letter : letterBucket)
//						startingList.add(new LineEntry(affixEntryRemoval, affixEntryAddition, letter));
//
//					//merge conditions
//					Stream<String> startingConditionStream = Arrays.stream(startingCondition[discriminatorIndex - 1].substring(2,
//						startingCondition[discriminatorIndex - 1].length() - 1).split(StringUtils.EMPTY));
//					other = Stream.concat(startingConditionStream, otherConditions.stream())
//						.distinct();
//					affixEntryCondition = StringUtils.join(ArrayUtils.remove(startingCondition, 0));
//				}
//				else if(discriminatorIndex + 1 > SEQUENCER.length(startingCondition)){
//					//collect intermediate letters
//					Collection<String> letterBucket = bucketForLetter(nextList, discriminatorIndex, comparator);
//
//					for(String letter : letterBucket)
//						startingList.add(new LineEntry(affixEntryRemoval, affixEntryAddition, letter));
//
//					other = otherConditions.stream();
//				}
//				else{
//					other = otherConditions.stream();
//				}
//
//				String otherCondition = other
//					.sorted(comparator)
//					.collect(Collectors.joining());
//				startingList.set(i, new LineEntry(affixEntryRemoval, affixEntryAddition, NOT_GROUP_STARTING + otherCondition + GROUP_ENDING
//					+ affixEntryCondition));
//			}
//		}
//	}

//	private Map<Integer, List<LineEntry>> bucketForLength(List<LineEntry> entries, Comparator<String> comparator){
//		Map<Integer, List<LineEntry>> bucket = new HashMap<>();
//		for(LineEntry entry : entries){
//			int entryLength = entry.condition.length() - (entry.condition.startsWith(NOT_GROUP_STARTING)? 3:
//				(entry.condition.startsWith(GROUP_STARTING)? 2: 0));
//			bucket.computeIfAbsent(entryLength, k -> new ArrayList<>())
//				.add(entry);
//		}
//		//order lists
//		Comparator<LineEntry> comp = (pair1, pair2) -> comparator.compare(pair1.condition, pair2.condition);
//		for(Map.Entry<Integer, List<LineEntry>> bag : bucket.entrySet())
//			bag.getValue().sort(comp);
//		return bucket;
//	}

//	private Collection<String> bucketForLetter(List<LineEntry> entries, int index, Comparator<String> comparator){
//		//collect by letter at given index
//		Map<String, String> bucket = new HashMap<>();
//		for(LineEntry entry : entries){
//			String condition = entry.condition;
//			String key = String.valueOf(condition.charAt(index));
//			String bag = bucket.get(key);
//			if(bag != null)
//				condition = Arrays.asList(bag.charAt(index - 1), condition.charAt(index - 1)).stream()
//					.map(String::valueOf)
//					.sorted(comparator)
//					.collect(Collectors.joining(StringUtils.EMPTY, NOT_GROUP_STARTING, GROUP_ENDING))
//					+ condition.substring(index);
//			bucket.put(key, condition);
//		}
//
//		//convert non-merged conditions
//		List<String> valuesBucket = bucket.values().stream()
//			.map(condition -> (condition.charAt(0) == '['? condition: NOT_GROUP_STARTING + condition.charAt(0) + GROUP_ENDING + condition.substring(index)))
//			.collect(Collectors.toList());
//		bucket.clear();
//
//		//merge non-merged conditions
//		for(String condition : valuesBucket){
//			int idx = condition.indexOf(GROUP_ENDING);
//			String key = condition.substring(0, idx + 1);
//			String bag = bucket.get(key);
//			if(bag != null){
//				String[] letters = (bag.charAt(idx + 1) == '['?
//					bag.substring(idx + 2, bag.indexOf(']', idx + 2)).split(StringUtils.EMPTY):
//					new String[]{String.valueOf(bag.charAt(idx + 1))});
//				letters = ArrayUtils.add(letters, String.valueOf(condition.charAt(idx + 1)));
//				condition = key
//					+ Arrays.asList(letters).stream()
//					.sorted(comparator)
//					.collect(Collectors.joining(StringUtils.EMPTY, GROUP_STARTING, GROUP_ENDING))
//					+ condition.substring(idx + 2);
//			}
//			bucket.put(key, condition);
//		}
//		return bucket.values();
//	}

//	private String composeHeader(AffixEntry.Type type, String flag, boolean isCombineable, int size){
//		StringBuilder sb = new StringBuilder();
//		return sb.append(type.getFlag().getCode())
//			.append(StringUtils.SPACE)
//			.append(flag)
//			.append(StringUtils.SPACE)
//			.append(isCombineable? RuleEntry.COMBINEABLE: RuleEntry.NOT_COMBINEABLE)
//			.append(StringUtils.SPACE)
//			.append(size)
//			.toString();
//	}

//	private String composeLine(AffixEntry.Type type, String flag, LineEntry partialLine){
//		StringBuilder sb = new StringBuilder();
//		sb.append(type.getFlag().getCode())
//			.append(StringUtils.SPACE)
//			.append(flag)
//			.append(StringUtils.SPACE)
//			.append(partialLine.removal);
//		int idx = partialLine.addition.indexOf(TAB);
//		if(idx >= 0)
//			sb.append(partialLine.addition.substring(0, idx))
//				.append(StringUtils.SPACE)
//				.append(partialLine.condition)
//				.append(TAB)
//				.append(partialLine.addition.substring(idx + 1));
//		else
//			sb.append(partialLine.addition)
//				.append(StringUtils.SPACE)
//				.append(partialLine.condition);
//		return sb.toString();
//	}

}
