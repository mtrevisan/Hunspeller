package unit731.hunspeller.parsers.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.services.SetHelper;


public class RulesReducer{

	private static final Logger LOGGER = LoggerFactory.getLogger(RulesReducer.class);

	private static final String NOT_GROUP_START = "[^";
	private static final String GROUP_START = "[";
	private static final String GROUP_END = "]";
	private static final String TAB = "\t";
	private static final String ZERO = "0";
	private static final String DOT = ".";

	private static final RegExpSequencer SEQUENCER = new RegExpSequencer();


	private final Comparator<LineEntry> shortestConditionComparator = Comparator.comparingInt(entry -> entry.condition.length());

	private final AffixData affixData;
	private final FlagParsingStrategy strategy;
	private final WordGenerator wordGenerator;
	private final Comparator<String> comparator;
	private final Comparator<LineEntry> lineEntryComparator;


	public RulesReducer(final AffixData affixData, final WordGenerator wordGenerator){
		Objects.requireNonNull(affixData);
		Objects.requireNonNull(wordGenerator);

		this.affixData = affixData;
		strategy = affixData.getFlagParsingStrategy();
		this.wordGenerator = wordGenerator;
		comparator = BaseBuilder.getComparator(affixData.getLanguage());
		lineEntryComparator = Comparator.comparingInt((LineEntry entry) -> RegExpSequencer.splitSequence(entry.condition).length)
			.thenComparingInt(entry -> StringUtils.countMatches(entry.condition, GROUP_END))
			.thenComparingInt(entry -> entry.removal.length())
			.thenComparing(entry -> StringUtils.reverse(entry.condition), comparator)
			.thenComparing(entry -> entry.removal, comparator)
			.thenComparingInt(entry -> entry.anAddition().length())
			.thenComparing(LineEntry::anAddition, comparator);
	}


	public List<LineEntry> collectProductionsByFlag(final List<Production> productions, final String flag, final AffixEntry.Type type){
		//remove base production
		productions.remove(0);
		//collect all productions that generates from the given flag
		final List<LineEntry> filteredRules = new ArrayList<>();
		for(final Production production : productions){
			final AffixEntry lastAppliedRule = production.getLastAppliedRule(type);
			if(lastAppliedRule != null && lastAppliedRule.getFlag().equals(flag)){
				final String word = lastAppliedRule.undoRule(production.getWord());
				final LineEntry affixEntry = (lastAppliedRule.isSuffix()?
					createSuffixEntry(production, word, type):
					createPrefixEntry(production, word, type));
				filteredRules.add(affixEntry);
			}
		}
		return compactProductions(filteredRules);
	}

	private List<LineEntry> compactProductions(final List<LineEntry> rules){
		final List<LineEntry> compactedRules = new ArrayList<>();
		if(rules.size() > 1){
			final String from = rules.get(0).from.iterator().next();
			//retrieve rule with longest condition (all the other conditions must be this long)
			final LineEntry compactedRule = rules.stream()
				.max(Comparator.comparingInt(rule -> rule.condition.length()))
				.get();
			final int longestConditionLength = compactedRule.condition.length();
			for(final LineEntry rule : rules){
				//recover the missing characters for the current condition to become of length the maximum found earlier
				final int startIndex = from.length() - longestConditionLength;
				//if a condition is not long enough, keep it separate
				if(startIndex >= 0){
					final int delta = longestConditionLength - rule.condition.length();
					final String deltaAddition = from.substring(startIndex, startIndex + delta);
					//add addition
					for(final String addition : rule.addition)
						compactedRule.addition.add(deltaAddition + addition);
				}
			}
			compactedRules.add(compactedRule);
		}
		else
			compactedRules.addAll(rules);
		return compactedRules;
	}

