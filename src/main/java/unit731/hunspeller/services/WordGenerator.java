package unit731.hunspeller.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.regexptrie.RegExpPrefix;
import unit731.hunspeller.collections.regexptrie.RegExpTrieNode;

import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.AffixParser;
import unit731.hunspeller.resources.AffixEntry;
import unit731.hunspeller.resources.DictionaryEntry;
import unit731.hunspeller.resources.RuleEntry;
import unit731.hunspeller.resources.RuleProductionEntry;


@AllArgsConstructor
public class WordGenerator{

	//default morphological fields:
	public static final String TAG_STEM = "st:";
	public static final String TAG_ALLOMORPH = "al:";
	private static final String TAG_PART_OF_SPEECH = "po:";
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

	private RuleProductionEntry getBaseProduction(DictionaryEntry dicEntry) throws IllegalArgumentException{
		String flag = affParser.getFlag();

		String word = dicEntry.getWord();
		try{
			String[] ruleFlags = dicEntry.getRuleFlags();
			Set<String> otherRuleFlags = extractLeftOverContinuationClasses(ruleFlags, true);

			return new RuleProductionEntry(dicEntry, otherRuleFlags, true, flag);
		}
		catch(IllegalArgumentException e){
			throw new IllegalArgumentException(word + " does not have a rule for flag " + e.getMessage());
		}
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
	 * @param isSuffix	Whether a suffix or a prefix should be produced.
	 * @returns	The new words generated by the rules.
	 */
	private List<RuleProductionEntry> applyAffixRuleFlags(Productable productable, boolean isSuffix){
		String word = productable.getWord();
		String[] ruleFlags = productable.getRuleFlags();
		String[] dataFields = productable.getDataFields();

		List<RuleProductionEntry> productions = new ArrayList<>();

		if(ruleFlags != null){
			String keepcaseTag = affParser.getKeepcase();

			Set<String> otherRuleFlags = extractLeftOverContinuationClasses(ruleFlags, isSuffix);

			//for each flag...
			for(String ruleFlag : ruleFlags){
				if(ruleFlag.equals(keepcaseTag))
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
					if(match == null || match.reset(word).find())
						applicableAffixes.add(entry);
				}

//				List<AffixEntry> en0 = applicableAffixes;
//				List<AffixEntry> en1 = (isSuffix? rule.getSuffixEntries().findSuffix(word): rule.getPrefixEntries().findPrefix(word)).stream()
//					.map(RegExpPrefix::getNode)
//					.map(RegExpTrieNode::getData)
//					.flatMap(List::stream)
//					.collect(Collectors.toList());
//				en0.sort((a1, a2) -> a1.toString().compareTo(a2.toString()));
//				en1.sort((a1, a2) -> a1.toString().compareTo(a2.toString()));
//				if(!ListUtils.isEqualList(en0, en1)){
//					System.out.println("");
//				}
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
		String keepcaseTag = affParser.getKeepcase();

		Set<String> otherRuleFlags = new HashSet<>();
		for(String ruleFlag : ruleFlags){
			if(ruleFlag.equals(keepcaseTag)){
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
			isCombineable, affParser.getFlag());
		production.getRules().add(affixEntry);
		return production;
	}

	private String[] combineDataFields(String[] dataFields, String[] affixEntryDataFields){
		List<String> newDataFields = new ArrayList<>();
		//Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the suffix fields)
		//Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the suffix fields)
		//Terminal Suffix: inflectional suffix fields "removed" by additional (not terminal) suffixes, useful for zero morphemes and affixes removed by splitting rules
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
