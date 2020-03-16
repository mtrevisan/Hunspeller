package unit731.hunlinter.services.text;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.services.PatternHelper;


public class StringHelper{

	private static final Pattern PATTERN_COMBINING_DIACRITICAL_MARKS = PatternHelper.pattern("\\p{InCombiningDiacriticalMarks}+");

	public enum Casing{
		/** All lower case or neutral case, e.g. "hello java" */
		LOWER_CASE,
		/** Start upper case, rest lower case, e.g. "Hello java" */
		TITLE_CASE,
		/** All upper case, e.g. "UPPERCASE" or "HELLO JAVA" */
		ALL_CAPS,
		/** Camel case, start lower case, e.g. "helloJava" */
		CAMEL_CASE,
		/** Pascal case, start upper case, e.g. "HelloJava" */
		PASCAL_CASE
	}


	private StringHelper(){}

	public static long countUppercases(final String text){
		return text.chars()
			.filter(Character::isUpperCase)
			.count();
	}

	//Classify the casing of a given string (ignoring characters for which no upper-/lowercase distinction exists)
	public static Casing classifyCasing(final String text){
		if(StringUtils.isBlank(text))
			return Casing.LOWER_CASE;

		final long upper = text.chars()
			.filter(chr -> Character.isLetter(chr) && Character.isUpperCase(chr))
			.count();
		if(upper == 0l)
			return Casing.LOWER_CASE;

		final boolean startsWithUppercase = Character.isUpperCase(text.charAt(0));
		if(startsWithUppercase && upper == 1l)
			return Casing.TITLE_CASE;

		final long lower = text.chars()
			//Unicode modifier letter apostrophe is considered as an uppercase letter, but shoule be regarded as caseless,
			//so it has to be excluded
			.filter(chr -> Character.isLetter(chr) && chr != HyphenationParser.RIGHT_MODIFIER_LETTER_APOSTROPHE
				&& Character.isLowerCase(chr))
			.count();
		if(lower == 0l)
			return Casing.ALL_CAPS;

		return (startsWithUppercase? Casing.PASCAL_CASE: Casing.CAMEL_CASE);
	}

	public static String longestCommonPrefix(final Collection<String> texts){
		return longestCommonAffix(texts, StringHelper::commonPrefix);
	}

	public static String longestCommonSuffix(final Collection<String> texts){
		return longestCommonAffix(texts, StringHelper::commonSuffix);
	}

	private static String longestCommonAffix(final Collection<String> texts,
			final BiFunction<String, String, String> commonAffix){
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
	private static String commonSuffix(final String a, final String b){
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
	private static String commonPrefix(final String a, final String b){
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
	private static boolean validSurrogatePairAt(final CharSequence string, final int index){
		return (index >= 0 && index <= (string.length() - 2)
			&& Character.isHighSurrogate(string.charAt(index))
			&& Character.isLowSurrogate(string.charAt(index + 1)));
	}

	public static int getLastCommonLetterIndex(final String word1, final String word2){
		int lastCommonLetter;
		final int minWordLength = Math.min(word1.length(), word2.length());
		for(lastCommonLetter = 0; lastCommonLetter < minWordLength; lastCommonLetter ++)
			if(word1.charAt(lastCommonLetter) != word2.charAt(lastCommonLetter))
				break;
		return lastCommonLetter;
	}

	public static String removeCombiningDiacriticalMarks(final String word){
		return PatternHelper.replaceAll(Normalizer.normalize(word, Normalizer.Form.NFKD), PATTERN_COMBINING_DIACRITICAL_MARKS,
			StringUtils.EMPTY);
	}

	public static Collector<String, List<String>, String> limitingJoin(final String delimiter, final int limit,
			final String ellipsis){
		return Collector.of(ArrayList::new,
			(l, e) -> {
				if(l.size() < limit)
					l.add(e);
				else if(l.size() == limit)
					l.add(ellipsis);
			},
			(l1, l2) -> {
				l1.addAll(l2.subList(0, Math.min(l2.size(), Math.max(0, limit - l1.size()))));
				if(l1.size() == limit)
					l1.add(ellipsis);
				return l1;
			},
			l -> String.join(delimiter, l)
		);
	}


	public static byte[] getRawBytes(final String text){
		return text.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Converts an array of bytes into a string representing the hexadecimal values of each byte in order
	 *
	 * @param byteArray	Array to be converted to hexadecimal characters
	 * @return	The hexadecimal characters
	 */
	public static String byteArrayToHexString(final byte[] byteArray){
		final StringBuffer sb = new StringBuffer(byteArray.length << 1);
		for(final byte b : byteArray){
			sb.append(Character.forDigit((b >> 4) & 0x0F, 16));
			sb.append(Character.forDigit((b & 0x0F), 16));
		}
		return sb.toString();
	}

	//NOTE: `bytes` should be non-negative (no check is done)
	public static String byteCountToHumanReadable(final long bytes){
		if(bytes < 1024l)
			return bytes + " B";

		final int exponent = (int)(Math.log10((double)bytes) / Math.log10(1024.));
		final char prefix = "KMGTPE".charAt(exponent - 1);
		final double divisor = Math.pow(1024., exponent);
		final double result = bytes / divisor;
		return String.format(Locale.ROOT, (result < 100? "%.1f": "%.0f") + " %ciB", result, prefix);
	}

	//Find the maximum consecutive repeating character in given string
	public static int maxRepeating(final String text, final char chr){
		final int n = text.length();
		int count = 0;
		int currentCount = 1;
		//traverse string except last character
		for(int i = 0; i < n; i ++){
			//if current character matches with next
			if(i < n - 1 && text.charAt(i) == chr && text.charAt(i + 1) == chr)
				currentCount ++;
			//if doesn't match, update result (if required) and reset count
			else{
				if(currentCount > count)
					count = currentCount;
				currentCount = 1;
			}
		}
		return count;
	}

	public static String removeAll(final String text, final char charToRemove){
		final StringBuffer sb = new StringBuffer(text);
		for(int i = 0; i < text.length(); i ++)
			if(sb.charAt(i) == charToRemove)
				sb.deleteCharAt(i);
		return sb.toString();
	}

}
