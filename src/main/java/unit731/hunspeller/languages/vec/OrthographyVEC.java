package unit731.hunspeller.languages.vec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.PatternHelper;


public class OrthographyVEC extends Orthography{

	private static final String[] STRESS_CODES = new String[]{"a\\", "e\\", "o\\", "e/", "i/", "i\\", "ì", "o/", "u/", "u\\", "ù"};
	private static final String[] TRUE_STRESS = new String[]{"à", "è", "ò", "é", "í", "í", "í", "ó", "ú", "ú", "ú"};

	private static final String[] EXTENDED_CHARS = new String[]{"dh", "jh", "lh", "nh", "th"};
	private static final String[] TRUE_CHARS = new String[]{"đ", "ɉ", "ƚ", "ñ", "ŧ"};

	private static final String[] MB_MP = new String[]{"mb", "mp"};
	private static final String[] NB_NP = new String[]{"nb", "np"};

	private static final Pattern PATTERN_REMOVE_H_FROM_NOT_FH = PatternHelper.pattern("(?<!f)h(?!aeeioouàéèíóòú)");

	private static final Pattern PATTERN_J_INTO_I = PatternHelper.pattern("^" + GraphemeVEC.PHONEME_JJH + "(?=[^aeiouàèéí"
		+ GraphemeVEC.PHONEME_I_UMLAUT + "òóúh])");
	private static final Pattern PATTERN_I_INITIAL_INTO_J = PatternHelper.pattern("^i(?=[aeiouàèéíòóú])");
	private static final Pattern PATTERN_I_INSIDE_INTO_J = PatternHelper.pattern("([aeiouàèéíòóú])i(?=[aeiouàèéíòóú])");
	private static final List<Pattern> PATTERN_I_INSIDE_INTO_J_FALSE_POSITIVES = Arrays.asList(
		PatternHelper.pattern("b[ae]roi[aeèi]r"),
		PatternHelper.pattern("re[sŧ]ei[ou]r[aeio]?")
	);
	private static final Pattern PATTERN_I_INSIDE_INTO_J_EXCLUSIONS = PatternHelper.pattern("[aeiouàèéíòóú]i(o|([oó]n|on-)([gmnstv].{1,3}|[ei])?([lƚ][oiae])?|é(-?[ou])?|e[dg]e(-[ou])?|omi|ent[eoi]?-?([gmnstv].{1,3})?([lƚ][oiae])?|inti)$");
	private static final Pattern PATTERN_LH_INITIAL_INTO_L = PatternHelper.pattern("^ƚ(?=[^ʼ'aeiouàèéíòóújw])");
	private static final Pattern PATTERN_LH_INSIDE_INTO_L = PatternHelper.pattern("([^ʼ'aeiouàèéíòóú–-])ƚ(?=[aeiouàèéíòóújw])|([aeiouàèéíòóú])ƚ(?=[^aeiouàèéíòóújw])");
	private static final Pattern PATTERN_X_INTO_S = PatternHelper.pattern(GraphemeVEC.GRAPHEME_X + "(?=[cfkpt])");
	private static final Pattern PATTERN_S_INTO_X = PatternHelper.pattern(GraphemeVEC.GRAPHEME_S + "(?=([mnñbdg" + GraphemeVEC.PHONEME_JJH
		+ "ɉsvrlŧ]))");

	private static final Pattern PATTERN_MORPHOLOGICAL = PatternHelper.pattern("([c" + GraphemeVEC.PHONEME_JJH + "ñ])i([aeiou])");

