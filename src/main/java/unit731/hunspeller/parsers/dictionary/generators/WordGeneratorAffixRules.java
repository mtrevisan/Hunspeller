package unit731.hunspeller.parsers.dictionary.generators;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.enums.AffixOption;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.vos.RuleEntry;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.Production;


class WordGeneratorAffixRules extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorAffixRules.class);


	WordGeneratorAffixRules(final AffixData affixData){
		super(affixData);
	}

	List<Production> applyOnefoldAffixRule(final String line, final RuleEntry overriddenRule){
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		final List<String> aliasesFlag = affixData.getData(AffixOption.ALIASES_FLAG);
		final List<String> aliasesMorphologicalField = affixData.getData(AffixOption.ALIASES_MORPHOLOGICAL_FIELD);

		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag, aliasesMorphologicalField);
		dicEntry.applyInputConversionTable(affixData::applyInputConversionTable);

		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		if(dicEntry.hasContinuationFlag(forbiddenWordFlag))
			return Collections.emptyList();

		//extract suffixed productions
		final List<Production> productions = getOnefoldProductions(dicEntry, false, !affixData.isComplexPrefixes(), overriddenRule);
		if(LOGGER.isDebugEnabled() && !productions.isEmpty()){
			LOGGER.debug("Suffix productions:");
			productions.forEach(production -> LOGGER.debug("   {} from {}", production.toString(affixData.getFlagParsingStrategy()),
				production.getRulesSequence()));
		}

		//remove rules that invalidate the affix rule
		enforceNeedAffixFlag(productions);

		//convert using output table
		for(final Production production : productions)
			production.applyOutputConversionTable(affixData::applyOutputConversionTable);

		if(LOGGER.isTraceEnabled())
			productions.forEach(production -> LOGGER.trace("Produced word: {}", production));
		return productions;
	}

	List<Production> applyAffixRules(final String line){
		return applyAffixRules(line, null);
	}

	List<Production> applyAffixRules(final String line, final RuleEntry overriddenRule){
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		final List<String> aliasesFlag = affixData.getData(AffixOption.ALIASES_FLAG);
		final List<String> aliasesMorphologicalField = affixData.getData(AffixOption.ALIASES_MORPHOLOGICAL_FIELD);

		final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag, aliasesMorphologicalField);
		dicEntry.applyInputConversionTable(affixData::applyInputConversionTable);

		final List<Production> productions = applyAffixRules(dicEntry, false, overriddenRule);

		//convert using output table
		for(final Production production : productions)
			production.applyOutputConversionTable(affixData::applyOutputConversionTable);

		if(LOGGER.isTraceEnabled())
			productions.forEach(production -> LOGGER.trace("Produced word: {}", production));

		return productions;
	}

}
