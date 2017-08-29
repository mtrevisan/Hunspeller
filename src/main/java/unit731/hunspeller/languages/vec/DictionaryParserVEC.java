package unit731.hunspeller.languages.vec;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.AffixEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.RuleProductionEntry;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.hyphenation.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;


public class DictionaryParserVEC extends DictionaryParser{

	private static final String VANISHING_EL = "ƚ";

	private static final Matcher MISMATCHED_VARIANTS = Pattern.compile("ƚ.*[ŧđ]|[ŧđ].*ƚ").matcher(StringUtils.EMPTY);
	private static final Matcher MULTIPLE_ACCENTS = Pattern.compile("([^àèéíòóú]*[àèéíòóú]){2,}").matcher(StringUtils.EMPTY);

	private static final Matcher L_BETWEEN_VOWELS = Pattern.compile("l i l$").matcher(StringUtils.EMPTY);
	private static final Matcher D_BETWEEN_VOWELS = Pattern.compile("d[ou]ra? [ou]ra?\\/[^ ]+ \\[aei\\]d[ou]ra?$").matcher(StringUtils.EMPTY);
	private static final Matcher NHIV = Pattern.compile("[cijɉñ]i[aàeèéiíoòóuú]").matcher(StringUtils.EMPTY);
	private static final Matcher CIUI = Pattern.compile("ciuí$").matcher(StringUtils.EMPTY);
	
	private static final Matcher VANISHING_L_AND_NON_VANISHING_DIMINUTIVE = Pattern.compile("ƚ[^/]+[^a]/[^\\t\\n]*&0").matcher(StringUtils.EMPTY);
	private static final Matcher NON_VANISHING_L_AND_VANISHING_DIMINUTIVE = Pattern.compile("^[^ƚ/]+[^a]/[^\\t\\n]*&1").matcher(StringUtils.EMPTY);
	private static final Matcher MISSING_PLURAL_AFTER_N_OR_L = Pattern.compile("^[^ƚ]*[eaouèàòéóú][ln]\\/[^ZUu\\t]+\\t").matcher(StringUtils.EMPTY);
	private static final Matcher ENDS_IN_MAN = Pattern.compile("man\\/").matcher(StringUtils.EMPTY);

	private static final String TAB = "\t";

	private static final String POS_NOUN = "noun";
	private static final String POS_PROPER_NOUN = "proper_noun";
	private static final String POS_VERB = "verb";
	private static final String POS_ADJECTIVE = "adjective";
	private static final String POS_ADJECTIVE_POSSESSIVE = "adjective_possessive";
	private static final String POS_ADJECTIVE_DEMONSTRATIVE = "adjective_demonstrative";
	private static final String POS_ADJECTIVE_IDENTIFICATIVE = "adjective_identificative";
	private static final String POS_ADJECTIVE_INTERROGATIVE = "adjective_interrogative";
	private static final String POS_QUANTIFIER = "quantifier";
	private static final String POS_NUMERAL_LATIN = "numeral_latin";
	private static final String POS_NUMERAL_CARDENAL = "numeral_cardenal";
	private static final String POS_NUMERAL_ORDENAL = "numeral_ordenal";
	private static final String POS_NUMERAL_COLLECTIVE = "numeral_collective";
	private static final String POS_NUMERAL_FRACTIONAL = "numeral_fractional";
	private static final String POS_NUMERAL_MULTIPLICATIVE = "numeral_multiplicative";
	private static final String POS_ARTICLE = "article";
	private static final String POS_PRONOUN = "pronoun";
	private static final String POS_PREPOSITION = "preposition";
	private static final String POS_ADVERB = "adverb";
	private static final String POS_CONJUNCTION = "conjunction";
	private static final String POS_PREFIX = "prefix";
	private static final String POS_INTERJECTION = "interjection";
	private static final String POS_UNIT_OF_MEASURE = "unit_of_measure";
	
	private static final Set<String> PART_OF_SPEECH = new HashSet<>();
	static{
		PART_OF_SPEECH.add(POS_NOUN);
		PART_OF_SPEECH.add(POS_PROPER_NOUN);
		PART_OF_SPEECH.add(POS_VERB);
		PART_OF_SPEECH.add(POS_ADJECTIVE);
		PART_OF_SPEECH.add(POS_ADJECTIVE_POSSESSIVE);
		PART_OF_SPEECH.add(POS_ADJECTIVE_DEMONSTRATIVE);
		PART_OF_SPEECH.add(POS_ADJECTIVE_IDENTIFICATIVE);
		PART_OF_SPEECH.add(POS_ADJECTIVE_INTERROGATIVE);
		PART_OF_SPEECH.add(POS_QUANTIFIER);
		PART_OF_SPEECH.add(POS_NUMERAL_LATIN);
		PART_OF_SPEECH.add(POS_NUMERAL_CARDENAL);
		PART_OF_SPEECH.add(POS_NUMERAL_ORDENAL);
		PART_OF_SPEECH.add(POS_NUMERAL_COLLECTIVE);
		PART_OF_SPEECH.add(POS_NUMERAL_FRACTIONAL);
		PART_OF_SPEECH.add(POS_NUMERAL_MULTIPLICATIVE);
		PART_OF_SPEECH.add(POS_ARTICLE);
		PART_OF_SPEECH.add(POS_PRONOUN);
		PART_OF_SPEECH.add(POS_PREPOSITION);
		PART_OF_SPEECH.add(POS_ADVERB);
		PART_OF_SPEECH.add(POS_CONJUNCTION);
		PART_OF_SPEECH.add(POS_PREFIX);
		PART_OF_SPEECH.add(POS_INTERJECTION);
		PART_OF_SPEECH.add(POS_UNIT_OF_MEASURE);
	}


