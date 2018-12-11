package unit731.hunspeller.languages;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.languages.valueobjects.LetterMatcherEntry;
import unit731.hunspeller.languages.valueobjects.RuleMatcherEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.MorphologicalTag;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


public class CorrectnessChecker{

	private static final MessageFormat WORD_WITH_RULE_CANNOT_HAVE = new MessageFormat("Word with rule {0} cannot have rule {1}");
	private static final MessageFormat WORD_WITH_LETTER_CANNOT_HAVE = new MessageFormat("Word with letter ''{0}'' cannot have rule {1}");
	private static final MessageFormat WORD_WITH_LETTER_CANNOT_HAVE_USE = new MessageFormat("Word with letter ''{0}'' cannot have rule {1}, use {2}");
	private static final MessageFormat WORD_HAS_NOT_MORPHOLOGICAL_FIELD = new MessageFormat("Word {0} does not have any morphological fields");
	private static final MessageFormat WORD_HAS_INVALID_MORPHOLOGICAL_FIELD_PREFIX = new MessageFormat("Word {0} has an invalid morphological field prefix: {1}");
	private static final MessageFormat WORD_HAS_UNKNOWN_MORPHOLOGICAL_FIELD_PREFIX = new MessageFormat("Word {0} has an unknown morphological field prefix: {1}");
	private static final MessageFormat WORD_HAS_UNKNOWN_MORPHOLOGICAL_FIELD_VALUE = new MessageFormat("Word {0} has an unknown morphological field value: {1}");


	protected AffixParser affParser;
	protected final AbstractHyphenator hyphenator;

	private boolean morphologicalFieldsCheck;
	protected boolean enableVerbSyllabationCheck;
	protected boolean wordCanHaveMultipleAccents;
	private final Map<String, Set<String>> dataFields = new HashMap<>();
	protected Set<String> unsyllabableWords;
	protected Set<String> multipleAccentedWords;
	protected Set<String> hasToContainAccent;
	protected Set<String> cannotContainAccent;
	private final Map<String, Set<LetterMatcherEntry>> letterAndRulesNotCombinable = new HashMap<>();
	private final Map<String, Set<RuleMatcherEntry>> ruleAndRulesNotCombinable = new HashMap<>();


	public CorrectnessChecker(AffixParser affParser, AbstractHyphenator hyphenator){
		Objects.requireNonNull(affParser);

		this.affParser = affParser;
		this.hyphenator = hyphenator;
	}

	protected final void loadRules(Properties rulesProperties) throws IOException{
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

		morphologicalFieldsCheck = Boolean.getBoolean((String)rulesProperties.get("morphologicalFieldsCheck"));
		enableVerbSyllabationCheck = Boolean.getBoolean((String)rulesProperties.get("verbSyllabationCheck"));
		wordCanHaveMultipleAccents = Boolean.getBoolean((String)rulesProperties.get("wordCanHaveMultipleAccents"));

		dataFields.put(MorphologicalTag.TAG_PART_OF_SPEECH, readPropertyAsSet(rulesProperties, "partOfSpeeches", ','));
		dataFields.put(MorphologicalTag.TAG_INFLECTIONAL_SUFFIX, readPropertyAsSet(rulesProperties, "inflectionalSuffixes", ','));
		dataFields.put(MorphologicalTag.TAG_TERMINAL_SUFFIX, readPropertyAsSet(rulesProperties, "terminalSuffixes", ','));
		dataFields.put(MorphologicalTag.TAG_STEM, null);
		dataFields.put(MorphologicalTag.TAG_ALLOMORPH, null);

		unsyllabableWords = readPropertyAsSet(rulesProperties, "unsyllabableWords", ',');
		multipleAccentedWords = readPropertyAsSet(rulesProperties, "multipleAccentedWords", ',');

		String[] flags = strategy.parseFlags(readProperty(rulesProperties, "hasToContainAccent"));
		hasToContainAccent = (flags != null? new HashSet<>(Arrays.asList(flags)): Collections.<String>emptySet());
		flags = strategy.parseFlags(readProperty(rulesProperties, "cannotContainAccent"));
		cannotContainAccent = (flags != null? new HashSet<>(Arrays.asList(flags)): Collections.<String>emptySet());

		Iterator<String> rules = readPropertyAsIterator(rulesProperties, "notCombinableRules", '/');
		while(rules.hasNext()){
			String masterFlag = rules.next();
			String[] wrongFlags = strategy.parseFlags(rules.next());
			ruleAndRulesNotCombinable.computeIfAbsent(masterFlag, k -> new HashSet<>())
				.add(new RuleMatcherEntry(WORD_WITH_RULE_CANNOT_HAVE, masterFlag, wrongFlags));
		}

		String letter = null;
		rules = readPropertyAsIterator(rulesProperties, "letterAndRulesNotCombinable", '/');
		while(rules.hasNext()){
			String elem = rules.next();
			if(elem.length() == 3 && elem.charAt(0) == '_' && elem.charAt(2) == '_')
				letter = String.valueOf(elem.charAt(1));
			else{
				flags = strategy.parseFlags(elem);
				String correctRule = flags[flags.length - 1];
				String[] wrongFlags = ArrayUtils.remove(flags, flags.length - 1);
				letterAndRulesNotCombinable.computeIfAbsent(letter, k -> new HashSet<>())
					.add(new LetterMatcherEntry((StringUtils.isNotBlank(correctRule)? WORD_WITH_LETTER_CANNOT_HAVE_USE: WORD_WITH_LETTER_CANNOT_HAVE),
						letter, wrongFlags, correctRule));
			}
		}
	}

