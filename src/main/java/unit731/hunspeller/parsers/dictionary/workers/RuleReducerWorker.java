package unit731.hunspeller.parsers.dictionary.workers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
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

	private static final RegExpSequencer SEQUENCER = new RegExpSequencer();

	private static final String TAB = "\t";
	private static final String DOT = ".";
	private static final String SLASH = "/";
	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	private static final String GROUP_END = "]";


	private static class LineEntry implements Serializable{

		private static final long serialVersionUID = 8374397415767767436L;

		private final Set<String> from;

		private final String removal;
		private String addition;
		private String condition;


		public static LineEntry createFrom(LineEntry entry, String condition){
			return new LineEntry(entry.removal, entry.addition, condition);
		}

		public static LineEntry createFrom(LineEntry entry, String condition, Collection<String> words){
			return new LineEntry(entry.removal, entry.addition, condition, words);
		}

		LineEntry(String removal, String addition, String condition){
			this(removal, addition, condition, Collections.<String>emptyList());
		}

		LineEntry(String removal, String addition, String condition, String word){
			this(removal, addition, condition, Arrays.asList(word));
		}

		LineEntry(String removal, String addition, String condition, Collection<String> words){
			this.removal = removal;
			this.addition = addition;
			this.condition = condition;

			from = new HashSet<>();
			if(words != null)
				from.addAll(words);
		}

		public List<LineEntry> split(AffixEntry.Type type){
			List<LineEntry> split = new ArrayList<>();
			if(type == AffixEntry.Type.SUFFIX)
				for(String f : from){
					int index = f.length() - condition.length() - 1;
					if(index < 0)
						throw new IllegalArgumentException("Cannot reduce rule, should be splitted further because of '" + f + "'");

					split.add(new LineEntry(removal, addition, f.substring(index), f));
				}
			else
				for(String f : from){
					int index = condition.length() + 1;
					if(index == f.length())
						throw new IllegalArgumentException("Cannot reduce rule, should be splitted further because of '" + f + "'");

					split.add(new LineEntry(removal, addition, f.substring(0, index), f));
				}
			return split;
		}

		@Override
		public String toString(){
			return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
				.append("from", from)
				.append("rem", removal)
				.append("add", addition)
				.append("cond", condition)
				.toString();
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


	private FlagParsingStrategy strategy;
	private Comparator<String> comparator;
	private final Comparator<LineEntry> shortestConditionComparator = Comparator.comparingInt((LineEntry entry) -> entry.condition.length())
		.thenComparing(Comparator.comparingInt((LineEntry entry) -> (entry.addition.contains(SLASH)? entry.addition.indexOf(SLASH): entry.addition.length())).reversed());
	private Comparator<LineEntry> lineEntryComparator;


	public RuleReducerWorker(String flag, boolean keepLongestCommonAffix, AffixData affixData, DictionaryParser dicParser, WordGenerator wordGenerator){
		Objects.requireNonNull(flag);
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(wordGenerator);

		strategy = affixData.getFlagParsingStrategy();
		comparator = BaseBuilder.getComparator(affixData.getLanguage());
		lineEntryComparator = Comparator.comparingInt((LineEntry entry) -> RegExpSequencer.splitSequence(entry.condition).length)
			.thenComparing(Comparator.comparingInt(entry -> StringUtils.countMatches(entry.condition, GROUP_END)))
			.thenComparing(Comparator.comparing(entry -> StringUtils.reverse(entry.condition), comparator))
			.thenComparing(Comparator.comparingInt(entry -> entry.removal.length()))
			.thenComparing(Comparator.comparing(entry -> entry.removal, comparator))
			.thenComparing(Comparator.comparingInt(entry -> entry.addition.length()))
			.thenComparing(Comparator.comparing(entry -> entry.addition, comparator));

		RuleEntry originalRuleEntry = affixData.getData(flag);
		if(originalRuleEntry == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		AffixEntry.Type type = originalRuleEntry.getType();

		List<LineEntry> plainRules = new ArrayList<>();
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			collectProductionsByFlag(productions, flag, type, plainRules);
		};
		Runnable completed = () -> {
			try{
				List<LineEntry> disjointRules = collectIntoEquivalenceClasses(plainRules);

				removeOverlappingConditions(disjointRules);

				//FIXME remove this useless call, manage duplications in removeOverlappingConditions...?
				mergeSimilarRules(disjointRules);

				List<String> rules = convertEntriesToRules(flag, type, keepLongestCommonAffix, disjointRules);

//TODO check feasibility of solution?

				LOGGER.info(Backbone.MARKER_RULE_REDUCER, composeHeader(type, flag, originalRuleEntry.isCombineable(), rules.size()));
				rules.forEach(rule -> LOGGER.info(Backbone.MARKER_RULE_REDUCER, rule));
			}
			catch(Exception e){
				LOGGER.info(Backbone.MARKER_RULE_REDUCER, e.getMessage());
			}
		};
		WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		createReadWorker(data, lineProcessor);
	}

	private void collectProductionsByFlag(List<Production> productions, String flag, AffixEntry.Type type, List<LineEntry> plainRules){
		Iterator<Production> itr = productions.iterator();
		//skip base production
		itr.next();
		while(itr.hasNext()){
			Production production = itr.next();

			AffixEntry lastAppliedRule = production.getLastAppliedRule(type);
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				String word = lastAppliedRule.undoRule(production.getWord());
				LineEntry affixEntry = (lastAppliedRule.isSuffix()? createSuffixEntry(production, word, type): createPrefixEntry(production, word, type));
				plainRules.add(affixEntry);
			}
		}
	}

	private List<LineEntry> collectIntoEquivalenceClasses(List<LineEntry> entries){
		Map<String, LineEntry> equivalenceTable = new HashMap<>();
		for(LineEntry entry : entries){
			String key = entry.removal + TAB + entry.addition + TAB + entry.condition;
			LineEntry ruleSet = equivalenceTable.putIfAbsent(key, entry);
			if(ruleSet != null)
				ruleSet.from.addAll(entry.from);
		}

		//extract same `from` rules
		Map<Integer, List<LineEntry>> sameFrom = bucket(equivalenceTable.values(), rule -> rule.from.hashCode());
		return sameFrom.values().stream()
			.map(ee -> {
				//collect all the addings
				String addition = ee.stream()
					.map(entry -> entry.addition)
					.collect(Collectors.joining(TAB));
				LineEntry representative = ee.get(0);
				return new LineEntry(representative.removal, addition, representative.condition, representative.from);
			})
			.collect(Collectors.toList());
	}

	private void removeOverlappingConditions(List<LineEntry> rules){
		//sort by shortest condition
		List<LineEntry> sortedList = new ArrayList<>(rules);
		sortedList.sort(shortestConditionComparator);

		while(!sortedList.isEmpty()){
			LineEntry parent = sortedList.remove(0);

			List<LineEntry> children = sortedList.stream()
				.filter(entry -> entry.condition.endsWith(parent.condition))
				.collect(Collectors.toList());
			if(children.isEmpty())
				continue;

			int parentConditionLength = parent.condition.length();
			Set<String> parentFrom = parent.from;
			String parentGroup = extractGroup(parentFrom, parentConditionLength);
			Set<String> childrenFrom = children.stream()
				 .flatMap(entry -> entry.from.stream())
				 .collect(Collectors.toSet());

			String childrenGroup = extractGroup(childrenFrom, parentConditionLength);
			if(StringUtils.containsAny(parentGroup, childrenGroup)){
				//split parents between belonging to children group and not belonging to children group
				String notChildrenGroup = NOT_GROUP_START + childrenGroup + GROUP_END;
				Map<String, List<String>> parentChildrenBucket = bucket(parentFrom,
					from -> {
						char chr = from.charAt(from.length() - parentConditionLength - 1);
						return (StringUtils.contains(childrenGroup, chr)? String.valueOf(chr): notChildrenGroup);
					}
				);
				Pair<LineEntry, List<LineEntry>> newRules = extractCommunalities(parentChildrenBucket, parent);
				LineEntry notInCommonRule = newRules.getLeft();
				List<LineEntry> inCommonRules = newRules.getRight();

				if(notInCommonRule != null){
					rules.add(notInCommonRule);

					List<LineEntry> newParents = bubbleUpNotGroup(parent, sortedList);
					rules.addAll(newParents);

					Iterator<LineEntry> itr = inCommonRules.iterator();
					while(itr.hasNext()){
						LineEntry icr = itr.next();

						//remove from in-common rules those already presents in new parents
						boolean removed = false;
						for(LineEntry np : newParents)
							if(np.condition.endsWith(icr.condition)){
								itr.remove();
								removed = true;
								break;
							}

						if(!removed){
							List<String> conditions = inCommonRules.stream()
								.map(entry -> entry.condition)
								.collect(Collectors.toList());
							String lettersToRemove = extractGroup(conditions, parent.condition.length());
							//remove `lettersToRemove` from `childrenGroup`
							String cleanedChildrenGroup = StringUtils.replaceEach(childrenGroup, lettersToRemove.split(""), new String[]{StringUtils.EMPTY});
							//substitute into notInCommonRule.condition
							notInCommonRule.condition = NOT_GROUP_START + cleanedChildrenGroup + GROUP_END + parent.condition;
						}
					}
				}

				//add new parents to the original list
				rules.addAll(inCommonRules);

				sortedList.addAll(inCommonRules);
				sortedList.sort(shortestConditionComparator);
			}
			else{
				String condition = NOT_GROUP_START + childrenGroup + GROUP_END + parent.condition;
				rules.add(LineEntry.createFrom(parent, condition, parent.from));

				for(LineEntry child : children)
					if(child.condition.length() == parentConditionLength){
						String childGroup = extractGroup(child.from, parentConditionLength);
						if(childGroup.length() > 1)
							childGroup = NOT_GROUP_START + childGroup + GROUP_END;
						child.condition = childGroup + child.condition;
					}

				List<LineEntry> newParents = bubbleUpNotGroup(parent, sortedList);
				rules.addAll(newParents);
			}

			rules.remove(parent);
		}
	}

	private String extractGroup(Collection<String> words, int indexFromLast){
		Set<String> group = new HashSet<>();
		for(String word : words){
			int index = word.length() - indexFromLast - 1;
			if(index < 0)
				throw new IllegalArgumentException("Cannot extract group from [" + StringUtils.join(words, ",")
					+ "] at index " + indexFromLast + " from last because of the presence of the word " + word + " that is too short");

			group.add(String.valueOf(word.charAt(index)));
		}
		return group.stream()
			.sorted(comparator)
			.collect(Collectors.joining(StringUtils.EMPTY));
	}

	/** Extract rule in common and not in common between parent and children */
	private Pair<LineEntry, List<LineEntry>> extractCommunalities(Map<String, List<String>> parentChildrenBucket, LineEntry parent){
		LineEntry newRule = null;
		List<LineEntry> newRules = new ArrayList<>();
		for(Map.Entry<String, List<String>> elem : parentChildrenBucket.entrySet()){
			String key = elem.getKey();
			if(key.startsWith(NOT_GROUP_START)){
				List<String> from = elem.getValue();
				newRule = LineEntry.createFrom(parent, key + parent.condition, from);
			}
			else{
				List<String> from = elem.getValue();
				newRules.add(LineEntry.createFrom(parent, key + parent.condition, from));
			}
		}
		return Pair.of(newRule, newRules);
	}

	private List<LineEntry> bubbleUpNotGroup(LineEntry parent, List<LineEntry> sortedList){
		List<LineEntry> newParents = new ArrayList<>();
		if(StringUtils.isNotEmpty(parent.condition)){
			//extract all the children rules
			List<LineEntry> children = sortedList.stream()
				.filter(entry -> entry.condition.endsWith(parent.condition))
				.collect(Collectors.toList());
			for(LineEntry le : children){
				int index = le.condition.length() - parent.condition.length() - 1;
				while(index > 0){
					//add additional rules
					String condition = NOT_GROUP_START + le.condition.charAt(index - 1) + GROUP_END + le.condition.substring(index);
					newParents.add(LineEntry.createFrom(parent, condition));

					index --;
				}

				sortedList.remove(le);
			}
		}
		return newParents;
	}

	private <K, V> Map<K, List<V>> bucket(Collection<V> entries, Function<V, K> keyGenerator){
		Map<K, List<V>> bucket = new HashMap<>();
		for(V entry : entries){
			K key = keyGenerator.apply(entry);
			if(key != null)
				bucket.computeIfAbsent(key, k -> new ArrayList<>())
					.add(entry);
		}
		return bucket;
	}

	private LineEntry createSuffixEntry(Production production, String word, AffixEntry.Type type){
		int lastCommonLetter;
		int wordLength = word.length();
		String producedWord = production.getWord();
		for(lastCommonLetter = 0; lastCommonLetter < Math.min(wordLength, producedWord.length()); lastCommonLetter ++)
			if(word.charAt(lastCommonLetter) != producedWord.charAt(lastCommonLetter))
				break;

		String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): AffixEntry.ZERO);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): AffixEntry.ZERO);
		if(production.getContinuationFlagCount() > 0)
			addition += production.getLastAppliedRule(type).toStringWithMorphologicalFields(strategy);
		String condition = (lastCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	private LineEntry createPrefixEntry(Production production, String word, AffixEntry.Type type){
		int firstCommonLetter;
		int wordLength = word.length();
		String producedWord = production.getWord();
		int productionLength = producedWord.length();
		int minLength = Math.min(wordLength, productionLength);
		for(firstCommonLetter = 1; firstCommonLetter <= minLength; firstCommonLetter ++)
			if(word.charAt(wordLength - firstCommonLetter) != producedWord.charAt(productionLength - firstCommonLetter))
				break;
		firstCommonLetter --;

		String removal = (firstCommonLetter < wordLength? word.substring(0, wordLength - firstCommonLetter): AffixEntry.ZERO);
		String addition = (firstCommonLetter < productionLength? producedWord.substring(0, productionLength - firstCommonLetter): AffixEntry.ZERO);
		if(production.getContinuationFlagCount() > 0)
			addition += production.getLastAppliedRule(type).toStringWithMorphologicalFields(strategy);
		String condition = (firstCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	private void mergeSimilarRules(Collection<LineEntry> entries){
		//merge common conditions (ex. `[^a]bc` and `[^a]dc` will become `[^a][bd]c`)
		Map<String, List<LineEntry>> mergeBucket = bucket(entries,
			entry -> (entry.condition.contains(GROUP_END)?
				entry.removal + TAB + entry.addition + TAB + RegExpSequencer.splitSequence(entry.condition)[0]
					+ RegExpSequencer.splitSequence(entry.condition).length: null));
		for(List<LineEntry> set : mergeBucket.values())
			if(set.size() > 1){
				LineEntry firstEntry = set.iterator().next();
				String[] firstEntryCondition = RegExpSequencer.splitSequence(firstEntry.condition);
				String[] commonPreCondition = SEQUENCER.subSequence(firstEntryCondition, 0, 1);
				String[] commonPostCondition = SEQUENCER.subSequence(firstEntryCondition, 2);
				//extract all the rules from `set` that has the condition compatible with firstEntry.condition
				String condition = set.stream()
					.map(entry -> RegExpSequencer.splitSequence(entry.condition)[1])
					.sorted(comparator)
					.distinct()
					.collect(Collectors.joining(StringUtils.EMPTY, GROUP_START, GROUP_END));
				condition = StringUtils.join(commonPreCondition) + condition + StringUtils.join(commonPostCondition);
				entries.add(LineEntry.createFrom(firstEntry, condition));

				set.forEach(entries::remove);
			}

		//transform a condition that is only a not group into a positive group
		int notGroupStartLength = NOT_GROUP_START.length();
		int groupEndLength = GROUP_END.length();
		for(LineEntry entry : entries)
			if(entry.condition.endsWith(GROUP_END)){
				String group = extractGroup(entry.from, 0);
				String originalNotGroup = entry.condition.substring(notGroupStartLength, entry.condition.length() - notGroupStartLength - groupEndLength);
				if(!StringUtils.contains(originalNotGroup, group))
					entry.condition = (group.length() > 1? GROUP_START + group + GROUP_END: group);
			}
	}

	private List<String> convertEntriesToRules(String flag, AffixEntry.Type type, boolean keepLongestCommonAffix, Collection<LineEntry> entries){
		//restore original rules
		List<LineEntry> restoredRules = entries.stream()
			.flatMap(rule -> {
				String[] additions = rule.addition.split(TAB);
				return Arrays.stream(additions)
					.map(addition -> new LineEntry(rule.removal, addition, rule.condition, rule.from));
			})
			.collect(Collectors.toList());

		List<LineEntry> sortedEntries = prepareRules(type, keepLongestCommonAffix, restoredRules);

		return composeAffixRules(flag, type, sortedEntries);
	}

	private List<LineEntry> prepareRules(AffixEntry.Type type, boolean keepLongestCommonAffix, Collection<LineEntry> entries){
		if(keepLongestCommonAffix)
			entries.forEach(entry -> {
				String lcs = longestCommonAffix(entry.from, (type == AffixEntry.Type.SUFFIX? this::commonSuffix: this::commonPrefix));
				if(lcs == null)
					lcs = entry.condition;
				else if(entry.condition.contains(GROUP_END)){
					String[] entryCondition = RegExpSequencer.splitSequence(entry.condition);
					if(!SEQUENCER.endsWith(RegExpSequencer.splitSequence(lcs), entryCondition)){
						int tailCharactersToExclude = entryCondition.length;
						if(tailCharactersToExclude <= lcs.length())
							lcs = lcs.substring(0, lcs.length() - tailCharactersToExclude) + entry.condition;
						else
							lcs = entry.condition;
					}
				}
				if(lcs.length() < entry.condition.length())
					throw new IllegalArgumentException("really bad error, lcs.length < condition.length");

				entry.condition = lcs;
			});
		List<LineEntry> sortedEntries = new ArrayList<>(entries);
		sortedEntries.sort(lineEntryComparator);
		return sortedEntries;
	}

	private List<String> composeAffixRules(String flag, AffixEntry.Type type, List<LineEntry> entries){
		int size = entries.size();
		List<String> rules = new ArrayList<>(size);
		for(LineEntry entry : entries)
			rules.add(composeLine(type, flag, entry));
		return rules;
	}

	private String longestCommonAffix(Collection<String> texts, BiFunction<String, String, String> commonAffix){
		String lcs = null;
		if(!texts.isEmpty()){
			Iterator<String> itr = texts.iterator();
			lcs = itr.next();
			while(!lcs.isEmpty() && itr.hasNext())
				lcs = commonAffix.apply(lcs, itr.next());
		}
		return lcs;
	}

	/**
	 * Returns the longest string {@code suffix} such that {@code a.toString().endsWith(suffix) &&
	 * b.toString().endsWith(suffix)}, taking care not to split surrogate pairs. If {@code a} and
	 * {@code b} have no common suffix, returns the empty string.
	 */
	private String commonSuffix(String a, String b){
		int s = 0;
		int aLength = a.length();
		int bLength = b.length();
		int maxSuffixLength = Math.min(aLength, bLength);
		while(s < maxSuffixLength && a.charAt(aLength - s - 1) == b.charAt(bLength - s - 1))
			s ++;
		if(validSurrogatePairAt(a, aLength - s - 1) || validSurrogatePairAt(b, bLength - s - 1))
			s --;
		return a.subSequence(aLength - s, aLength).toString();
	}

	/**
	 * Returns the longest string {@code prefix} such that {@code a.toString().startsWith(prefix) &&
	 * b.toString().startsWith(prefix)}, taking care not to split surrogate pairs. If {@code a} and
	 * {@code b} have no common prefix, returns the empty string.
	 */
	private String commonPrefix(String a, String b){
		int p = 0;
		int maxPrefixLength = Math.min(a.length(), b.length());
		while(p < maxPrefixLength && a.charAt(p) == b.charAt(p))
			p ++;
		if(validSurrogatePairAt(a, p - 1) || validSurrogatePairAt(b, p - 1))
			p --;
		return a.subSequence(0, p).toString();
	}

	/**
	 * True when a valid surrogate pair starts at the given {@code index} in the given {@code string}.
	 * Out-of-range indexes return false.
	 */
	private boolean validSurrogatePairAt(CharSequence string, int index){
		return (index >= 0 && index <= (string.length() - 2)
			&& Character.isHighSurrogate(string.charAt(index))
			&& Character.isLowSurrogate(string.charAt(index + 1)));
	}

	private String composeHeader(AffixEntry.Type type, String flag, boolean isCombineable, int size){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getFlag().getCode())
			.add(flag)
			.add(Character.toString(isCombineable? RuleEntry.COMBINEABLE: RuleEntry.NOT_COMBINEABLE))
			.add(Integer.toString(size))
			.toString();
	}

	private String composeLine(AffixEntry.Type type, String flag, LineEntry partialLine){
		StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getFlag().getCode())
			.add(flag)
			.add(partialLine.removal)
			.add(partialLine.addition)
			.add(partialLine.condition.isEmpty()? DOT: partialLine.condition)
			.toString();
	}

}
