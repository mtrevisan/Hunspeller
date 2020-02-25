package unit731.hunlinter.workers;

import java.text.MessageFormat;

import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import unit731.hunlinter.languages.RulesLoader;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;
import unit731.hunlinter.workers.exceptions.LinterException;


public class HyphenationLinterWorker extends WorkerDictionary{

	public static final String WORKER_NAME = "Hyphenation linter";

	private static final String SLASH = "/";
	private static final String ASTERISK = "*";

	private static final String POS_NUMERAL_LATIN = "numeral_latin";
	private static final String POS_UNIT_OF_MEASURE = "unit_of_measure";

	private static final MessageFormat WORD_IS_NOT_SYLLABABLE = new MessageFormat("Word {0} ({1}) is not syllabable");


	public HyphenationLinterWorker(final String language, final DictionaryParser dicParser, final HyphenatorInterface hyphenator,
			final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true)
			.withRelaunchException(false));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(hyphenator);


		final Orthography orthography = BaseBuilder.getOrthography(language);
		final RulesLoader rulesLoader = new RulesLoader(language, null);

		final BiConsumer<Integer, String> lineProcessor = (row, line) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			productions.forEach(production -> {
				final String word = production.getWord();
				if(word.length() > 1 && !production.hasPartOfSpeech(POS_NUMERAL_LATIN) && !production.hasPartOfSpeech(POS_UNIT_OF_MEASURE)
						&& !rulesLoader.containsUnsyllabableWords(word)){
					final Hyphenation hyphenation = hyphenator.hyphenate(word);
					final List<String> syllabes = hyphenation.getSyllabes();
					if(orthography.hasSyllabationErrors(syllabes)){
						final String message = WORD_IS_NOT_SYLLABABLE.format(new Object[]{word,
							orthography.formatHyphenation(syllabes, new StringJoiner(SLASH), syllabe -> ASTERISK + syllabe + ASTERISK), row});
						final StringBuffer sb = new StringBuffer(message);
						if(production.hasProductionRules())
							sb.append(" (via ").append(production.getRulesSequence()).append(")");
						throw new LinterException(sb.toString());
					}
				}
			});
		};

		setReadDataProcessor(lineProcessor);
	}

}