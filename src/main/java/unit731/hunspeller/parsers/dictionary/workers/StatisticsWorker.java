package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.awt.Frame;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.DictionaryStatisticsDialog;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;


public class StatisticsWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Statistics";

	private final boolean performHyphenationStatistics;
	private final DictionaryStatistics dicStatistics;


	public StatisticsWorker(AffixParser affParser, DictionaryParser dicParser, HyphenatorInterface hyphenator, WordGenerator wordGenerator,
			boolean performHyphenationStatistics, Frame parent){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(hyphenator);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(parent);

		this.performHyphenationStatistics = performHyphenationStatistics;

		AffixData affixData = affParser.getAffixData();
		dicStatistics = new DictionaryStatistics(affixData.getLanguage(), affixData.getCharset());


		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			for(Production production : productions){
				//collect statistics
				String word = production.getWord();
				if(performHyphenationStatistics){
					List<String> subwords = hyphenator.splitIntoCompounds(word);
					if(subwords.isEmpty())
						dicStatistics.addData(word);
					else
						for(String subword : subwords){
							Hyphenation hyph = hyphenator.hyphenate(dicStatistics.getOrthography().markDefaultStress(subword));
							dicStatistics.addData(word, hyph);
						}
				}
				else
					dicStatistics.addData(word);
			}
		};
		Runnable completed = () -> {
			try{
				dicStatistics.close();
			}
			catch(IOException e){}

			//show statistics window
			DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(dicStatistics, parent);
			dialog.setLocationRelativeTo(parent);
			dialog.setVisible(true);
		};
		Runnable cancelled = () -> {
			try{
				dicStatistics.close();
			}
			catch(IOException e){}
		};
		WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser, completed, cancelled);
		createReadWorker(data, lineProcessor);
	}

	public boolean isPerformHyphenationStatistics(){
		return performHyphenationStatistics;
	}

	@Override
	public void clear(){
		dicStatistics.clear();
	}

}
