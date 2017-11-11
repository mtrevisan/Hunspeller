package unit731.hunspeller.parsers.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.trie.TrieNode;
import unit731.hunspeller.collections.trie.sequencers.RegExpTrieSequencer;

import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


@AllArgsConstructor
public class WordGenerator{

	//default morphological fields:
	public static final String TAG_STEM = "st:";
	public static final String TAG_ALLOMORPH = "al:";
	public static final String TAG_PART_OF_SPEECH = "po:";
	private static final String TAG_DERIVATIONAL_PREFIX = "dp:";
	private static final String TAG_INFLECTIONAL_PREFIX = "ip:";
	private static final String TAG_TERMINAL_PREFIX = "tp:";
	private static final String TAG_DERIVATIONAL_SUFFIX = "ds:";
	private static final String TAG_INFLECTIONAL_SUFFIX = "is:";
	private static final String TAG_TERMINAL_SUFFIX = "ts:";
	private static final String TAG_SURFACE_PREFIX = "sp:";
	private static final String TAG_FREQUENCY = "fr:";
	public static final String TAG_PHONETIC = "ph:";
	private static final String TAG_HYPHENATION = "hy:";
	private static final String TAG_PART = "pa:";
	private static final String TAG_FLAG = "fl:";


	@NonNull
	private final AffixParser affParser;


	public List<RuleProductionEntry> applyRules(DictionaryEntry dicEntry) throws IllegalArgumentException{
		List<RuleProductionEntry> productions = new ArrayList<>();

		productions.add(getBaseProduction(dicEntry));

		productions.addAll(applySuffixRuleFlags(dicEntry));

		productions.addAll(getPrefixProductions(productions));

		productions.addAll(applyTwofold(productions));

//		productions
//			.forEach(production -> LOGGER.log(Level.INFO, "Produced word {0}", production));

		return productions;
	}

	public FlagParsingStrategy getFlagParsingStrategy(){
		return affParser.getFlagParsingStrategy();
	}

	private RuleProductionEntry getBaseProduction(DictionaryEntry dicEntry) throws IllegalArgumentException{
		String word = dicEntry.getWord();
		try{
			String[] ruleFlags = dicEntry.getRuleFlags();
			Set<String> otherRuleFlags = extractLeftOverContinuationClasses(ruleFlags, true);

			return new RuleProductionEntry(dicEntry, otherRuleFlags);
		}
		catch(IllegalArgumentException e){
			throw new IllegalArgumentException(word + " does not have a rule for flag " + e.getMessage());
		}
	}

	private List<RuleProductionEntry> applySuffixRules(Productable productable){
		String[] dataFields = productable.getDataFields();

		Affixes affixes = separateAffixes(productable.getRuleFlags());
		Set<String> suffixes = affixes.getSuffixes();

		List<RuleProductionEntry> productions = new ArrayList<>();
		for(String suffix : suffixes){
			RuleEntry rule = affParser.getData(suffix);
			if(rule == null)
				throw new IllegalArgumentException(suffix);

			//extract the list of applicable affixes...
			String word = productable.getWord();
			List<AffixEntry> entries = rule.getEntries();
			List<AffixEntry> applicableAffixes = new ArrayList<>();
			for(AffixEntry entry : entries){
				Matcher match = entry.getMatch();
				//... only if it matches the given word...
				if(match == null || PatternService.find(word, match))
					applicableAffixes.add(entry);
			}
			if(applicableAffixes.isEmpty())
				throw new IllegalArgumentException("Word has no applicable rules for " + suffix + " from " + word
					+ affParser.getStrategy().joinRuleFlags(productable.getRuleFlags()));

			//... and apply each rule
			for(AffixEntry entry : applicableAffixes){
				RuleProductionEntry production = getProduction(entry, word, new HashSet<>(Arrays.asList(entry.getContinuationClasses())),
					dataFields, rule.isCombineable());
				productions.add(production);
			}
		}

		return productions;
	}

	/** Separate the prefixes from the suffixes */
	private Affixes separateAffixes(String[] ruleFlags) throws IllegalArgumentException{
		Set<String> terminalAffixes = new HashSet<>();
		Set<String> prefixes = new HashSet<>();
		Set<String> suffixes = new HashSet<>();
		for(String ruleFlag : ruleFlags){
			//always keep these flags
			if(affParser.definesFlag(ruleFlag)){
				terminalAffixes.add(ruleFlag);
				continue;
			}

			RuleEntry rule = affParser.getData(ruleFlag);
			if(rule == null)
				throw new IllegalArgumentException(ruleFlag);

			if(rule.isSuffix())
				suffixes.add(ruleFlag);
			else
				prefixes.add(ruleFlag);
		}
		return new Affixes(terminalAffixes, prefixes, suffixes);
	}

