package unit731.hunlinter.workers;

import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import java.awt.Frame;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import unit731.hunlinter.DictionaryStatisticsDialog;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.dictionary.DictionaryStatistics;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;


public class StatisticsWorker extends WorkerDictionary{

	public static final String WORKER_NAME = "Statistics";

	private final DictionaryStatistics dicStatistics;
	private final HyphenatorInterface hyphenator;
	private final Orthography orthography;


	public StatisticsWorker(final AffixParser affParser, final DictionaryParser dicParser, final HyphenatorInterface hyphenator,
			final WordGenerator wordGenerator, final Frame parent){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(affParser);
		Objects.requireNonNull(wordGenerator);


		final AffixData affixData = affParser.getAffixData();
		final String language = affixData.getLanguage();
		dicStatistics = new DictionaryStatistics(language, affixData.getCharset());
		this.hyphenator = hyphenator;
		orthography = BaseBuilder.getOrthography(language);

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(indexData.getData(), affixData);
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

			for(final Inflection inflection : inflections){
				//collect statistics
				final String word = inflection.getWord();
				final String[] subwords = (hyphenator != null? hyphenator.splitIntoCompounds(word): new String[0]);
				if(subwords.length == 0)
					dicStatistics.addData(word);
				else
					for(final String subword : subwords){
						final Hyphenation hyph = hyphenator.hyphenate(orthography.markDefaultStress(subword));
						dicStatistics.addData(word, hyph);
					}
			}
		};
		final Consumer<Exception> cancelled = exception -> dicStatistics.close();

		getWorkerData()
			.withDataCancelledCallback(cancelled);

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final Path dicPath = dicParser.getDicFile().toPath();
			final Charset charset = dicParser.getCharset();
			processLines(dicPath, charset, lineProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		final Function<Void, Void> step2 = ignored -> {
			dicStatistics.close();

			//show statistics window
			final DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(dicStatistics, parent);
			GUIUtils.addCancelByEscapeKey(dialog);
			dialog.setLocationRelativeTo(parent);
			dialog.setVisible(true);

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

	public boolean isPerformingHyphenationStatistics(){
		return (hyphenator != null);
	}

}