	protected final String readProperty(Properties rulesProperties, String key){
		return rulesProperties.getProperty(key, StringUtils.EMPTY);
	}

	protected final Set<String> readPropertyAsSet(Properties rulesProperties, String key, char separator){
		String line = readProperty(rulesProperties, key);
		return (StringUtils.isNotEmpty(line)? new HashSet<>(Arrays.asList(StringUtils.split(line, separator))): Collections.<String>emptySet());
	}

	protected final Iterator<String> readPropertyAsIterator(Properties rulesProperties, String key, char separator){
		List<String> values = new ArrayList<>();
		@SuppressWarnings("unchecked")
		Set<String> keys = (Set<String>)(Collection<?>)rulesProperties.keySet();
		for(String k : keys)
			if(k.equals(key) || k.startsWith(key) && StringUtils.isNumeric(k.substring(key.length()))){
				String line = readProperty(rulesProperties, k);
				if(StringUtils.isNotEmpty(line))
					values.addAll(Arrays.asList(StringUtils.split(line, separator)));
			}
		return values.iterator();
	}

	protected void letterToFlagIncompatibilityCheck(Production production, Map<String, Set<LetterMatcherEntry>> checks)
			throws IllegalArgumentException{
		for(Map.Entry<String, Set<LetterMatcherEntry>> check : checks.entrySet())
			if(StringUtils.containsAny(production.getWord(), check.getKey()))
				for(LetterMatcherEntry entry : check.getValue())
					entry.match(production);
	}

	protected void flagToFlagIncompatibilityCheck(Production production, Map<String, Set<RuleMatcherEntry>> checks)
			throws IllegalArgumentException{
		for(Map.Entry<String, Set<RuleMatcherEntry>> check : checks.entrySet())
			if(production.hasContinuationFlag(check.getKey()))
				for(RuleMatcherEntry entry : check.getValue())
					entry.match(production);
	}

	public AffixParser getAffParser(){
		return affParser;
	}

	public AbstractHyphenator getHyphenator(){
		return hyphenator;
	}

	//used by the correctness worker:
	public void checkProduction(Production production) throws IllegalArgumentException{
		try{
			String forbidCompoundFlag = affParser.getForbidCompoundFlag();
			if(forbidCompoundFlag != null && !production.hasProductionRules() && production.hasContinuationFlag(forbidCompoundFlag))
				throw new IllegalArgumentException("Non-affix entry contains " + AffixTag.COMPOUND_FORBID_FLAG.getCode());

			if(morphologicalFieldsCheck)
				morphologicalFieldCheck(production);

			incompatibilityCheck(production);

			List<String> splittedWords = hyphenator.splitIntoCompounds(production.getWord());
			for(String subword : splittedWords)
				checkCompoundProduction(subword, production);
		}
		catch(IllegalArgumentException e){
			StringBuilder sb = new StringBuilder(e.getMessage());
			if(production.hasProductionRules())
				sb.append(" (via ").append(production.getRulesSequence()).append(")");
			sb.append(" for ").append(production.getWord());
			throw new IllegalArgumentException(sb.toString());
		}
	}

	private void morphologicalFieldCheck(Production production) throws IllegalArgumentException{
		if(!production.hasMorphologicalFields())
			throw new IllegalArgumentException(WORD_HAS_NOT_MORPHOLOGICAL_FIELD.format(new Object[]{production.getWord()}));

		production.forEachMorphologicalField(morphologicalField -> {
			if(morphologicalField.length() < 4)
				throw new IllegalArgumentException(WORD_HAS_INVALID_MORPHOLOGICAL_FIELD_PREFIX.format(new Object[]{production.getWord(),
					morphologicalField}));

			Set<String> morphologicalFieldTypes = dataFields.get(morphologicalField.substring(0, 3));
			if(morphologicalFieldTypes == null)
				throw new IllegalArgumentException(WORD_HAS_UNKNOWN_MORPHOLOGICAL_FIELD_PREFIX.format(new Object[]{production.getWord(),
					morphologicalField}));
			if(!morphologicalFieldTypes.contains(morphologicalField.substring(3)))
				throw new IllegalArgumentException(WORD_HAS_UNKNOWN_MORPHOLOGICAL_FIELD_VALUE.format(new Object[]{production.getWord(),
					morphologicalField}));
		});
	}

	private void incompatibilityCheck(Production production) throws IllegalArgumentException{
		letterToFlagIncompatibilityCheck(production, letterAndRulesNotCombinable);

		flagToFlagIncompatibilityCheck(production, ruleAndRulesNotCombinable);
	}

	//used by the correctness worker:
	protected void checkCompoundProduction(String subword, Production production) throws IllegalArgumentException{}

	//used by the minimal pairs worker:
	public boolean isConsonant(char chr){
		return true;
	}

	//used by the minimal pairs worker:
	public boolean shouldBeProcessedForMinimalPair(Production production){
		return true;
	}

}
