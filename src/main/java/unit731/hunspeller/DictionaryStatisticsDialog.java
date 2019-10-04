package unit731.hunspeller;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.lang3.StringUtils;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.CategorySeries;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.gui.GUIUtils;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.Frequency;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.hyphenation.Hyphenation;


public class DictionaryStatisticsDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryStatisticsDialog.class);

	private static final long serialVersionUID = 5762751368059394067l;

	private static final String SERIES_NAME = "series";
	private static final String LIST_SEPARATOR = ", ";
	private static final String TAB = "\t";

	private final DictionaryStatistics statistics;

	private final JFileChooser saveTextFileFileChooser;


	public DictionaryStatisticsDialog(DictionaryStatistics statistics, Frame parent){
		super(parent, "Dictionary statistics", false);

		Objects.requireNonNull(statistics);
		Objects.requireNonNull(parent);

		this.statistics = statistics;

		initComponents();

		try{
			JPopupMenu copyingPopupMenu = GUIUtils.createCopyingPopupMenu(compoundWordsOutputLabel.getHeight());
			GUIUtils.addPopupMenu(copyingPopupMenu, compoundWordsOutputLabel, contractedWordsOutputLabel, lengthsModeOutputLabel, longestWordCharactersOutputLabel,
				longestWordSyllabesOutputLabel, mostCommonSyllabesOutputLabel, syllabeLengthsModeOutputLabel, totalWordsOutputLabel, uniqueWordsOutputLabel);
		}
		catch(IOException ignored){}

		addListenerOnClose();

		saveTextFileFileChooser = new JFileChooser();
		saveTextFileFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		File currentDir = new File(".");
		saveTextFileFileChooser.setCurrentDirectory(currentDir);


		fillStatisticData();
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      totalWordsLabel = new javax.swing.JLabel();
      totalWordsOutputLabel = new javax.swing.JLabel();
      uniqueWordsLabel = new javax.swing.JLabel();
      uniqueWordsOutputLabel = new javax.swing.JLabel();
      compoundWordsLabel = new javax.swing.JLabel();
      compoundWordsOutputLabel = new javax.swing.JLabel();
      contractedWordsLabel = new javax.swing.JLabel();
      contractedWordsOutputLabel = new javax.swing.JLabel();
      lengthsModeLabel = new javax.swing.JLabel();
      lengthsModeOutputLabel = new javax.swing.JLabel();
      syllabeLengthsModeLabel = new javax.swing.JLabel();
      syllabeLengthsModeOutputLabel = new javax.swing.JLabel();
      mostCommonSyllabesLabel = new javax.swing.JLabel();
      mostCommonSyllabesOutputLabel = new javax.swing.JLabel();
      longestWordCharactersLabel = new javax.swing.JLabel();
      longestWordCharactersOutputLabel = new javax.swing.JLabel();
      longestWordSyllabesLabel = new javax.swing.JLabel();
      longestWordSyllabesOutputLabel = new javax.swing.JLabel();
      mainTabbedPane = new javax.swing.JTabbedPane();
      lengthsPanel = createChartPanel("Word length distribution", "Word length", "Frequency");
      syllabesPanel = createChartPanel("Word syllabe distribution", "Word syllabe", "Frequency");
      stressesPanel = createChartPanel("Word stress distribution", "Word stressed syllabe index (from last)", "Frequency");
      exportButton = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      totalWordsLabel.setLabelFor(totalWordsOutputLabel);
      totalWordsLabel.setText("Total words:");

      totalWordsOutputLabel.setText("...");

      uniqueWordsLabel.setLabelFor(uniqueWordsOutputLabel);
      uniqueWordsLabel.setText("Unique words:");

      uniqueWordsOutputLabel.setText("...");

      compoundWordsLabel.setText("Compound words:");

      compoundWordsOutputLabel.setText("...");

      contractedWordsLabel.setText("Contracted words:");

      contractedWordsOutputLabel.setText("...");

      lengthsModeLabel.setLabelFor(lengthsModeOutputLabel);
      lengthsModeLabel.setText("Mode of wordsʼ length:");

      lengthsModeOutputLabel.setText("...");

      syllabeLengthsModeLabel.setLabelFor(syllabeLengthsModeOutputLabel);
      syllabeLengthsModeLabel.setText("Mode of wordsʼ syllabe:");

      syllabeLengthsModeOutputLabel.setText("...");

      mostCommonSyllabesLabel.setLabelFor(mostCommonSyllabesOutputLabel);
      mostCommonSyllabesLabel.setText("Most common syllabes:");

      mostCommonSyllabesOutputLabel.setText("...");

      longestWordCharactersLabel.setLabelFor(longestWordCharactersOutputLabel);
      longestWordCharactersLabel.setText("Longest word(s) (by characters):");

      longestWordCharactersOutputLabel.setText("...");

      longestWordSyllabesLabel.setLabelFor(longestWordSyllabesOutputLabel);
      longestWordSyllabesLabel.setText("Longest word(s) (by syllabes):");

      longestWordSyllabesOutputLabel.setText("...");

      javax.swing.GroupLayout lengthsPanelLayout = new javax.swing.GroupLayout(lengthsPanel);
      lengthsPanel.setLayout(lengthsPanelLayout);
      lengthsPanelLayout.setHorizontalGroup(
         lengthsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 655, Short.MAX_VALUE)
      );
      lengthsPanelLayout.setVerticalGroup(
         lengthsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 301, Short.MAX_VALUE)
      );

      mainTabbedPane.addTab("Word lengths", lengthsPanel);

      javax.swing.GroupLayout syllabesPanelLayout = new javax.swing.GroupLayout(syllabesPanel);
      syllabesPanel.setLayout(syllabesPanelLayout);
      syllabesPanelLayout.setHorizontalGroup(
         syllabesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 655, Short.MAX_VALUE)
      );
      syllabesPanelLayout.setVerticalGroup(
         syllabesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 301, Short.MAX_VALUE)
      );

      mainTabbedPane.addTab("Word syllabes", syllabesPanel);

      javax.swing.GroupLayout stressesPanelLayout = new javax.swing.GroupLayout(stressesPanel);
      stressesPanel.setLayout(stressesPanelLayout);
      stressesPanelLayout.setHorizontalGroup(
         stressesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 655, Short.MAX_VALUE)
      );
      stressesPanelLayout.setVerticalGroup(
         stressesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 301, Short.MAX_VALUE)
      );

      mainTabbedPane.addTab("Word stresses", stressesPanel);

      exportButton.setText("Export");
      exportButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            exportButtonActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(totalWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(totalWordsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addComponent(mainTabbedPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lengthsModeLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(lengthsModeOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(syllabeLengthsModeLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(syllabeLengthsModeOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(mostCommonSyllabesLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(mostCommonSyllabesOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(longestWordSyllabesLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(longestWordSyllabesOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(uniqueWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(uniqueWordsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(compoundWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(compoundWordsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(longestWordCharactersLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(longestWordCharactersOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(contractedWordsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(contractedWordsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGap(0, 0, Short.MAX_VALUE)
                  .addComponent(exportButton)
                  .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(totalWordsLabel)
               .addComponent(totalWordsOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(uniqueWordsLabel)
               .addComponent(uniqueWordsOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(compoundWordsLabel)
               .addComponent(compoundWordsOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(contractedWordsLabel)
               .addComponent(contractedWordsOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(lengthsModeLabel)
               .addComponent(lengthsModeOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(syllabeLengthsModeLabel)
               .addComponent(syllabeLengthsModeOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(mostCommonSyllabesLabel)
               .addComponent(mostCommonSyllabesOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(longestWordCharactersLabel)
               .addComponent(longestWordCharactersOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(longestWordSyllabesLabel)
               .addComponent(longestWordSyllabesOutputLabel))
            .addGap(18, 18, 18)
            .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 329, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, Short.MAX_VALUE)
            .addComponent(exportButton)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	public void setCurrentFont(final Font font){
		mostCommonSyllabesOutputLabel.setFont(font);
		longestWordCharactersOutputLabel.setFont(font);
		longestWordSyllabesOutputLabel.setFont(font);
	}

   private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
		int fileChosen = saveTextFileFileChooser.showSaveDialog(this);
		if(fileChosen == JFileChooser.APPROVE_OPTION){
			exportButton.setEnabled(false);

			try{
				File outputFile = saveTextFileFileChooser.getSelectedFile();
				exportToFile(outputFile);
			}
			catch(Exception e){
				LOGGER.error("Cannot export statistics", e);
			}

			exportButton.setEnabled(true);
		}
   }//GEN-LAST:event_exportButtonActionPerformed

	private void addListenerOnClose(){
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosed(WindowEvent e){
				statistics.clear();
			}
		});
	}

	private void fillStatisticData(){
		long totalWords = statistics.getTotalProductions();
		if(totalWords > 0){
			fillBaseStatistics();
			if(statistics.hasSyllabeStatistics())
				fillSyllabeStatistics();
			else
				cleanupSyllabeStatistics();

			fillLengthsFrequencies(statistics.getLengthsFrequencies(), totalWords, lengthsPanel);

			fillLengthsFrequencies(statistics.getSyllabeLengthsFrequencies(), totalWords, syllabesPanel);

			fillLengthsFrequencies(statistics.getStressFromLastFrequencies(), totalWords, stressesPanel);
		}
	}

	private void fillBaseStatistics(){
		long totalWords = statistics.getTotalProductions();
		int uniqueWords = statistics.getUniqueWords();
		int contractedWords = statistics.getContractedWords();
		Frequency<Integer> lengthsFrequencies = statistics.getLengthsFrequencies();
		int longestWordCharsCount = statistics.getLongestWordCountByCharacters();
		List<String> longestWords = statistics.getLongestWordsByCharacters();
		longestWords = DictionaryStatistics.extractRepresentatives(longestWords, 4);

		String formattedTotalWords = DictionaryParser.COUNTER_FORMATTER.format(totalWords);
		String formattedUniqueWords = DictionaryParser.COUNTER_FORMATTER.format(uniqueWords)
			+ " (" + DictionaryParser.PERCENT_FORMATTER_1.format((double)uniqueWords / totalWords) + ")";
		String formattedContractedWords = DictionaryParser.COUNTER_FORMATTER.format(contractedWords)
			+ " (" + DictionaryParser.PERCENT_FORMATTER_1.format((double)contractedWords / uniqueWords) + ")";
		String formattedLengthsMode = lengthsFrequencies.getMode().stream().map(String::valueOf).collect(Collectors.joining(LIST_SEPARATOR));
		String formattedLongestWords = String.join(LIST_SEPARATOR, longestWords)
			+ " (" + longestWordCharsCount + ")";

		totalWordsOutputLabel.setText(formattedTotalWords);
		uniqueWordsOutputLabel.setText(formattedUniqueWords);
		contractedWordsOutputLabel.setText(formattedContractedWords);
		lengthsModeOutputLabel.setText(formattedLengthsMode);
		longestWordCharactersOutputLabel.setText(formattedLongestWords);
	}

	private void fillSyllabeStatistics(){
		int compoundWords = statistics.getCompoundWords();
		int uniqueWords = statistics.getUniqueWords();
		Frequency<Integer> syllabeLengthsFrequencies = statistics.getSyllabeLengthsFrequencies();
		List<String> mostCommonSyllabes = statistics.getMostCommonSyllabes(7);
		List<String> longestWordSyllabes = statistics.getLongestWordsBySyllabes().stream()
			.map(Hyphenation::getSyllabes)
			.map(syllabes -> StringUtils.join(syllabes, HyphenationParser.SOFT_HYPHEN))
			.collect(Collectors.toList());
		longestWordSyllabes = DictionaryStatistics.extractRepresentatives(longestWordSyllabes, 4);
		int longestWordSyllabesCount = statistics.getLongestWordCountBySyllabes();

		String formattedCompoundWords = DictionaryParser.COUNTER_FORMATTER.format(compoundWords)
			+ " (" + DictionaryParser.PERCENT_FORMATTER_1.format((double)compoundWords / uniqueWords) + ")";
		String formattedSyllabeLengthsMode = syllabeLengthsFrequencies.getMode().stream().map(String::valueOf).collect(Collectors.joining(LIST_SEPARATOR));
		String formattedMostCommonSyllabes = String.join(LIST_SEPARATOR, mostCommonSyllabes);
		String formattedLongestWordSyllabes = String.join(LIST_SEPARATOR, longestWordSyllabes)
			+ " (" + longestWordSyllabesCount + ")";

		compoundWordsOutputLabel.setText(formattedCompoundWords);
		syllabeLengthsModeOutputLabel.setText(formattedSyllabeLengthsMode);
		mostCommonSyllabesOutputLabel.setText(formattedMostCommonSyllabes);
		longestWordSyllabesOutputLabel.setText(formattedLongestWordSyllabes);

		compoundWordsLabel.setEnabled(true);
		compoundWordsOutputLabel.setEnabled(true);
		syllabeLengthsModeLabel.setEnabled(true);
		syllabeLengthsModeOutputLabel.setEnabled(true);
		mostCommonSyllabesLabel.setEnabled(true);
		mostCommonSyllabesOutputLabel.setEnabled(true);
		longestWordSyllabesLabel.setEnabled(true);
		longestWordSyllabesOutputLabel.setEnabled(true);
	}

	private void cleanupSyllabeStatistics(){
		compoundWordsOutputLabel.setText(StringUtils.EMPTY);
		syllabeLengthsModeOutputLabel.setText(StringUtils.EMPTY);
		mostCommonSyllabesOutputLabel.setText(StringUtils.EMPTY);
		longestWordSyllabesOutputLabel.setText(StringUtils.EMPTY);

		compoundWordsLabel.setEnabled(false);
		compoundWordsOutputLabel.setEnabled(false);
		syllabeLengthsModeLabel.setEnabled(false);
		syllabeLengthsModeOutputLabel.setEnabled(false);
		mostCommonSyllabesLabel.setEnabled(false);
		mostCommonSyllabesOutputLabel.setEnabled(false);
		longestWordSyllabesLabel.setEnabled(false);
		longestWordSyllabesOutputLabel.setEnabled(false);
	}

	private void fillLengthsFrequencies(Frequency<Integer> frequencies, long totalSamples, JPanel panel){
		boolean hasData = frequencies.entrySetIterator().hasNext();

		mainTabbedPane.setEnabledAt(mainTabbedPane.indexOfComponent(panel), hasData);
		if(hasData){
			CategoryChart chart = (CategoryChart)((XChartPanel<?>)panel).getChart();
			addSeriesToChart(chart, frequencies, totalSamples);
		}
	}

	private JPanel createChartPanel(String title, String xAxisTitle, String yAxisTitle){
		CategoryChart chart = new CategoryChartBuilder()
			.title(title)
			.xAxisTitle(xAxisTitle)
			.yAxisTitle(yAxisTitle)
			.theme(Styler.ChartTheme.Matlab)
			.build();

		CategoryStyler styler = chart.getStyler();
		styler.setAvailableSpaceFill(0.98);
		styler.setOverlapped(true);
		styler.setLegendVisible(false);
		styler.setXAxisMin(0.);
		styler.setYAxisMin(0.);
		styler.setYAxisDecimalPattern("#%");
		styler.setYAxisTitleVisible(false);
		styler.setChartBackgroundColor(getBackground());
		styler.setToolTipsEnabled(true);

		return new XChartPanel<>(chart);
	}

	private void addSeriesToChart(CategoryChart chart, Frequency<Integer> freqs, long totalCount){
		List<Integer> xData = new ArrayList<>();
		List<Double> yData = new ArrayList<>();
		Iterator<Map.Entry<Integer, Long>> itr = freqs.entrySetIterator();
		while(itr.hasNext()){
			Map.Entry<Integer, Long> elem = itr.next();
			xData.add(elem.getKey());
			yData.add(elem.getValue().doubleValue() / totalCount);
		}

		chart.addSeries(SERIES_NAME, xData, yData);
	}

	private void exportToFile(File outputFile) throws IOException{
		try(BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)){
			boolean hasSyllabeStatistics = syllabeLengthsModeLabel.isEnabled();

			writer.write(totalWordsLabel.getText() + TAB + StringUtils.replaceChars(totalWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(uniqueWordsLabel.getText() + TAB + StringUtils.replaceChars(uniqueWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(compoundWordsLabel.getText() + TAB + StringUtils.replaceChars(compoundWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(contractedWordsLabel.getText() + TAB + StringUtils.replaceChars(contractedWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(lengthsModeLabel.getText() + TAB + lengthsModeOutputLabel.getText());
			writer.newLine();
			if(hasSyllabeStatistics){
				writer.write(syllabeLengthsModeLabel.getText() + TAB + syllabeLengthsModeOutputLabel.getText());
				writer.newLine();
				writer.write(mostCommonSyllabesLabel.getText() + TAB + mostCommonSyllabesOutputLabel.getText());
				writer.newLine();
			}
			writer.write(longestWordCharactersLabel.getText() + TAB + longestWordCharactersOutputLabel.getText());
			writer.newLine();
			if(hasSyllabeStatistics){
				writer.write(longestWordSyllabesLabel.getText() + TAB + longestWordSyllabesOutputLabel.getText());
				writer.newLine();
			}

			exportGraph(writer, lengthsPanel);
			exportGraph(writer, syllabesPanel);
			exportGraph(writer, stressesPanel);
		}
	}

	private void exportGraph(BufferedWriter writer, Component comp) throws IOException{
		int index = mainTabbedPane.indexOfComponent(comp);
		boolean hasData = mainTabbedPane.isEnabledAt(index);
		if(hasData){
			String name = mainTabbedPane.getTitleAt(index);
			CategorySeries series = ((CategoryChart)((XChartPanel<?>)comp).getChart()).getSeriesMap().get(SERIES_NAME);
			Iterator<?> xItr = series.getXData().iterator();
			Iterator<? extends Number> yItr = series.getYData().iterator();
			writer.newLine();
			writer.write(name);
			writer.newLine();
			while(xItr.hasNext()){
				writer.write(xItr.next() + ":" + TAB + DictionaryParser.PERCENT_FORMATTER_1.format(yItr.next()));
				writer.newLine();
			}
		}
	}

	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel compoundWordsLabel;
   private javax.swing.JLabel compoundWordsOutputLabel;
   private javax.swing.JLabel contractedWordsLabel;
   private javax.swing.JLabel contractedWordsOutputLabel;
   private javax.swing.JButton exportButton;
   private javax.swing.JLabel lengthsModeLabel;
   private javax.swing.JLabel lengthsModeOutputLabel;
   private javax.swing.JPanel lengthsPanel;
   private javax.swing.JLabel longestWordCharactersLabel;
   private javax.swing.JLabel longestWordCharactersOutputLabel;
   private javax.swing.JLabel longestWordSyllabesLabel;
   private javax.swing.JLabel longestWordSyllabesOutputLabel;
   private javax.swing.JTabbedPane mainTabbedPane;
   private javax.swing.JLabel mostCommonSyllabesLabel;
   private javax.swing.JLabel mostCommonSyllabesOutputLabel;
   private javax.swing.JPanel stressesPanel;
   private javax.swing.JLabel syllabeLengthsModeLabel;
   private javax.swing.JLabel syllabeLengthsModeOutputLabel;
   private javax.swing.JPanel syllabesPanel;
   private javax.swing.JLabel totalWordsLabel;
   private javax.swing.JLabel totalWordsOutputLabel;
   private javax.swing.JLabel uniqueWordsLabel;
   private javax.swing.JLabel uniqueWordsOutputLabel;
   // End of variables declaration//GEN-END:variables

}