	private List<RuleProductionEntry> applySuffixRuleFlags(Productable productable){
		return applyAffixRuleFlags(productable, true);
	}

	private List<RuleProductionEntry> applyPrefixRuleFlags(Productable productable){
		return applyAffixRuleFlags(productable, false);
	}

	/**
	 * Applies suffix rules to an entry.
	 *
	 * @param productable	The productable used to extract all the (single-fold) derivations.
	 * @param isSuffix		Whether a suffix or a prefix should be produced.
	 * @returns	The new words generated by the rules.
	 */
	private List<RuleProductionEntry> applyAffixRuleFlags(Productable productable, boolean isSuffix){
		String word = productable.getWord();
		String[] ruleFlags = productable.getRuleFlags();
		String[] dataFields = productable.getDataFields();

		List<RuleProductionEntry> productions = new ArrayList<>();

		if(ruleFlags != null){
			Set<String> otherRuleFlags = extractLeftOverContinuationClasses(ruleFlags, isSuffix);

			//for each flag...
			for(String ruleFlag : ruleFlags){
				if(affParser.definesFlag(ruleFlag))
					continue;

				//... that is a suffix or prefix...
				RuleEntry rule = affParser.getData(ruleFlag);
				if(isSuffix ^ rule.isSuffix())
					continue;

				//... extract the list of applicable affixes...
				List<AffixEntry> entries = rule.getEntries();
				List<AffixEntry> applicableAffixes = new ArrayList<>();
				for(AffixEntry entry : entries){
					Matcher match = entry.getMatch();
					//... only if it matches the given word
					if(match == null || PatternService.find(word, match))
						applicableAffixes.add(entry);
				}
				if(applicableAffixes.isEmpty())
					throw new IllegalArgumentException("Word has no applicable rules for " + ruleFlag + " from " + productable.getWord()
						+ affParser.getStrategy().joinRuleFlags(productable.getRuleFlags()));


//List<AffixEntry> en0 = new ArrayList<>(applicableAffixes);
//List<AffixEntry> en1 = new ArrayList<>();
//String[] arr = RegExpTrieSequencer.extractCharacters(word);
//Collection<TrieNode<String[], String, List<AffixEntry>>> lst;
//if(isSuffix){
//	ArrayUtils.reverse(arr);
//	for(AffixEntry entry : rule.getSuffixEntries()){
//		Matcher match = entry.getMatch();
//		//... only if it matches the given word
//		if(match == null || PatternService.find(arr, match))
//			en1.add(entry);
//	}
//}
//else{
//	for(AffixEntry entry : rule.getPrefixEntries()){
//		Matcher match = entry.getMatch();
//		//... only if it matches the given word
//		if(match == null || PatternService.find(arr, match))
//			en1.add(entry);
//	}
//}
//en0.sort((a1, a2) -> a1.toString().compareTo(a2.toString()));
//en1.sort((a1, a2) -> a1.toString().compareTo(a2.toString()));
				//List<RegExpPrefix<AffixEntry>> rePrefixes = (isSuffix? rule.getSuffixEntries().findSuffix(word): rule.getPrefixEntries().findPrefix(word));
				//List<AffixEntry> applicableAffixes = rePrefixes.stream()
				//	.map(RegExpPrefix::getNode)
				//	.map(RegExpTrieNode::getData)
				//	.flatMap(List::stream)
				//	.collect(Collectors.toList());

				//... and applying each affix rule
				for(AffixEntry entry : applicableAffixes){
					RuleProductionEntry production = getProduction(entry, word, otherRuleFlags, dataFields, rule.isCombineable());
					productions.add(production);
				}
			}
		}

		return productions;
	}

	/** Collect other type's flags (if the current production is for suffixes then collects prefixes, and vice-versa) */
	private Set<String> extractLeftOverContinuationClasses(String[] ruleFlags, boolean isSuffix) throws IllegalArgumentException{
		Set<String> otherRuleFlags = new HashSet<>();
		for(String ruleFlag : ruleFlags){
			//always keep these flags
			if(affParser.definesFlag(ruleFlag)){
				otherRuleFlags.add(ruleFlag);
				continue;
			}

			RuleEntry rule = affParser.getData(ruleFlag);
			if(rule == null)
				throw new IllegalArgumentException(ruleFlag);

			if(isSuffix ^ rule.isSuffix())
				otherRuleFlags.add(ruleFlag);
		}
		return otherRuleFlags;
	}

