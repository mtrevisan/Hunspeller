package unit731.hunspeller.languages.vec;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.languages.DictionaryCorrectnessChecker;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.RulesLoader;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.vos.AffixEntry;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.hyphenation.HyphenatorInterface;
import unit731.hunspeller.services.PatternHelper;


public class DictionaryCorrectnessCheckerVEC extends DictionaryCorrectnessChecker{

	public static final String LANGUAGE = "vec";

	private static final String POS_PROPER_NOUN = "proper_noun";
	private static final String POS_NOUN = "noun";
	private static final String POS_ADJECTIVE = "adjective";
	private static final String POS_ADJECTIVE_POSSESSIVE = "adjective_possessive";
	private static final String POS_ADJECTIVE_DEMONSTRATIVE = "adjective_demonstrative";
	private static final String POS_ADJECTIVE_IDENTIFICATIVE = "adjective_identificative";
	private static final String POS_ADJECTIVE_INTERROGATIVE = "adjective_interrogative";
	private static final String POS_QUANTIFIER = "quantifier";
	private static final String POS_VERB = "verb";
	private static final String POS_ARTICLE = "article";
	private static final String POS_PRONOUN = "pronoun";
	private static final String POS_PREPOSITION = "preposition";
	private static final String POS_ADVERB = "adverb";
	private static final String POS_CONJUNCTION = "conjunction";
	private static final String POS_NUMERAL_LATIN = "numeral_latin";
	private static final String POS_UNIT_OF_MEASURE = "unit_of_measure";

	private static final int MINIMAL_PAIR_MINIMUM_LENGTH = 3;

	private static Pattern PATTERN_NON_VANISHING_EL;
	private static Pattern PATTERN_VANISHING_EL_NEXT_TO_CONSONANT;
	private static Pattern PATTERN_PHONEME_CIJJHNHIV;
	private static Pattern PATTERN_NORTHERN_PLURAL;
	private static String PLURAL_NOUN_MASCULINE_RULE;
	private static String VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL;
	private static String NORTHERN_PLURAL_RULE;
	private static String NORTHERN_PLURAL_STRESSED_RULE;
	private static String NORTHERN_PLURAL_EXCEPTION;

	private static final MessageFormat WORD_WITH_UNNECESSARY_STRESS = new MessageFormat("Word have unnecessary stress, {0}");
	private static final MessageFormat WORD_WITH_VAN_EL_CANNOT_CONTAIN_NON_VAN_EL = new MessageFormat("Word with ƚ cannot contain non–ƚ, {0}");
	private static final MessageFormat WORD_WITH_VAN_EL_CANNOT_CONTAIN_RULE = new MessageFormat("Word with ƚ cannot contain rule {0} or {1}, {2}");
	private static final MessageFormat WORD_WITH_VAN_EL_CANNOT_CONTAIN_DH_OR_TH = new MessageFormat("Word with ƚ cannot contain đ or ŧ, {0}");
	private static final MessageFormat WORD_WITH_VAN_EL_NEAR_CONSONANT = new MessageFormat("Word with ƚ near a consonant, {0}");
	private static final MessageFormat WORD_WITH_RULE_CANNOT_HAVE_RULES_OTHER_THAN = new MessageFormat("Word with rule {0} cannot have other rules than {1}");
	private static final MessageFormat NORTHERN_PLURAL_MISSING = new MessageFormat("Northern plural missing, add {0}");
	private static final MessageFormat NORTHERN_PLURAL_NOT_NEEDED = new MessageFormat("Northern plural not needed, remove {0} or {1}");
	private static final MessageFormat WORD_IS_MISSPELLED = new MessageFormat("{0} is misspelled, should be {1}");
	private static final MessageFormat WORD_HAS_MULTIPLE_ACCENTS = new MessageFormat("{0} cannot have multiple accents");
	private static final MessageFormat WORD_HAS_MISSING_ACCENT = new MessageFormat("{0} cannot be generated by the rule {1} because of the missing accent");
	private static final MessageFormat WORD_HAS_PRESENT_ACCENT = new MessageFormat("{0} cannot be generated by the rule {1} because of the already present accent");
	private static final MessageFormat WORD_CANNOT_HAVE_CIJJHNHIV = new MessageFormat("{0} cannot have [cijɉñ]iV");


	private final Orthography orthography;

	private String[] pluralFlags;


	public DictionaryCorrectnessCheckerVEC(final AffixData affixData, final HyphenatorInterface hyphenator){
		super(affixData, hyphenator);

		Objects.requireNonNull(hyphenator);

		orthography = OrthographyVEC.getInstance();
	}