	private static final Pattern PATTERN_CONSONANT_GEMINATES = PatternHelper.pattern("([^aeiou]){1}\\1+");

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new OrthographyVEC();
	}


	private OrthographyVEC(){}

	public static Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	public String correctOrthography(String word){
		//correct stress
		word = StringUtils.replaceEach(word, STRESS_CODES, TRUE_STRESS);

		//correct h occurrences after d, j, l, n, t
		word = StringUtils.replaceEach(word, EXTENDED_CHARS, TRUE_CHARS);

		//remove other occurrences of h not into fhV
		if(!GraphemeVEC.GRAPHEME_H.equals(word))
			word = PatternHelper.replaceAll(word, PATTERN_REMOVE_H_FROM_NOT_FH, StringUtils.EMPTY);

		//correct mb/mp occurrences into nb/np
		word = StringUtils.replaceEach(word, MB_MP, NB_NP);

		word = GraphemeVEC.handleJHJWIUmlautPhonemes(word);

		word = correctIJOccurrences(word);

		//correct lh occurrences into l not at the beginning of a word and not between vowels
		word = PatternHelper.replaceAll(word, PATTERN_LH_INITIAL_INTO_L, GraphemeVEC.GRAPHEME_L);
		word = PatternHelper.replaceAll(word, PATTERN_LH_INSIDE_INTO_L, "$1l");
		//correct x occurrences into s prior to c, f, k, p, t
		//correct s occurrences into x prior to m, n, ñ, b, d, g, j, ɉ, s, v, r, l
		word = PatternHelper.replaceAll(word, PATTERN_X_INTO_S, GraphemeVEC.GRAPHEME_S);
		word = PatternHelper.replaceAll(word, PATTERN_S_INTO_X, GraphemeVEC.GRAPHEME_X);

		//correct morphological errors
		word = PatternHelper.replaceAll(word, PATTERN_MORPHOLOGICAL, "$1$2");

		word = GraphemeVEC.rollbackJHJWIUmlautPhonemes(word);

		//eliminate consonant geminates
		word = PatternHelper.replaceAll(word, PATTERN_CONSONANT_GEMINATES, "$1");

		word = correctApostrophes(word);

		return word;
	}

	private String correctIJOccurrences(String word){
		//correct i occurrences into j at the beginning of a word followed by a vowel and between vowels, correcting also the converse
		word = PatternHelper.replaceAll(word, PATTERN_J_INTO_I, GraphemeVEC.GRAPHEME_I);
		word = PatternHelper.replaceAll(word, PATTERN_I_INITIAL_INTO_J, GraphemeVEC.PHONEME_JJH);
		boolean iInsideIntoJFalsePositive = false;
		for(final Pattern p : PATTERN_I_INSIDE_INTO_J_FALSE_POSITIVES)
			if(PatternHelper.find(word, p)){
				iInsideIntoJFalsePositive = true;
				break;
			}
		if(!iInsideIntoJFalsePositive && !PatternHelper.find(word, PATTERN_I_INSIDE_INTO_J_EXCLUSIONS))
			word = PatternHelper.replaceAll(word, PATTERN_I_INSIDE_INTO_J, "$1" + GraphemeVEC.PHONEME_JJH);
		return word;
	}

	@Override
	public boolean[] getSyllabationErrors(final List<String> syllabes){
		final int size = syllabes.size();
		final boolean[] errors = new boolean[size];
		for(int i = 0; i < size; i ++){
			final String syllabe = syllabes.get(i);
			errors[i] = (!syllabe.contains(HyphenationParser.APOSTROPHE) && !syllabe.contains(HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK)
				&& !syllabe.equals(HyphenationParser.MINUS_SIGN) && !StringUtils.containsAny(syllabe, WordVEC.VOWELS));
		}
		return errors;
	}

	@Override
	public List<Integer> getStressIndexFromLast(final List<String> syllabes){
		final List<Integer> indexes = new ArrayList<>();
		final int size = syllabes.size() - 1;
		for(int i = 0; i <= size; i ++)
			if(hasStressedGrapheme(syllabes.get(size - i)))
				indexes.add(i);
		return indexes;
	}

	@Override
	public int countGraphemes(final String word){
		return WordVEC.countGraphemes(word);
	}

	@Override
	public String markDefaultStress(final String word){
		return WordVEC.markDefaultStress(word);
	}

	@Override
	public boolean hasStressedGrapheme(final String word){
		return WordVEC.hasStressedGrapheme(word);
	}

}
