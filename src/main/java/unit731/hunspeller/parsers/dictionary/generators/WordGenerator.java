package unit731.hunspeller.parsers.dictionary.generators;

import java.util.List;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.vos.DictionaryEntry;
import unit731.hunspeller.parsers.vos.RuleEntry;
import unit731.hunspeller.parsers.vos.Production;


public class WordGenerator{

	public static final int BASE_PRODUCTION_INDEX = 0;

	private final WordGeneratorAffixRules wordGeneratorAffixRules;
	private final WordGeneratorCompoundRules wordGeneratorCompoundRules;
	private final WordGeneratorCompoundFlag wordGeneratorCompoundFlag;
	private final WordGeneratorCompoundBeginMiddleEnd wordGeneratorCompoundBeginMiddleEnd;


	public WordGenerator(final AffixData affixData, final DictionaryParser dicParser){
		wordGeneratorAffixRules = new WordGeneratorAffixRules(affixData);
		wordGeneratorCompoundRules = new WordGeneratorCompoundRules(affixData, dicParser, this);
		wordGeneratorCompoundFlag = new WordGeneratorCompoundFlag(affixData, dicParser, this);
		wordGeneratorCompoundBeginMiddleEnd = new WordGeneratorCompoundBeginMiddleEnd(affixData, dicParser, this);
	}

	public DictionaryEntry createFromDictionaryLine(final String line){
		return DictionaryEntry.createFromDictionaryLine(line, wordGeneratorAffixRules.affixData);
	}

	public List<Production> applyAffixRules(final DictionaryEntry dicEntry){
		return wordGeneratorAffixRules.applyAffixRules(dicEntry);
	}

	public List<Production> applyAffixRules(final DictionaryEntry dicEntry, final RuleEntry overriddenRule){
		return wordGeneratorAffixRules.applyAffixRules(dicEntry, overriddenRule);
	}

	public List<Production> applyCompoundRules(final String[] inputCompounds, final String compoundRule, final int limit)
			throws IllegalArgumentException{
		return wordGeneratorCompoundRules.applyCompoundRules(inputCompounds, compoundRule, limit);
	}

	public List<Production> applyCompoundFlag(final String[] inputCompounds, final int limit, final int maxCompounds)
			throws IllegalArgumentException{
		return wordGeneratorCompoundFlag.applyCompoundFlag(inputCompounds, limit, maxCompounds);
	}

	public List<Production> applyCompoundBeginMiddleEnd(final String[] inputCompounds, final int limit) throws IllegalArgumentException{
		return wordGeneratorCompoundBeginMiddleEnd.applyCompoundBeginMiddleEnd(inputCompounds, limit);
	}

}
