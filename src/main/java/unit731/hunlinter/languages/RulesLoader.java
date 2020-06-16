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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
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
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.datastructures.SetHelper;


public class RulesLoader{

	private static final MessageFormat WORD_WITH_RULE_CANNOT_HAVE = new MessageFormat("Word with rule {0} cannot have rule {1}");
	private static final MessageFormat WORD_WITH_LETTER_CANNOT_HAVE = new MessageFormat("Word with letter ''{0}'' cannot have rule {1}");
	private static final MessageFormat WORD_WITH_LETTER_CANNOT_HAVE_USE = new MessageFormat("Word with letter ''{0}'' cannot have rule {1}, use {2}");


	private final Properties rulesProperties;

	private final boolean morphologicalFieldsCheck;
	private final boolean enableVerbSyllabationCheck;
	private final boolean wordCanHaveMultipleStresses;
	private final Map<MorphologicalTag, Set<String>> dataFields = new EnumMap<>(MorphologicalTag.class);
	private final Set<String> unsyllabableWords;
	private final Set<String> multipleStressedWords;
	private final Set<String> hasToContainStress = new HashSet<>();
	private final Set<String> cannotContainStress = new HashSet<>();
	private final Map<String, Set<LetterMatcherEntry>> letterAndRulesNotCombinable = new HashMap<>();
	private final Map<String, Set<RuleMatcherEntry>> ruleAndRulesNotCombinable = new HashMap<>();


	public RulesLoader(final String language, final FlagParsingStrategy strategy){
		Objects.requireNonNull(language);

		rulesProperties = BaseBuilder.getRulesProperties(language);

		morphologicalFieldsCheck = Boolean.parseBoolean((String)rulesProperties.get("morphologicalFieldsCheck"));
		enableVerbSyllabationCheck = Boolean.parseBoolean((String)rulesProperties.get("verbSyllabationCheck"));
		wordCanHaveMultipleStresses = Boolean.parseBoolean((String)rulesProperties.get("wordCanHaveMultipleStresses"));

		fillDataFields(MorphologicalTag.PART_OF_SPEECH, "partOfSpeeches");
		fillDataFields(MorphologicalTag.DERIVATIONAL_SUFFIX, "derivationalSuffixes");
		fillDataFields(MorphologicalTag.INFLECTIONAL_SUFFIX, "inflectionalSuffixes");
		fillDataFields(MorphologicalTag.TERMINAL_SUFFIX, "terminalSuffixes");
		dataFields.put(MorphologicalTag.STEM, null);
		dataFields.put(MorphologicalTag.ALLOMORPH, null);

		unsyllabableWords = readPropertyAsSet("unsyllabableWords", ',');
		multipleStressedWords = readPropertyAsSet("multipleStressedWords", ',');

		if(strategy != null){
			String[] flags = strategy.parseFlags(readProperty("hasToContainStress"));
			if(flags != null)
				hasToContainStress.addAll(Arrays.asList(flags));
			flags = strategy.parseFlags(readProperty("cannotContainStress"));
			if(flags != null)
				cannotContainStress.addAll(Arrays.asList(flags));

			Iterator<String> rules = readPropertyAsIterator("notCombinableRules", '/');
			while(rules.hasNext()){
				final String masterFlag = rules.next();
				final String[] wrongFlags = strategy.parseFlags(rules.next());
				ruleAndRulesNotCombinable.computeIfAbsent(masterFlag, k -> new HashSet<>(1))
					.add(new RuleMatcherEntry(WORD_WITH_RULE_CANNOT_HAVE, masterFlag, wrongFlags));
			}

			String letter = null;
			rules = readPropertyAsIterator("letterAndRulesNotCombinable", '/');
			while(rules.hasNext()){
				final String elem = rules.next();
				if(elem.length() == 1)
					letter = String.valueOf(elem.charAt(0));
				else{
					flags = strategy.parseFlags(elem);
					final String correctRule = flags[flags.length - 1];
					final String[] wrongFlags = ArrayUtils.remove(flags, flags.length - 1);
					letterAndRulesNotCombinable.computeIfAbsent(letter, k -> new HashSet<>(1))
						.add(new LetterMatcherEntry((StringUtils.isNotBlank(correctRule)? WORD_WITH_LETTER_CANNOT_HAVE_USE: WORD_WITH_LETTER_CANNOT_HAVE),
							letter, wrongFlags, correctRule));
				}
			}
		}
	}

	private void fillDataFields(final MorphologicalTag tag, final String property){
		final Iterator<String> itr = readPropertyAsIterator(property, ',');
		final Set<String> set = new HashSet<>();
		while(itr.hasNext())
			set.add(tag.getCode() + itr.next());
		dataFields.put(tag, set);
	}

	public final String readProperty(final String key){
		return rulesProperties.getProperty(key, StringUtils.EMPTY);
	}

	public final Set<String> readPropertyAsSet(final String key, final char separator){
		final String line = readProperty(key);
		return (StringUtils.isNotEmpty(line)? SetHelper.setOf(StringUtils.split(line, separator)): Collections.emptySet());
	}

	public final Iterator<String> readPropertyAsIterator(final String key, final char separator){
		final List<String> list = new ArrayList<>(rulesProperties.size());
		for(final Object o : rulesProperties.keySet()){
			final String k = (String)o;
			if(k.equals(key) || k.startsWith(key) && StringUtils.isNumeric(k.substring(key.length()))){
				final String line = readProperty(k);
				list.addAll(Arrays.asList(StringUtils.split(line, separator)));
			}
		}
		return list.iterator();
	}


	public boolean isMorphologicalFieldsCheck(){
		return morphologicalFieldsCheck;
	}

	public boolean isEnableVerbSyllabationCheck(){
		return enableVerbSyllabationCheck;
	}

	public boolean isWordCanHaveMultipleStresses(){
		return wordCanHaveMultipleStresses;
	}

	public boolean containsDataField(final MorphologicalTag key){
		return dataFields.containsKey(key);
	}

	public Set<String> getDataField(final MorphologicalTag key){
		return dataFields.get(key);
	}

	public boolean containsUnsyllabableWords(final String word){
		return unsyllabableWords.contains(word);
	}

	public boolean containsMultipleStressedWords(final String word){
		return multipleStressedWords.contains(word);
	}

	public boolean containsHasToContainStress(final String word){
		return hasToContainStress.contains(word);
	}

	public boolean containsCannotContainStress(final String word){
		return cannotContainStress.contains(word);
	}

	public void letterToFlagIncompatibilityCheck(final Inflection inflection){
		for(final Map.Entry<String, Set<LetterMatcherEntry>> entry : letterAndRulesNotCombinable.entrySet())
			if(StringUtils.containsAny(inflection.getWord(), entry.getKey()))
				for(final LetterMatcherEntry letterMatcherEntry : entry.getValue())
					letterMatcherEntry.match(inflection);
	}

	public void flagToFlagIncompatibilityCheck(final Inflection inflection){
		for(final Map.Entry<String, Set<RuleMatcherEntry>> check : ruleAndRulesNotCombinable.entrySet())
			if(inflection.hasContinuationFlag(check.getKey()))
				for(final RuleMatcherEntry entry : check.getValue())
					entry.match(inflection);
	}

}