	@Override
	public void loadRules(){
		rulesLoader = new RulesLoader(affixData.getLanguage(), affixData.getFlagParsingStrategy());

		final String pluralFlagsValue = rulesLoader.readProperty("pluralFlags");
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		pluralFlags = (pluralFlagsValue != null? strategy.parseFlags(pluralFlagsValue): null);

		PATTERN_NON_VANISHING_EL = PatternHelper.pattern(rulesLoader.readProperty("patternNonVanishingEl"), Pattern.CASE_INSENSITIVE);
		PATTERN_VANISHING_EL_NEXT_TO_CONSONANT = PatternHelper.pattern(rulesLoader.readProperty("patternVanishingElNextToConsonant"),
			Pattern.CASE_INSENSITIVE);
		PATTERN_PHONEME_CIJJHNHIV = PatternHelper.pattern(rulesLoader.readProperty("patternPhonemeCIJJHNHIV"), Pattern.CASE_INSENSITIVE);
		PATTERN_NORTHERN_PLURAL = PatternHelper.pattern(rulesLoader.readProperty("patternNorthernPlural"), Pattern.CASE_INSENSITIVE);

		PLURAL_NOUN_MASCULINE_RULE = rulesLoader.readProperty("masculinePluralNoun");
		VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL = rulesLoader.readProperty("variantTransformationAtEndVanishingEl");
		NORTHERN_PLURAL_RULE = rulesLoader.readProperty("northernPlural");
		NORTHERN_PLURAL_STRESSED_RULE = rulesLoader.readProperty("northernPluralStressed");
		NORTHERN_PLURAL_EXCEPTION = rulesLoader.readProperty("northernPluralException");
	}

	private boolean hasPluralFlag(final Production production){
		return (pluralFlags != null && production.hasContinuationFlag(pluralFlags));
	}

	@Override
	public void checkProduction(final Production production) throws IllegalArgumentException{
		super.checkProduction(production);

		stressCheck(production);

		vanishingElCheck(production);

		incompatibilityCheck(production);

		if(production.hasNonTerminalContinuationFlags(affixData::isTerminalAffix) && !production.hasPartOfSpeech(POS_VERB) && !production.hasPartOfSpeech(POS_ADVERB))
			northernPluralCheck(production);

		finalSonorizationCheck(production);

		orthographyCheck(production);
	}

	private void stressCheck(final Production production) throws IllegalArgumentException{
		final String derivedWord = production.getWord();
		final String unmarkedDefaultStressWord = WordVEC.unmarkDefaultStress(derivedWord);
		if(!derivedWord.equals(unmarkedDefaultStressWord))
			throw new IllegalArgumentException(WORD_WITH_UNNECESSARY_STRESS.format(new Object[]{derivedWord}));
	}

