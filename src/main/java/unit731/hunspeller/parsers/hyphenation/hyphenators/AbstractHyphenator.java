package unit731.hunspeller.parsers.hyphenation.hyphenators;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.radixtree.RadixTree;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationBreak;
import unit731.hunspeller.parsers.hyphenation.vos.HyphenationOptions;
import unit731.hunspeller.services.PatternHelper;


public abstract class AbstractHyphenator implements HyphenatorInterface{

	protected final HyphenationParser hypParser;
	private final String breakCharacter;


	protected AbstractHyphenator(HyphenationParser hypParser, String breakCharacter){
		Objects.requireNonNull(hypParser);
		Objects.requireNonNull(breakCharacter);

		this.hypParser = hypParser;
		this.breakCharacter = breakCharacter;
	}

	/**
	 * Performs hyphenation
	 * NOTE: Calling the method {@link unit731.hunspeller.languages.Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @return the hyphenation object(s)
	 */
	@Override
	public Hyphenation hyphenate(String word){
		return hyphenate(word, hypParser.getPatterns());
	}

	/**
	 * Performs hyphenation including an additional rule
	 * NOTE: Calling the method {@link unit731.hunspeller.languages.Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @param addedRule	Rule to add to the set of rules that will generate the hyphenation
	 * @param level	The level to add the rule to
	 * @return the hyphenation object
	 */
	@Override
	public Hyphenation hyphenate(String word, String addedRule, HyphenationParser.Level level){
		String key = HyphenationParser.getKeyFromData(addedRule);
		Map<HyphenationParser.Level, RadixTree<String, String>> patterns = hypParser.getPatterns();
		Hyphenation hyph = null;
		if(!patterns.get(level).containsKeyPrefixedBy(key)){
			patterns.get(level).put(key, addedRule);

			hyph = hyphenate(word, patterns);

			patterns.get(level).removePrefixedBy(key);
		}
		return hyph;
	}

	/**
	 * Performs hyphenation
	 *
	 * @param word	String to hyphenate
	 * @param patterns	The radix tree containing the patterns
	 * @return the hyphenation object
	 */
	private Hyphenation hyphenate(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns){
		//apply first level hyphenation
		HyphenationBreak hyphBreak = hyphenate(word, patterns, HyphenationParser.Level.NON_COMPOUND,
			hypParser.getOptParser().getNonCompoundOptions());

		List<String> compounds = createHyphenatedWord(word, hyphBreak);
		List<String> syllabes = compounds;
		List<String> rules = hyphBreak.getRules();

		//if there is a second level
		if(hypParser.isSecondLevelPresent()){
			List<String> syllabes2ndLevel = new ArrayList<>();
			List<String> rules2ndLevel = new ArrayList<>();

			//apply second level hyphenation for the word parts
			int i = 0;
			int parentRulesSize = rules.size();
			for(String compound : compounds){
				HyphenationBreak subHyph = hyphenate(compound, patterns, HyphenationParser.Level.COMPOUND,
					hypParser.getOptParser().getCompoundOptions());

				syllabes2ndLevel.addAll(createHyphenatedWord(compound, subHyph));
				rules2ndLevel.addAll(subHyph.getRules());
				if(i < parentRulesSize)
					rules2ndLevel.add(rules.get(i ++));
			}

			syllabes = syllabes2ndLevel;
			rules = rules2ndLevel;
		}

		//enforce no-hyphens
		hyphBreak.enforceNoHyphens(syllabes, hypParser.getOptParser().getNoHyphen());

		boolean[] errors = hypParser.getOrthography().getSyllabationErrors(syllabes);

		return new Hyphenation(syllabes, compounds, rules, errors, breakCharacter);
	}