	public DictionaryParserVEC(File dicFile, WordGenerator wordGenerator, Charset charset){
		super(dicFile, wordGenerator, charset);
	}

	@Override
	protected void checkLineLanguageSpecific(String line) throws IllegalArgumentException{
		if(!line.contains(TAB))
			throw new IllegalArgumentException("Line does not contains data fields");
		if(VANISHING_L_AND_NON_VANISHING_DIMINUTIVE.reset(line).find())
			throw new IllegalArgumentException("Cannot use &0 rule with vanishing el, use &1");
		if(NON_VANISHING_L_AND_VANISHING_DIMINUTIVE.reset(line).find())
			throw new IllegalArgumentException("Cannot use &1 rule with non-vanishing el, use &0");
		if(!line.contains(POS_VERB) && !line.contains(POS_ARTICLE) && !line.contains(POS_ADVERB) && !line.contains(POS_PRONOUN)
				&& !ENDS_IN_MAN.reset(line).find() && MISSING_PLURAL_AFTER_N_OR_L.reset(line).find())
			throw new IllegalArgumentException("Plural missing after n or l, add u0 or U0");
	}

	@Override
	protected void checkProductionLanguageSpecific(DictionaryEntry dicEntry, RuleProductionEntry production) throws IllegalArgumentException{
		String derivedWord = production.getWord();
		if(MISMATCHED_VARIANTS.reset(derivedWord).find())
			throw new IllegalArgumentException("Word with a vanishing el cannot contain characters from another variant: " + derivedWord);
		if(derivedWord.contains(VANISHING_EL) && production.containsRuleFlag("U0"))
			throw new IllegalArgumentException("Word with a vanishing el cannot contain rule U0:" + derivedWord);

		if(production.containsRuleFlag("B0") && production.containsRuleFlag("&0"))
			throw new IllegalArgumentException("Word with rule B0 cannot rule &0:" + derivedWord);

		String[] dataFields = dicEntry.getDataFields();
		for(String dataField : dataFields)
			if(dataField.startsWith(WordGenerator.TAG_PART_OF_SPEECH) && !PART_OF_SPEECH.contains(dataField.substring(3)))
				throw new IllegalArgumentException("Word has an unknown Part Of Speech: " + dataField);

		String[] splittedWords = derivedWord.split(HyphenationParser.HYPHEN_MINUS);
		for(String subword : splittedWords){
			if(MULTIPLE_ACCENTS.reset(subword).find())
				throw new IllegalArgumentException("Word cannot have multiple accents: " + derivedWord);
			if(Word.isStressed(subword) && !subword.equals(Word.unmarkDefaultStress(subword))){
				boolean elBetweenVowelsRemoval = production.getRules().stream()
					.map(AffixEntry::toString)
					.map(L_BETWEEN_VOWELS::reset)
					.anyMatch(Matcher::find);
				if(!elBetweenVowelsRemoval)
					throw new IllegalArgumentException("Word cannot have an accent here: " + derivedWord);
			}
			if(!dicEntry.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_NUMERAL_LATIN) && NHIV.reset(subword).find()
					&& !CIUI.reset(subword).find()){
				boolean dBetweenVowelsRemoval = production.getRules().stream()
					.map(AffixEntry::toString)
					.map(D_BETWEEN_VOWELS::reset)
					.anyMatch(Matcher::find);
				if(!dBetweenVowelsRemoval)
					throw new IllegalArgumentException("Word cannot have [cijɉñ]iV: " + derivedWord);
			}
		}

		//check syllabation
		if(hyphenationParser != null && derivedWord.length() > 1 && !derivedWord.contains(HyphenationParser.HYPHEN_MINUS)){
			Hyphenation hyphenation = hyphenationParser.hyphenate(derivedWord);
			if(hyphenation.hasErrors())
				throw new IllegalArgumentException("Word is not syllabable (" + String.join(HyphenationParser.HYPHEN, hyphenation.getSyllabes())
					+ "): " + derivedWord);
		}
	}

	@Override
	public String prepareTextForFilter(String text){
		text = super.prepareTextForFilter(text);

		text = StringUtils.replaceAll(text, "ì", "í");
		text = StringUtils.replaceAll(text, "i/", "í");
		text = StringUtils.replaceAll(text, "o/", "ó");
		text = StringUtils.replaceAll(text, "ù", "ú");
		text = StringUtils.replaceAll(text, "u/", "ú");
		text = StringUtils.replaceAll(text, "dh", "(dh|đ)");
		text = StringUtils.replaceAll(text, "jh", "(jh|ɉ)");
		text = StringUtils.replaceAll(text, "lh", "(lh|ƚ)");
		text = StringUtils.replaceAll(text, "nh", "(nh|ñ)");
		text = StringUtils.replaceAll(text, "th", "(th|ŧ)");

		return text;
	}

}