	private void vanishingElCheck(final Production production) throws IllegalArgumentException{
		final String derivedWord = production.getWord();
		if(derivedWord.contains(GraphemeVEC.GRAPHEME_L_STROKE)){
			if(!derivedWord.toLowerCase(Locale.ROOT).startsWith(GraphemeVEC.GRAPHEME_L)
					&& PatternHelper.find(StringUtils.replace(derivedWord, "–", StringUtils.EMPTY), PATTERN_NON_VANISHING_EL))
				throw new IllegalArgumentException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_NON_VAN_EL.format(new Object[]{derivedWord}));
			if(production.hasContinuationFlag(NORTHERN_PLURAL_RULE))
				throw new IllegalArgumentException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_RULE.format(new Object[]{NORTHERN_PLURAL_RULE,
					NORTHERN_PLURAL_STRESSED_RULE, derivedWord}));
			if(derivedWord.contains(GraphemeVEC.GRAPHEME_D_STROKE) || derivedWord.contains(GraphemeVEC.GRAPHEME_T_STROKE))
				throw new IllegalArgumentException(WORD_WITH_VAN_EL_CANNOT_CONTAIN_DH_OR_TH.format(new Object[]{derivedWord}));
		}
		if(PatternHelper.find(derivedWord, PATTERN_VANISHING_EL_NEXT_TO_CONSONANT))
			throw new IllegalArgumentException(WORD_WITH_VAN_EL_NEAR_CONSONANT.format(new Object[]{derivedWord}));
	}

	private void incompatibilityCheck(final Production production) throws IllegalArgumentException{
		if(production.hasContinuationFlag(VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL)
				&& (production.getContinuationFlagCount() != 2 || !production.hasContinuationFlag(PLURAL_NOUN_MASCULINE_RULE)))
			throw new IllegalArgumentException(WORD_WITH_RULE_CANNOT_HAVE_RULES_OTHER_THAN.format(new Object[]{
				VARIANT_TRANSFORMATIONS_END_RULE_VANISHING_EL, PLURAL_NOUN_MASCULINE_RULE}));
	}

	private void northernPluralCheck(final Production production) throws IllegalArgumentException{
		if(hasToCheckForNorthernPlural(production)){
			final String word = production.getWord();
			final String rule = getRuleToCheckNorthernPlural(word);
			final boolean canHaveNorthernPlural = canHaveNorthernPlural(production, rule);
			final boolean hasNorthernPluralFlag = production.hasContinuationFlag(rule);
			if(canHaveNorthernPlural && !hasNorthernPluralFlag)
				throw new IllegalArgumentException(NORTHERN_PLURAL_MISSING.format(new Object[]{rule}));
			if(!canHaveNorthernPlural && hasNorthernPluralFlag)
				throw new IllegalArgumentException(NORTHERN_PLURAL_NOT_NEEDED.format(new Object[]{NORTHERN_PLURAL_RULE,
					NORTHERN_PLURAL_STRESSED_RULE}));
		}
	}

	private boolean hasToCheckForNorthernPlural(final Production production){
		return (!production.hasPartOfSpeech(POS_ARTICLE) && !production.hasPartOfSpeech(POS_PRONOUN) && !production.hasPartOfSpeech(POS_PROPER_NOUN)
			&& hyphenator.hyphenate(production.getWord()).countSyllabes() > 1);
	}

	private String getRuleToCheckNorthernPlural(final String word){
		final List<String> subwords = hyphenator.splitIntoCompounds(word);
		return (!WordVEC.hasStressedGrapheme(subwords.get(subwords.size() - 1)) || PatternHelper.find(word, PATTERN_NORTHERN_PLURAL)?
			NORTHERN_PLURAL_RULE: NORTHERN_PLURAL_STRESSED_RULE);
	}

	private boolean canHaveNorthernPlural(final Production production, final String rule){
		final String word = production.getWord();
		final boolean hasPluralFlag = hasPluralFlag(production);
		return (hasPluralFlag && !word.contains(GraphemeVEC.GRAPHEME_L_STROKE)
			&& !word.endsWith(NORTHERN_PLURAL_EXCEPTION) && affixData.isAffixProductive(word, rule));
	}

	private void orthographyCheck(final Production production) throws IllegalArgumentException{
		if(hasToCheckForOrthographyAndSyllabation(production)){
			String word = production.getWord();
			if(!rulesLoader.containsUnsyllabableWords(word) && !rulesLoader.containsMultipleAccentedWords(word)){
				word = word.toLowerCase(Locale.ROOT);
				orthographyCheck(word);
			}
		}
	}

	private boolean hasToCheckForOrthographyAndSyllabation(final Production production){
		return ((rulesLoader.isEnableVerbSyllabationCheck() || !production.hasPartOfSpeech(POS_VERB))
			&& !production.hasPartOfSpeech(POS_NUMERAL_LATIN) && !production.hasPartOfSpeech(POS_UNIT_OF_MEASURE));
	}

	private void orthographyCheck(final String word) throws IllegalArgumentException{
		final String correctedDerivedWord = orthography.correctOrthography(word);
		if(!correctedDerivedWord.equals(word))
			throw new IllegalArgumentException(WORD_IS_MISSPELLED.format(new Object[]{word, correctedDerivedWord}));
	}

	@Override
	protected void checkCompoundProduction(final String subword, final int subwordIndex, final Production production)
			throws IllegalArgumentException{
		if(subwordIndex == 0)
			accentCheck(subword, production);

		ciuiCheck(subword, production);
	}

	private void accentCheck(final String subword, final Production production) throws IllegalArgumentException{
		if(!rulesLoader.containsMultipleAccentedWords(subword)){
			final int accents = WordVEC.countAccents(subword);
			if(!rulesLoader.isWordCanHaveMultipleAccents() && accents > 1)
				throw new IllegalArgumentException(WORD_HAS_MULTIPLE_ACCENTS.format(new Object[]{production.getWord()}));

			final String appliedRuleFlag = getLastAppliedRule(production);
			if(appliedRuleFlag != null){
				//retrieve last applied rule
				if(accents == 0 && rulesLoader.containsHasToContainAccent(appliedRuleFlag))
					throw new IllegalArgumentException(WORD_HAS_MISSING_ACCENT.format(new Object[]{production.getWord(), appliedRuleFlag}));
				if(accents > 0 && rulesLoader.containsCannotContainAccent(appliedRuleFlag))
					throw new IllegalArgumentException(WORD_HAS_PRESENT_ACCENT.format(new Object[]{production.getWord(), appliedRuleFlag}));
			}
		}
	}

	private String getLastAppliedRule(final Production production){
		String appliedRuleFlag = null;
		final List<AffixEntry> appliedRules = production.getAppliedRules();
		if(appliedRules != null)
			appliedRuleFlag = appliedRules.get(appliedRules.size() - 1)
				.getFlag();
		return appliedRuleFlag;
	}

	private void ciuiCheck(final String subword, final Production production) throws IllegalArgumentException{
		if(!production.hasPartOfSpeech(POS_NUMERAL_LATIN)){
			final String phonemizedSubword = GraphemeVEC.handleJHJWIUmlautPhonemes(subword);
			if(PatternHelper.find(phonemizedSubword, PATTERN_PHONEME_CIJJHNHIV))
				throw new IllegalArgumentException(WORD_CANNOT_HAVE_CIJJHNHIV.format(new Object[]{production.getWord()}));
		}
	}