	/**
	 * Performs hyphenation
	 *
	 * @param word	String to hyphenate
	 * @param patterns	The radix tree containing the patterns
	 * @param level	Level at which to hyphenate
	 * @param options	The hyphenation options
	 * @return the hyphenation breakpoints object
	 */
	private HyphenationBreak hyphenate(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns,
			HyphenationParser.Level level, HyphenationOptions options){
		//clear already present word boundaries' characters
		word = PatternHelper.clear(word, HyphenationParser.PATTERN_WORD_BOUNDARIES);
		int wordSize = word.length();

		String customHyphenation = hypParser.getCustomHyphenations().get(level).get(word);
		HyphenationBreak hyphBreak;
		if(customHyphenation != null){
			//hyphenation is custom, extract break point positions:
			String[] hyphenations = customHyphenation.split(HyphenationParser.HYPHEN_EQUALS);
			Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>(wordSize);
			int charCount = Normalizer.normalize(hyphenations[0], Normalizer.Form.NFKC).length();
			for(int i = 1; i < hyphenations.length; i ++){
				String customRule = hyphenations[i - 1] + "1" + hyphenations[i];
				indexesAndRules.put(charCount, Pair.of(1, customRule));

				charCount += Normalizer.normalize(hyphenations[i], Normalizer.Form.NFKC).length();
			}
			hyphBreak = new HyphenationBreak(indexesAndRules, wordSize);
		}
		else if(Normalizer.normalize(word, Normalizer.Form.NFKC).length() < options.getMinimumLength())
			//ignore short words (early out):
			hyphBreak = new HyphenationBreak(Collections.<Integer, Pair<Integer, String>>emptyMap(), wordSize);
		else
			hyphBreak = calculateBreakpoints(word, patterns.get(level), options);

		return hyphBreak;
	}

	protected abstract HyphenationBreak calculateBreakpoints(String word, RadixTree<String, String> patterns, HyphenationOptions options);

	@Override
	public List<String> splitIntoCompounds(String word){
		List<String> response;
		if(hypParser.isSecondLevelPresent()){
			//apply first level hyphenation non-compound
			HyphenationBreak hyphBreak = hyphenate(word, hypParser.getPatterns(), HyphenationParser.Level.NON_COMPOUND,
				hypParser.getOptParser().getNonCompoundOptions());
			response = createHyphenatedWord(word, hyphBreak);

			manageNoHyphen(response);
		}
		else if(hypParser.getPatternNoHyphen() != null)
			//apply retro-compatibility word separators
			response = Arrays.asList(PatternHelper.split(word, hypParser.getPatternNoHyphen()));
		else
			response = Collections.<String>emptyList();
		return response;
	}

	private void manageNoHyphen(List<String> response){
		for(String nohyp : hypParser.getOptParser().getNoHyphen()){
			int nohypLength = nohyp.length();
			if(isStarting(nohyp)){
				if(response.get(0).equals(nohyp.substring(1)))
					response.remove(0);
			}
			else if(isEnding(nohyp)){
				if(response.get(response.size() - 1).equals(nohyp.substring(0, nohypLength - 1)))
					response.remove(response.size() - 1);
			}
			else{
				Iterator<String> itr = response.iterator();
				while(itr.hasNext())
					if(nohyp.equals(itr.next()))
						itr.remove();
			}
		}
	}

	private boolean isStarting(String key){
		return (key.charAt(0) == '^');
	}

	private boolean isEnding(String key){
		return (key.charAt(key.length() - 1) == '$');
	}

