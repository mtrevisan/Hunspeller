/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.languages;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;

import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.IntStream;


public class Orthography{

	private static final String WRONG_APOSTROPHES = HyphenationParser.APOSTROPHE + "‘";
	private static final String CORRECT_APOSTROPHES = StringUtils.repeat(HyphenationParser.MODIFIER_LETTER_APOSTROPHE, WRONG_APOSTROPHES.length());

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new Orthography();
	}


	protected Orthography(){}

	public static Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	public String correctOrthography(final String word){
		return correctApostrophes(word);
	}

	protected String correctApostrophes(final String word){
		return StringUtils.replaceChars(word, WRONG_APOSTROPHES, CORRECT_APOSTROPHES);
	}

	public boolean[] getSyllabationErrors(final String[] syllabes){
		return new boolean[syllabes.length];
	}

	public boolean hasSyllabationErrors(final String[] syllabes){
		final boolean[] errors = getSyllabationErrors(syllabes);
		return IntStream.range(0, errors.length)
			.mapToObj(idx -> errors[idx])
			.anyMatch(error -> error);
	}

	public StringJoiner formatHyphenation(final String[] syllabes){
		final StringJoiner sj = new StringJoiner(HyphenationParser.SOFT_HYPHEN);
		return formatHyphenation(syllabes, sj, Function.identity());
	}

	public StringJoiner formatHyphenation(final String[] syllabes, final StringJoiner sj, final Function<String, String> errorFormatter){
		final boolean[] errors = getSyllabationErrors(syllabes);
		for(int i = 0; i < syllabes.length; i ++){
			final Function<String, String> fun = (errors[i]? errorFormatter: Function.identity());
			sj.add(fun.apply(syllabes[i]));
		}
		return sj;
	}

	/**
	 * @param syllabes	The list of syllabes
	 * @return The 0-based index of the syllabe starting from the end
	 */
	public int getStressedSyllabeIndexFromLast(final String[] syllabes){
		return -1;
	}

	public int countGraphemes(final String word){
		return word.length();
	}

	public String markDefaultStress(final String word){
		return word;
	}

	public boolean hasStressedGrapheme(final String word){
		return false;
	}

}