	private LineEntry createSuffixEntry(final Production production, final String word, final AffixEntry.Type type){
		int lastCommonLetter;
		final int wordLength = word.length();
		final String producedWord = production.getWord();
		for(lastCommonLetter = 0; lastCommonLetter < Math.min(wordLength, producedWord.length()); lastCommonLetter ++)
			if(word.charAt(lastCommonLetter) != producedWord.charAt(lastCommonLetter))
				break;

		final String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): AffixEntry.ZERO);
		String addition = (lastCommonLetter < producedWord.length()? producedWord.substring(lastCommonLetter): AffixEntry.ZERO);
		final AffixEntry lastAppliedRule = production.getLastAppliedRule(type);
		if(lastAppliedRule != null)
			addition += lastAppliedRule.toStringWithMorphologicalFields(strategy);
		final String condition = (lastCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	private LineEntry createPrefixEntry(final Production production, final String word, final AffixEntry.Type type){
		int firstCommonLetter;
		final int wordLength = word.length();
		final String producedWord = production.getWord();
		final int productionLength = producedWord.length();
		final int minLength = Math.min(wordLength, productionLength);
		for(firstCommonLetter = 1; firstCommonLetter <= minLength; firstCommonLetter ++)
			if(word.charAt(wordLength - firstCommonLetter) != producedWord.charAt(productionLength - firstCommonLetter))
				break;
		firstCommonLetter --;

		final String removal = (firstCommonLetter < wordLength? word.substring(0, wordLength - firstCommonLetter): AffixEntry.ZERO);
		String addition = (firstCommonLetter < productionLength? producedWord.substring(0, productionLength - firstCommonLetter): AffixEntry.ZERO);
		if(production.getContinuationFlagCount() > 0)
			addition += production.getLastAppliedRule(type).toStringWithMorphologicalFields(strategy);
		final String condition = (firstCommonLetter < wordLength? removal: StringUtils.EMPTY);
		return new LineEntry(removal, addition, condition, word);
	}

	public List<LineEntry> reduceRules(final List<LineEntry> plainRules, final AffixEntry.Type type){
		List<LineEntry> compactedRules = compactRules(plainRules);

		//reshuffle originating list to place the correct productions in the correct rule
		//case 18
		redistributeAdditions(compactedRules, type);

		compactedRules = disjoinConditions(compactedRules);

		mergeSimilarRules(compactedRules);

		return compactedRules;
	}

	public List<String> convertFormat(final String flag, final boolean keepLongestCommonAffix, final List<LineEntry> compactedRules)
			throws IllegalArgumentException{
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		final AffixEntry.Type type = ruleToBeReduced.getType();
		final List<String> prettyPrintRules = convertEntriesToRules(flag, type, keepLongestCommonAffix, compactedRules);
		prettyPrintRules.add(0, composeHeader(type, flag, ruleToBeReduced.combinableChar(), prettyPrintRules.size()));
		return prettyPrintRules;
	}

	public void checkReductionCorrectness(final String flag, final List<String> reducedRules, final List<LineEntry> originalRules,
			final List<String> originalLines) throws IllegalArgumentException{
		final RuleEntry ruleToBeReduced = affixData.getData(flag);
		if(ruleToBeReduced == null)
			throw new IllegalArgumentException("Non-existent rule " + flag + ", cannot reduce");

		final List<AffixEntry> entries = reducedRules.stream()
			.skip(1)
			.map(line -> new AffixEntry(line, strategy, null, null))
			.collect(Collectors.toList());
		final AffixEntry.Type type = ruleToBeReduced.getType();
		final RuleEntry overriddenRule = new RuleEntry((type == AffixEntry.Type.SUFFIX), ruleToBeReduced.combinableChar(), entries);
		for(final String line : originalLines){
			final List<Production> originalProductions = wordGenerator.applyAffixRules(line);
			final List<Production> productions = wordGenerator.applyAffixRules(line, overriddenRule);

			final List<LineEntry> filteredOriginalRules = collectProductionsByFlag(originalProductions, flag, type);
			final List<LineEntry> filteredRules = collectProductionsByFlag(productions, flag, type);
			if(!filteredOriginalRules.equals(filteredRules))
				throw new IllegalArgumentException("Something very bad occurs while producing from '" + line + "', expected "
					+ filteredOriginalRules + ", obtained " + filteredRules);
		}
	}

	private List<LineEntry> compactRules(final Collection<LineEntry> rules){
		//same removal, addition, and condition parts
		return collect(rules,
			entry -> entry.removal + TAB + mergeSet(entry.addition) + TAB + entry.condition,
			(rule, entry) -> rule.from.addAll(entry.from));
	}

	/** Reshuffle originating list to place the correct productions in the correct rule */
	private void redistributeAdditions(List<LineEntry> rules, AffixEntry.Type type){
		rules = makeAdditionsDisjoint(rules, type);

		//sort processing-list by shortest condition
		rules.sort(shortestConditionComparator);

		//for each parent rule
		for(final LineEntry parent : rules){
			//extract raw additions from parent
			final Map<String, String> parentRemoval = new HashMap<>();
			final Set<String> parentAdditions = new HashSet<>();
			parent.addition.forEach(add -> {
				final int lcsLength = longestCommonAffix(Arrays.asList(add, parent.removal),
					(type == AffixEntry.Type.SUFFIX? this::commonPrefix: this::commonSuffix))
					.length();
				if(lcsLength > 0){
					parentRemoval.put(parent.removal.substring(lcsLength), add);
					parentAdditions.add(add.substring(lcsLength));
				}
			});
			if(parentRemoval.isEmpty())
				continue;

			for(final LineEntry child : rules)
				if(child != parent && parentRemoval.containsKey(child.removal)){
					//extract raw additions from child
					int minimumLCSLength = Integer.MAX_VALUE;
					final Set<String> childAdditions = new HashSet<>();
					for(final String addition : child.addition){
						final int lcsLength = longestCommonAffix(Arrays.asList(addition, child.removal),
							(type == AffixEntry.Type.SUFFIX? this::commonPrefix: this::commonSuffix))
							.length();
						if(lcsLength < minimumLCSLength)
							minimumLCSLength = lcsLength;
						childAdditions.add(addition.substring(lcsLength));
					}

					final String childEndingCondition = child.condition.substring(minimumLCSLength);
					//extract from each child all the additions present in the parent
					if(parentRemoval.containsKey(childEndingCondition) && childAdditions.equals(parentAdditions)){
						parent.addition.remove(parentRemoval.get(childEndingCondition));
						child.from.addAll(parent.from);
					}
				}
		}
System.out.println();
	}

	private List<LineEntry> makeAdditionsDisjoint(final List<LineEntry> rules, AffixEntry.Type type){
		//sort processing-list by longest condition
		rules.sort(shortestConditionComparator.reversed());

		//tranform
		//	[rem=èra,add=[ereta, ara, era, iera, ièra, areta, iereta],cond=èra,from=...]
		//	[rem=èra,add=[ereta, ara, era, areta],cond=èra,from=...]
		//into
		//	[rem=èra,add=[iera, ièra, iereta],    cond=èra,from=...]
		//	[rem=èra,add=[ereta, ara, era, areta],cond=èra,from=...]

		//transform
		//	[rem=ièr,add=[ar, areto, ereto, ier, èr, iar, iereto, er],cond=ièr,from=...]
		//into
		//	[rem= èr,add=[er, ar, ereto],           cond=ièr,from=...]
		//	[rem=ièr,add=[ar, areto, ereto, èr, er],cond=ièr,from=...]
		final List<LineEntry> disjointedRules = new ArrayList<>();
		for(final LineEntry rule : rules){
			final Map<String, List<String>> lcss = bucket(rule.addition,
				add -> longestCommonAffix(Arrays.asList(add, rule.removal), (type == AffixEntry.Type.SUFFIX? this::commonPrefix: this::commonSuffix)));
			if(lcss.size() > 1){
				//order keys from longer to shorter
				final List<String> keys = new ArrayList<>(lcss.keySet());
				keys.sort(Comparator.comparingInt(String::length).reversed());

				//add each key, remove the list from the addition
				for(final String key : keys){
					final int keyLength = key.length();
					final String condition = rule.condition.substring(keyLength);
					if(condition.isEmpty())
						break;

					final String removal = (rule.removal.length() > condition.length()? condition: rule.removal);
					final Set<String> addition = lcss.get(key).stream()
						.map(add -> add.substring(keyLength))
						.collect(Collectors.toSet());
					final LineEntry newEntry = new LineEntry(removal, addition, condition, rule.from);
					insertRuleOrUpdateFrom(disjointedRules, newEntry);
				}
			}
			else
				insertRuleOrUpdateFrom(disjointedRules, rule);
		}

		final Map<String, List<LineEntry>> conditionBucket = bucket(disjointedRules, rule -> rule.condition);
		for(final List<LineEntry> entry : conditionBucket.values())
			if(entry.size() == 2){
				final Set<String> additionIntersection = SetHelper.intersection(entry.get(0).addition, entry.get(1).addition);
				if(!additionIntersection.isEmpty()){
					final LineEntry container = entry.get(entry.get(0).addition.size() >= entry.get(1).addition.size()? 0: 1);
					final LineEntry contained = entry.get(entry.get(0).addition.size() >= entry.get(1).addition.size()? 1: 0);
					container.addition.removeAll(additionIntersection);
					if(contained.addition.equals(additionIntersection))
						contained.from.addAll(container.from);
					else
						throw new IllegalArgumentException("to do 1");
				}
			}
			else if(entry.size() != 1)
				throw new IllegalArgumentException("to do 2");

		return disjointedRules;
	}

	private void insertRuleOrUpdateFrom(final List<LineEntry> expandedRules, final LineEntry rule){
		final int idx = expandedRules.indexOf(rule);
		if(idx >= 0)
			expandedRules.get(idx)
				.from.addAll(rule.from);
		else
			expandedRules.add(rule);
	}

	private <K, V> List<V> collect(final Collection<V> entries, final Function<V, K> keyMapper, final BiConsumer<V, V> mergeFunction){
		final Map<K, V> compaction = new HashMap<>();
		for(final V entry : entries){
			final K key = keyMapper.apply(entry);
			final V rule = compaction.putIfAbsent(key, entry);
			if(rule != null)
				mergeFunction.accept(rule, entry);
		}
		return new ArrayList<>(compaction.values());
	}

	private List<LineEntry> disjoinConditions(List<LineEntry> rules){
		//expand same conditions (if any); store surely disjoint rules
		final List<LineEntry> nonOverlappingRules = disjoinSameConditions(rules);

		//expand same ending conditions (if any); store surely disjoint rules
		nonOverlappingRules.addAll(disjoinSameEndingConditions(rules));

		return nonOverlappingRules;
	}

	private List<LineEntry> disjoinSameConditions(final List<LineEntry> rules){
		final List<LineEntry> finalRules = new ArrayList<>();

		//bucket by same condition
		final Map<String, List<LineEntry>> conditionBucket = bucket(rules, rule -> rule.condition);

		for(final Map.Entry<String, List<LineEntry>> entry : conditionBucket.entrySet()){
			final List<LineEntry> sameCondition = entry.getValue();
			//remove empty condition and multiple rules with the same condition
			if(entry.getKey().isEmpty() || sameCondition.size() > 1){
				final String condition = entry.getKey();

				//extract children
				final List<LineEntry> children = rules.stream()
					.filter(rule -> rule.condition.endsWith(condition))
					.collect(Collectors.toList());

				final Map<LineEntry, Set<Character>> groups = children.stream()
					.collect(Collectors.toMap(Function.identity(), child -> extractGroup(child.from, condition.length())));

				rules.removeAll(sameCondition);

				//separate conditions:
				//for each rule with same condition
				for(final LineEntry parent : sameCondition){
					//extract ratifying group
					final Set<Character> parentGroup = groups.get(parent);

					//extract negated group
					final Set<Character> childrenGroup = children.stream()
						.filter(child -> child != parent)
						.map(groups::get)
						.flatMap(Set::stream)
						.collect(Collectors.toSet());

					final Set<Character> groupsIntersection = SetHelper.intersection(parentGroup, childrenGroup);
					parentGroup.removeAll(groupsIntersection);
					if(!parentGroup.isEmpty()){
						final String preCondition = (parent.condition.isEmpty() || groupsIntersection.isEmpty() && parentGroup.size() <= childrenGroup.size()?
							makeGroup(parentGroup): makeNotGroup(childrenGroup));
						final LineEntry newRule = LineEntry.createFrom(parent, preCondition + parent.condition);
						finalRules.add(newRule);
					}
					groupsIntersection.stream()
						.map(chr -> LineEntry.createFrom(parent, chr + parent.condition))
						.forEach(rules::add);
				}
			}
		}

		return finalRules;
	}

	private List<LineEntry> disjoinSameEndingConditions(final List<LineEntry> rules){
		final List<LineEntry> finalRules = new ArrayList<>();

		//bucket by condition ending
		final List<List<LineEntry>> forest = bucketByConditionEnding(rules);

		//for each bush in the forest
		for(final List<LineEntry> bush : forest){
			//if there is only one rule, then it goes in the final set
			if(bush.size() == 1){
				final LineEntry parent = bush.get(0);
				finalRules.add(parent);
			}
			//otherwise process the rules
			else{
				Iterator<LineEntry> itr = bush.iterator();
				while(itr.hasNext()){
					final LineEntry parent = itr.next();

					bush.remove(parent);

					final List<LineEntry> bubbles = extractBubbles(bush, parent);
					if(!bubbles.isEmpty()){
						//extract ratifying group
						final int parentConditionLength = parent.condition.length();
						final Set<Character> parentGroup = extractGroup(parent.from, parentConditionLength);

						//extract negated group
						final Set<Character> childrenGroup = bubbles.stream()
							.map(child -> child.condition.charAt(child.condition.length() - parentConditionLength - 1))
							.collect(Collectors.toSet());

						//if intersection(parent-group, children-group) is empty
						final Set<Character> groupIntersection = SetHelper.intersection(parentGroup, childrenGroup);
						parentGroup.removeAll(groupIntersection);

						//calculate new condition (if it was empty or the ratifying group is of cardinality 1, choose the ratifying over the negated)
						final String preCondition = (parentConditionLength == 0 || parentGroup.size() == 1 && childrenGroup.size() > 1?
							makeGroup(parentGroup): makeNotGroup(childrenGroup));
						LineEntry newEntry = LineEntry.createFrom(parent, preCondition + parent.condition);

						//keep only rules that matches some existent words
						if(newEntry.isProductive())
							finalRules.add(newEntry);
						else
							LOGGER.debug("skip unused rule: {} {} {}", newEntry.removal, String.join("|", newEntry.addition),
								(newEntry.condition.isEmpty()? DOT: newEntry.condition));

						final int maxConditionLength = bush.get(bush.size() - 1).condition.length();
						if(parentConditionLength + 1 >= maxConditionLength){
							bush.removeAll(bubbles);

							finalRules.addAll(bubbles);
						}
						else if(bush.stream().allMatch(rule -> rule.condition.length() > parentConditionLength + 1)){
							final List<LineEntry> bushes = new ArrayList<>(bush);
							bushes.add(parent);
							for(final Character chr : childrenGroup){
								newEntry = LineEntry.createFrom(parent, chr + parent.condition);
								if(!bush.contains(newEntry))
									bush.add(newEntry);
							}

							bush.sort(shortestConditionComparator);
						}
						else if(!groupIntersection.isEmpty() && !parentGroup.isEmpty()){
							//expand intersection
							for(final Character chr : groupIntersection){
								newEntry = LineEntry.createFrom(parent, chr + parent.condition);
								bush.add(newEntry);

								finalRules.addAll(disjoinSameConditions(bush));
							}

							bush.sort(shortestConditionComparator);
						}

						//continue until bubbles.condition length is reached
					}
					else
						finalRules.add(parent);

					itr = bush.iterator();
				}
			}
		}

		return finalRules;
	}

	private List<List<LineEntry>> bucketByConditionEnding(final List<LineEntry> rules){
		rules.sort(shortestConditionComparator);

		List<List<LineEntry>> forest = new ArrayList<>();
		while(!rules.isEmpty()){
			//extract base condition
			final String parentCondition = rules.get(0).condition;
			
			//extract similar (same ending condition) rules from processing-list
			final List<LineEntry> children = new ArrayList<>();
			final Iterator<LineEntry> itr = rules.iterator();
			while(itr.hasNext()){
				final LineEntry rule = itr.next();

				if(rule.condition.endsWith(parentCondition)){
					children.add(rule);
					itr.remove();
				}
			}
			forest.add(children);
		}
		return forest;
	}

	private List<LineEntry> extractBubbles(final List<LineEntry> bush, final LineEntry parent){
		final int parentConditionLength = parent.condition.length();
		final List<LineEntry> bubbles = bush.stream()
			.filter(child -> child.condition.endsWith(parent.condition) && child.condition.length() > parentConditionLength)
			.collect(Collectors.toList());

		//if the bush contains a rule whose `from` is contained into this bubble, then remove the bubble
		bubbles.removeIf(bubble -> !SetHelper.isDisjoint(parent.from, bubble.from));
		return bubbles;
	}

	public boolean isContainedInto(final LineEntry parent, final LineEntry child, final AffixEntry.Type type){
		final Set<String> parentBones = extractRuleSpine(parent, type);
		final Set<String> childBones = extractRuleSpine(child, type);
		return childBones.containsAll(parentBones);
	}

	private Set<String> extractRuleSpine(final LineEntry rule, final AffixEntry.Type type){
		final Set<String> parentBones = new HashSet<>();
		for(final String add : rule.addition){
			final int lcsLength = longestCommonAffix(Arrays.asList(add, rule.removal),
				(type == AffixEntry.Type.SUFFIX? this::commonPrefix: this::commonSuffix))
				.length();
			parentBones.add(add.substring(lcsLength) + TAB + rule.removal.substring(lcsLength));
		}
		return parentBones;
	}

	private Set<Character> extractGroup(final Collection<String> words, final int indexFromLast){
		final Set<Character> group = new HashSet<>();
		for(final String word : words){
			final int index = word.length() - indexFromLast - 1;
			if(index < 0)
				throw new IllegalArgumentException("Cannot extract group from [" + StringUtils.join(words, ",")
					+ "] at index " + indexFromLast + " from last because of the presence of the word '" + word + "' that is too short");

			group.add(word.charAt(index));
		}
		return group;
	}

	private String makeGroup(final Set<Character> group){
		final String merge = mergeSet(group);
		return (group.size() > 1? GROUP_START + merge + GROUP_END: merge);
	}

	private String makeGroup(final Set<Character> group, final String suffix){
		return makeGroup(group) + suffix;
	}

	private String makeNotGroup(final Set<Character> group){
		final String merge = mergeSet(group);
		return NOT_GROUP_START + merge + GROUP_END;
	}

	private <V> String mergeSet(final Set<V> set){
		return set.stream()
			.map(String::valueOf)
			.sorted(comparator)
			.collect(Collectors.joining());
	}

	private <K, V> Map<K, List<V>> bucket(final Collection<V> entries, final Function<V, K> keyMapper){
		final Map<K, List<V>> bucket = new HashMap<>();
		for(final V entry : entries){
			final K key = keyMapper.apply(entry);
			if(key != null)
				bucket.computeIfAbsent(key, k -> new ArrayList<>())
					.add(entry);
		}
		return bucket;
	}

	/** Merge common conditions (ex. `[^a]bc` and `[^a]dc` will become `[^a][bd]c`) */
	private void mergeSimilarRules(List<LineEntry> entries){
		final Map<String, List<LineEntry>> similarityBucket = bucket(entries, entry -> (entry.condition.contains(GROUP_END)?
			entry.removal + TAB + entry.addition + TAB + RegExpSequencer.splitSequence(entry.condition)[0] + TAB
				+ RegExpSequencer.splitSequence(entry.condition).length:
			null));
		for(final List<LineEntry> similarities : similarityBucket.values())
			if(similarities.size() > 1){
				final LineEntry anEntry = similarities.iterator().next();
				final String[] aCondition = RegExpSequencer.splitSequence(anEntry.condition);
				final String[] commonPreCondition = SEQUENCER.subSequence(aCondition, 0, 1);
				final String[] commonPostCondition = SEQUENCER.subSequence(aCondition, 2);
				//extract all the rules from `similarities` that has the condition compatible with firstEntry.condition
				final Set<Character> group = similarities.stream()
					.map(entry -> RegExpSequencer.splitSequence(entry.condition)[1].charAt(0))
					.collect(Collectors.toSet());
				final String condition = StringUtils.join(commonPreCondition) + makeGroup(group, StringUtils.EMPTY) + StringUtils.join(commonPostCondition);
				entries.add(LineEntry.createFrom(anEntry, condition));

				similarities.forEach(entries::remove);
			}
	}

	private List<String> convertEntriesToRules(final String flag, final AffixEntry.Type type, final boolean keepLongestCommonAffix,
			final Collection<LineEntry> entries){
		//restore original rules
		final List<LineEntry> restoredRules = entries.stream()
			.flatMap(rule -> rule.addition.stream()
				.map(addition -> {
					final int lcp = commonPrefix(rule.removal, addition).length();
					final String removal = rule.removal.substring(lcp);
					return new LineEntry((removal.isEmpty()? ZERO: removal), addition.substring(lcp), rule.condition, rule.from);
				}))
			.collect(Collectors.toList());

		final List<LineEntry> sortedEntries = prepareRules(type, keepLongestCommonAffix, restoredRules);

		return composeAffixRules(flag, type, sortedEntries);
	}

	private List<LineEntry> prepareRules(final AffixEntry.Type type, final boolean keepLongestCommonAffix, final Collection<LineEntry> entries){
		if(keepLongestCommonAffix)
			for(final LineEntry entry : entries){
				String lcs = longestCommonAffix(entry.from, (type == AffixEntry.Type.SUFFIX? this::commonSuffix: this::commonPrefix));
				if(lcs == null)
					lcs = entry.condition;
				else if(entry.condition.contains(GROUP_END)){
					final String[] entryCondition = RegExpSequencer.splitSequence(entry.condition);
					if(!SEQUENCER.endsWith(RegExpSequencer.splitSequence(lcs), entryCondition)){
						final int tailCharactersToExclude = entryCondition.length;
						if(tailCharactersToExclude <= lcs.length())
							lcs = lcs.substring(0, lcs.length() - tailCharactersToExclude) + entry.condition;
						else
							lcs = entry.condition;
					}
				}
				if(lcs.length() < entry.condition.length())
					throw new IllegalArgumentException("really bad error, lcs.length < condition.length");

				entry.condition = lcs;
			}
		final List<LineEntry> sortedEntries = new ArrayList<>(entries);
		sortedEntries.sort(lineEntryComparator);
		return sortedEntries;
	}

	private String longestCommonAffix(final Collection<String> texts, final BiFunction<String, String, String> commonAffix){
		String lcs = null;
		if(!texts.isEmpty()){
			final Iterator<String> itr = texts.iterator();
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
	private String commonSuffix(final String a, final String b){
		int s = 0;
		final int aLength = a.length();
		final int bLength = b.length();
		final int maxSuffixLength = Math.min(aLength, bLength);
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
	private String commonPrefix(final String a, final String b){
		int p = 0;
		final int maxPrefixLength = Math.min(a.length(), b.length());
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
	private boolean validSurrogatePairAt(final CharSequence string, final int index){
		return (index >= 0 && index <= (string.length() - 2)
			&& Character.isHighSurrogate(string.charAt(index))
			&& Character.isLowSurrogate(string.charAt(index + 1)));
	}

	private List<String> composeAffixRules(final String flag, final AffixEntry.Type type, final List<LineEntry> entries){
		final int size = entries.size();
		final List<String> rules = new ArrayList<>(size);
		for(final LineEntry entry : entries)
			rules.add(composeLine(type, flag, entry));
		return rules;
	}

	private String composeHeader(final AffixEntry.Type type, final String flag, final char combinableChar, final int size){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getTag().getCode())
			.add(flag)
			.add(Character.toString(combinableChar))
			.add(Integer.toString(size))
			.toString();
	}

	private String composeLine(final AffixEntry.Type type, final String flag, final LineEntry partialLine){
		String addition = partialLine.anAddition();
		String morphologicalRules = StringUtils.EMPTY;
		final int idx = addition.indexOf(TAB);
		if(idx >= 0){
			morphologicalRules = addition.substring(idx);
			addition = addition.substring(0, idx);
		}
		String line = type.getTag().getCode() + StringUtils.SPACE + flag + StringUtils.SPACE + partialLine.removal + StringUtils.SPACE
			+ addition + StringUtils.SPACE + (partialLine.condition.isEmpty()? DOT: partialLine.condition);
		if(idx >= 0)
			line += morphologicalRules;
		return line;
	}

}