	protected List<String> createHyphenatedWord(String word, HyphenationBreak hyphBreak){
		List<String> result = new ArrayList<>();

		int startIndex = 0;
		int size = word.length();
		int after = 0;
		String addAfter = null;
		for(int endIndex = 0; endIndex < size; endIndex ++)
			if(hyphBreak.isBreakpoint(endIndex)){
				String subword = word.substring(startIndex, endIndex);

				if(StringUtils.isNotBlank(addAfter)){
					//append first characters to next subword
					subword = addAfter + subword.substring(after);
					after = 0;
					addAfter = null;
				}

				//manage augmented patterns:
				String augmentedPatternData = hyphBreak.getRule(endIndex);
				if(augmentedPatternData != null && HyphenationParser.isAugmentedRule(augmentedPatternData)){
					int index = HyphenationParser.getIndexOfBreakpoint(PatternHelper.clear(augmentedPatternData,
						HyphenationParser.PATTERN_WORD_INITIAL));

					Matcher m = HyphenationParser.PATTERN_AUGMENTED_RULE.matcher(augmentedPatternData);
					if(m.find()){
						String addBefore = m.group(HyphenationParser.PARAM_ADD_BEFORE);
						addAfter = m.group(HyphenationParser.PARAM_ADD_AFTER);
						String start = m.group(HyphenationParser.PARAM_START);
						String cut = m.group(HyphenationParser.PARAM_CUT);
						if(start == null){
							String rule = m.group(HyphenationParser.PARAM_RULE);
							start = Integer.toString(1);
							cut = Integer.toString(PatternHelper.clear(rule, HyphenationParser.PATTERN_POINTS_AND_NUMBERS).length());
						}

						//remove last characters from subword
						//  ll3a/aa=b,2,2
						//syll able
						//sylaa-bble
						int delta = index - Integer.parseInt(start) + 1;
						int end = subword.length() - delta;
						after = Integer.parseInt(cut) - delta;
						subword = subword.substring(0, end) + addBefore;
					}
				}

				result.add(subword);
				startIndex = endIndex;
			}

		String subword = word.substring(startIndex);
		if(StringUtils.isNotBlank(addAfter))
			subword = addAfter + subword.substring(Math.min(Math.max(after, 0), subword.length()));
		result.add(subword);

		return result;
	}

	public static int getAugmentedRuleAddLength(String rule){
		int length = 0;
		Matcher m = HyphenationParser.PATTERN_AUGMENTED_RULE.matcher(rule);
		if(m.find()){
			String addBefore = m.group(HyphenationParser.PARAM_ADD_BEFORE);
			String basicRule = m.group(HyphenationParser.PARAM_RULE);
			String start = m.group(HyphenationParser.PARAM_START);
			if(start == null)
				start = Integer.toString(1);
			length = addBefore.length() - Integer.parseInt(start) + breakpointIndex(basicRule) - 1;
		}
		return length;
	}

	private static int breakpointIndex(String rule){
		return StringUtils.indexOfAny(rule, '1', '3', '5', '7', '9');
	}


	//FIXME method firm is awful
	protected Map<Integer, Pair<Integer, String>> extractSyllabe(String rule, int startingIndex, String word, int normalizedWordSize,
			HyphenationOptions options, Map<Integer, Pair<Integer, String>> indexesAndRules){
		int leftMin = options.getLeftMin();
		int rightMin = options.getRightMin();

		int wordSize = word.length();
		String reducedRule = removeNonStandardPart(rule);

		//cycle the pattern's characters searching for numbers
		int j = -1;
		for(char chr : reducedRule.toCharArray()){
			if(!Character.isDigit(chr))
				j ++;
			else{
				//check if a break point should be skipped based on left and right min options
				int idx = startingIndex + j;
				int normalizedIdx = (normalizedWordSize != wordSize? getNormalizedLength(word, idx): idx);
				if(leftMin <= normalizedIdx && normalizedIdx <= normalizedWordSize - rightMin){
					int dd = Character.digit(chr, 10);
					//check if the break number is great than the one stored so far
					if(dd > indexesAndRules.getOrDefault(idx, HyphenationBreak.EMPTY_PAIR).getKey())
						indexesAndRules.put(idx, Pair.of(dd, rule));
				}
			}
		}
		return indexesAndRules;
	}

	private String removeNonStandardPart(String rule){
		return PatternHelper.clear(rule, HyphenationParser.PATTERN_REDUCE);
	}

	protected int getNormalizedLength(String word){
		return Normalizer.normalize(word, Normalizer.Form.NFKC).length();
	}

	private static int getNormalizedLength(String word, int index){
		return Normalizer.normalize(word.substring(0, index - 1), Normalizer.Form.NFKC).length() + 1;
	}

}