	private RuleProductionEntry getProduction(AffixEntry affixEntry, String word, Set<String> otherRuleFlags, String[] dataFields,
			boolean isCombineable) throws IllegalArgumentException{
		//produce the new word
		String newWord = affixEntry.applyRule(word, affParser.isFullstrip());
		String[] newDataFields = combineDataFields(dataFields, affixEntry.getDataFields());

		RuleProductionEntry production = new RuleProductionEntry(newWord, otherRuleFlags, affixEntry.getContinuationClasses(), newDataFields,
			isCombineable);
		production.getRules().add(affixEntry);
		return production;
	}

	private String[] combineDataFields(String[] dataFields, String[] affixEntryDataFields){
		List<String> newDataFields = new ArrayList<>();
		//Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the suffix fields)
		//Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the suffix fields)
		//Terminal Suffix: inflectional suffix fields "removed" by additional (not terminal) suffixes, useful for zero morphemes and affixes
		//	removed by splitting rules
		if(dataFields != null)
			for(String dataField : dataFields)
				if(!dataField.startsWith(TAG_INFLECTIONAL_SUFFIX) && !dataField.startsWith(TAG_INFLECTIONAL_PREFIX)
						&& (!dataField.startsWith(TAG_PART_OF_SPEECH) || affixEntryDataFields == null
							|| !Arrays.stream(affixEntryDataFields).anyMatch(field -> field.startsWith(TAG_PART_OF_SPEECH)))
						&& (!dataField.startsWith(TAG_TERMINAL_SUFFIX) || affixEntryDataFields == null
							|| !Arrays.stream(affixEntryDataFields).allMatch(field -> !field.startsWith(TAG_TERMINAL_SUFFIX))))
					newDataFields.add(dataField);
		if(affixEntryDataFields != null)
			newDataFields.addAll(Arrays.asList(affixEntryDataFields));
		return newDataFields.toArray(new String[0]);
	}

	private List<RuleProductionEntry> getPrefixProductions(List<RuleProductionEntry> previousProductions){
		List<RuleProductionEntry> prefixedProductions = new ArrayList<>();
		for(RuleProductionEntry production : previousProductions){
			if(!production.isCombineable())
				continue;

			List<RuleProductionEntry> secondProductions = applyPrefixRuleFlags(production);
			if(secondProductions.isEmpty())
				continue;

			copyOverSuffixRules(secondProductions, production);

			prefixedProductions.addAll(secondProductions);
		}
		return prefixedProductions;
	}

	private List<RuleProductionEntry> applyTwofold(List<RuleProductionEntry> productions){
		List<RuleProductionEntry> twofoldProductions = new ArrayList<>();
		for(RuleProductionEntry production : productions){
			List<RuleProductionEntry> prods;
			if(affParser.isComplexPrefixes())
				prods = applyPrefixRuleFlags(production);
			else
				prods = applySuffixRuleFlags(production);

			checkTwofoldViolation(prods);

			copyOverSuffixRules(prods, production);

			twofoldProductions.addAll(prods);
		}
		return twofoldProductions;
	}

	private void checkTwofoldViolation(List<RuleProductionEntry> prods) throws IllegalArgumentException{
		List<String> twofoldBreakingRules = prods.stream()
			.map(RuleProductionEntry::getRuleFlags)
			.flatMap(Arrays::stream)
			.distinct()
			.filter(flag -> {
				Boolean suffix = affParser.isSuffix(flag);
				return (suffix != null && (affParser.isComplexPrefixes() ^ suffix));
			})
			.collect(Collectors.toList());
		if(twofoldBreakingRules.size() > 0)
			throw new IllegalArgumentException("Twofold rule violated (" + StringUtils.join(twofoldBreakingRules, ", ") + ")");
	}

	private void copyOverSuffixRules(List<RuleProductionEntry> secondProductions, RuleProductionEntry production){
		List<AffixEntry> pps = production.getRules();
		for(RuleProductionEntry prod : secondProductions){
			int j = 0;
			for(AffixEntry pp : pps)
				prod.getRules().add(j ++, pp);
		}
	}

}