//	private void variantIncompatibilityCheck(final RuleProductionEntry production, final Set<MatcherEntry> checks)
//			throws IllegalArgumentException{
//		if(canContainsVanishingEl(production.getWord()))
//			for(final MatcherEntry entry : checks)
//				entry.match(production);
//	}
//
//	//(^[ʼ']?l|[aeiouàèéíòóú]l)[aeiouàèéíòóú]
//	private static boolean canContainsVanishingEl(final String word){
//		boolean result = false;
//		final int size = word.length();
//		if(size > 1){
//			int index = (WordVEC.isApostrophe(word.charAt(0))? 1: 0);
//			if(index + 1 < size){
//				final char chr = word.charAt(index);
//				result = (chr == 'l' && WordVEC.isVowel(word.charAt(index + 1)));
//				while(!result){
//					index = WordVEC.getFirstVowelIndex(word, index);
//					if(index < 0 || index + 2 >= size)
//						break;
//
//					if(word.charAt(index + 1) == 'l' && WordVEC.isVowel(word.charAt(index + 2)))
//						result = true;
//
//					index ++;
//				}
//			}
//		}
//		return result;
//	}

	private void finalSonorizationCheck(final Production production) throws IllegalArgumentException{
//		if(!production.hasProductionRules()&& !production.isPartOfSpeech(POS_VERB) && !production.isPartOfSpeech(POS_PROPER_NOUN)){
//			final boolean hasFinalSonorizationFlag = production.hasContinuationFlag(FINAL_SONORIZATION_RULE);
//			final boolean canHaveFinalSonorization = (!production.getWord().toLowerCase(Locale.ROOT).contains(GraphemeVEC.L_STROKE_GRAPHEME) && wordGenerator.isAffixProductive(production.getWord(), FINAL_SONORIZATION_RULE));
//			if(canHaveFinalSonorization ^ hasFinalSonorizationFlag){
//				if(canHaveFinalSonorization)
//					throw new IllegalArgumentException("Final sonorization missing for " + production.getWord() + ", add " + FINAL_SONORIZATION_RULE);
//				if(!canHaveFinalSonorization)
//					throw new IllegalArgumentException("Final sonorization not needed for " + production.getWord() + ", remove " + FINAL_SONORIZATION_RULE);
//			}
//		}
	}


	@Override
	public boolean isConsonant(final char chr){
		return WordVEC.isConsonant(chr);
	}

	@Override
	public boolean shouldBeProcessedForMinimalPair(final Production production){
		final String word = production.getWord();
		return (word.length() >= MINIMAL_PAIR_MINIMUM_LENGTH
			&& word.indexOf('ƚ') < 0
			&& word.indexOf('ɉ') < 0
			&& (production.hasPartOfSpeech(POS_NOUN)
			|| production.hasPartOfSpeech(POS_ADJECTIVE)
			|| production.hasPartOfSpeech(POS_ADJECTIVE_POSSESSIVE)
			|| production.hasPartOfSpeech(POS_ADJECTIVE_DEMONSTRATIVE)
			|| production.hasPartOfSpeech(POS_ADJECTIVE_IDENTIFICATIVE)
			|| production.hasPartOfSpeech(POS_ADJECTIVE_INTERROGATIVE)
			|| production.hasPartOfSpeech(POS_QUANTIFIER)
			|| production.hasPartOfSpeech(POS_PRONOUN)
			|| production.hasPartOfSpeech(POS_PREPOSITION)
			|| production.hasPartOfSpeech(POS_ADVERB)
			|| production.hasPartOfSpeech(POS_CONJUNCTION)));
	}

}
