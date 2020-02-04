package unit731.hunlinter;

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
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.DictionaryStatistics;
import unit731.hunlinter.parsers.dictionary.Frequency;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.services.FileHelper;


public class DictionaryStatisticsDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryStatisticsDialog.class);

	private static final long serialVersionUID = 5762751368059394067l;

	private static final String LIST_SEPARATOR = ", ";
	private static final String TAB = "\t";

	private final DictionaryStatistics statistics;

	private final JFileChooser saveTextFileFileChooser;


	public DictionaryStatisticsDialog(final DictionaryStatistics statistics, final Frame parent){
		super(parent, "Dictionary statistics", false);

		Objects.requireNonNull(statistics);
		Objects.requireNonNull(parent);

		this.statistics = statistics;

		initComponents();

		final Font currentFont = GUIUtils.getCurrentFont();
		mostCommonSyllabesOutputLabel.setFont(currentFont);
		longestWordCharactersOutputLabel.setFont(currentFont);
		longestWordSyllabesOutputLabel.setFont(currentFont);

		try{
			final JPopupMenu popupMenu = new JPopupMenu();
			popupMenu.add(GUIUtils.createPopupCopyMenu(compoundWordsOutputLabel.getHeight(), popupMenu, GUIUtils::copyCallback));
			GUIUtils.addPopupMenu(popupMenu, compoundWordsOutputLabel, contractedWordsOutputLabel, lengthsModeOutputLabel,
				longestWordCharactersOutputLabel, longestWordSyllabesOutputLabel, mostCommonSyllabesOutputLabel,
				syllabeLengthsModeOutputLabel, totalWordsOutputLabel, uniqueWordsOutputLabel);
		}
		catch(final IOException ignored){}

		addListenerOnClose();

		saveTextFileFileChooser = new JFileChooser();
		saveTextFileFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		final File currentDir = new File(".");
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

      totalWordsOutputLabel.setText("…");

      uniqueWordsLabel.setLabelFor(uniqueWordsOutputLabel);
      uniqueWordsLabel.setText("Unique words:");

      uniqueWordsOutputLabel.setText("…");

      compoundWordsLabel.setText("Compound words:");

      compoundWordsOutputLabel.setText("…");

      contractedWordsLabel.setText("Contracted words:");

      contractedWordsOutputLabel.setText("…");

      lengthsModeLabel.setLabelFor(lengthsModeOutputLabel);
      lengthsModeLabel.setText("Mode of wordsʼ length:");

      lengthsModeOutputLabel.setText("…");

      syllabeLengthsModeLabel.setLabelFor(syllabeLengthsModeOutputLabel);
      syllabeLengthsModeLabel.setText("Mode of wordsʼ syllabe:");

      syllabeLengthsModeOutputLabel.setText("…");

      mostCommonSyllabesLabel.setLabelFor(mostCommonSyllabesOutputLabel);
      mostCommonSyllabesLabel.setText("Most common syllabes:");
      mostCommonSyllabesLabel.setPreferredSize(new java.awt.Dimension(113, 17));

      mostCommonSyllabesOutputLabel.setText("…");
      mostCommonSyllabesOutputLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      longestWordCharactersLabel.setLabelFor(longestWordCharactersOutputLabel);
      longestWordCharactersLabel.setText("Longest word(s) (by characters):");
      longestWordCharactersLabel.setPreferredSize(new java.awt.Dimension(158, 17));

      longestWordCharactersOutputLabel.setText("…");
      longestWordCharactersOutputLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      longestWordSyllabesLabel.setLabelFor(longestWordSyllabesOutputLabel);
      longestWordSyllabesLabel.setText("Longest word(s) (by syllabes):");
      longestWordSyllabesLabel.setPreferredSize(new java.awt.Dimension(146, 17));
      longestWordSyllabesLabel.setRequestFocusEnabled(false);

      longestWordSyllabesOutputLabel.setText("…");
      longestWordSyllabesOutputLabel.setPreferredSize(new java.awt.Dimension(9, 17));

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
                  .addComponent(mostCommonSyllabesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(mostCommonSyllabesOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(longestWordSyllabesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
                  .addComponent(longestWordCharactersLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
               .addComponent(mostCommonSyllabesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(mostCommonSyllabesOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(longestWordCharactersLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(longestWordCharactersOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(longestWordSyllabesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(longestWordSyllabesOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 329, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(exportButton)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
		final int fileChosen = saveTextFileFileChooser.showSaveDialog(this);
		if(fileChosen == JFileChooser.APPROVE_OPTION){
			exportButton.setEnabled(false);

			try{
				final File outputFile = saveTextFileFileChooser.getSelectedFile();
				exportToFile(outputFile);

				FileHelper.openFolder(outputFile);
			}
			catch(final Exception e){
				LOGGER.error("Cannot export statistics", e);
			}

			exportButton.setEnabled(true);
		}
   }//GEN-LAST:event_exportButtonActionPerformed

	private void addListenerOnClose(){
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowDeactivated(final WindowEvent e){
				statistics.clear();
			}
		});
	}

	private void fillStatisticData(){
		final long totalWords = statistics.getTotalProductions();
		if(totalWords > 0){
			fillBaseStatistics();
			if(statistics.hasSyllabeStatistics())
				fillSyllabeStatistics();
			else
				cleanupSyllabeStatistics();

			fillLengthsFrequencies(statistics.getLengthsFrequencies(), totalWords, (ChartPanel)lengthsPanel);

			fillLengthsFrequencies(statistics.getSyllabeLengthsFrequencies(), totalWords, (ChartPanel)syllabesPanel);

			fillLengthsFrequencies(statistics.getStressFromLastFrequencies(), totalWords, (ChartPanel)stressesPanel);
		}
	}

	private void fillBaseStatistics(){
		final long totalWords = statistics.getTotalProductions();
		final int uniqueWords = statistics.getUniqueWords();
		final int contractedWords = statistics.getContractedWords();
		final Frequency<Integer> lengthsFrequencies = statistics.getLengthsFrequencies();
		final int longestWordCharsCount = statistics.getLongestWordCountByCharacters();
		List<String> longestWords = statistics.getLongestWordsByCharacters();
		longestWords = DictionaryStatistics.extractRepresentatives(longestWords, 4);

		final String formattedTotalWords = DictionaryParser.COUNTER_FORMATTER.format(totalWords);
		double x = (double)uniqueWords / totalWords;
		final String formattedUniqueWords = DictionaryParser.COUNTER_FORMATTER.format(uniqueWords)
			+ String.format(Locale.ROOT, " (%." + Frequency.getDecimals(x) + "f%%)", x * 100.);
		x = (double)contractedWords / totalWords;
		final String formattedContractedWords = DictionaryParser.COUNTER_FORMATTER.format(contractedWords)
			+ String.format(Locale.ROOT, " (%." + Frequency.getDecimals(x) + "f%%)", x * 100.);
		final String formattedLengthsMode = lengthsFrequencies.getMode().stream()
			.map(String::valueOf)
			.collect(Collectors.joining(LIST_SEPARATOR));
		final String formattedLongestWords = StringUtils.join(longestWords, LIST_SEPARATOR)
			+ " (" + longestWordCharsCount + ")";

		totalWordsOutputLabel.setText(formattedTotalWords);
		uniqueWordsOutputLabel.setText(formattedUniqueWords);
		contractedWordsOutputLabel.setText(formattedContractedWords);
		lengthsModeOutputLabel.setText(formattedLengthsMode);
		longestWordCharactersOutputLabel.setText(formattedLongestWords);
	}

	private void fillSyllabeStatistics(){
		final int compoundWords = statistics.getCompoundWords();
		final int uniqueWords = statistics.getUniqueWords();
		final Frequency<Integer> syllabeLengthsFrequencies = statistics.getSyllabeLengthsFrequencies();
		final List<String> mostCommonSyllabes = statistics.getMostCommonSyllabes(7);
		List<String> longestWordSyllabes = statistics.getLongestWordsBySyllabes().stream()
			.map(Hyphenation::getSyllabes)
			.map(syllabes -> StringUtils.join(syllabes, HyphenationParser.SOFT_HYPHEN))
			.collect(Collectors.toList());
		longestWordSyllabes = DictionaryStatistics.extractRepresentatives(longestWordSyllabes, 4);
		final int longestWordSyllabesCount = statistics.getLongestWordCountBySyllabes();

		double x = (double)compoundWords / uniqueWords;
		final String formattedCompoundWords = DictionaryParser.COUNTER_FORMATTER.format(compoundWords)
			+ String.format(Locale.ROOT, " (%." + Frequency.getDecimals(x) + "f%%)", x * 100.);
		final String formattedSyllabeLengthsMode = syllabeLengthsFrequencies.getMode().stream()
			.map(String::valueOf)
			.collect(Collectors.joining(LIST_SEPARATOR));
		final String formattedMostCommonSyllabes = StringUtils.join(mostCommonSyllabes, LIST_SEPARATOR);
		final String formattedLongestWordSyllabes = StringUtils.join(longestWordSyllabes, LIST_SEPARATOR)
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

	private void fillLengthsFrequencies(final Frequency<Integer> frequencies, final long totalSamples, final ChartPanel panel){
		final boolean hasData = frequencies.entrySetIterator().hasNext();

		mainTabbedPane.setEnabledAt(mainTabbedPane.indexOfComponent(panel), hasData);
		if(hasData){
			//extract data set
			final XYSeries series = new XYSeries("frequencies");
			final Iterator<Map.Entry<Integer, Long>> itr = frequencies.entrySetIterator();
			while(itr.hasNext()){
				final Map.Entry<Integer, Long> elem = itr.next();
				series.add(elem.getKey().doubleValue(), elem.getValue().doubleValue() / totalSamples);
			}
			final XYSeriesCollection dataset = new XYSeriesCollection(series);

			panel.getChart().getXYPlot().setDataset(dataset);
		}
	}

	private JPanel createChartPanel(final String title, final String xAxisTitle, final String yAxisTitle){
		final JFreeChart chart = createChart(title, xAxisTitle, yAxisTitle);
		final ChartPanel panel = new ChartPanel(chart){
			@Override
			protected JPopupMenu createPopupMenu(final boolean properties, final boolean copy, final boolean save,
					final boolean print, final boolean zoom){
				final JPopupMenu result = new JPopupMenu("Chart:");
				final JMenuItem propertiesItem = new JMenuItem("Properties...");
				propertiesItem.setActionCommand("PROPERTIES");
				propertiesItem.addActionListener(this);
				result.add(propertiesItem);

				result.addSeparator();

				final JMenuItem saveAsPNGItem = new JMenuItem("Save as PNG...");
				saveAsPNGItem.setActionCommand("SAVE_AS_PNG");
				saveAsPNGItem.addActionListener(this);
				result.add(saveAsPNGItem);

				result.addSeparator();

				final JMenuItem printItem = new JMenuItem("Print...");
				printItem.setActionCommand("PRINT");
				printItem.addActionListener(this);
				result.add(printItem);

				return result;
			}
		};
		return panel;
	}

	private JFreeChart createChart(final String title, final String xAxisTitle, final String yAxisTitle){
		final XYPlot plot = createChartPlot(xAxisTitle, yAxisTitle);
		return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
	}

	private XYPlot createChartPlot(final String xAxisTitle, final String yAxisTitle){
		final XYBarRenderer renderer = createChartRenderer();
		final NumberAxis xAxis = createChartXAxis(xAxisTitle);
		final NumberAxis yAxis = createChartYAxis(yAxisTitle);

		final XYPlot plot = new XYPlot(null, xAxis, yAxis, renderer);
		plot.setOrientation(PlotOrientation.VERTICAL);
		//background color
		plot.setBackgroundPaint(Color.WHITE);
		//gridlines
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(true);
		plot.setRangeGridlinePaint(Color.BLACK);
		return plot;
	}

	private XYBarRenderer createChartRenderer(){
		final XYBarRenderer renderer = new XYBarRenderer();
		renderer.setSeriesStroke(0, new BasicStroke(1.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
			1.f, new float[]{10.f, 6.f}, 0.f));
		//bar color
		renderer.setSeriesPaint(0, Color.BLUE);
		//solid bar color
		renderer.setBarPainter(new StandardXYBarPainter());
		//shadow
		renderer.setShadowVisible(false);
		//tooltip
		final XYToolTipGenerator xyToolTipGenerator = createChartTooltip();
		renderer.setDefaultToolTipGenerator(xyToolTipGenerator);
		return renderer;
	}

	private XYToolTipGenerator createChartTooltip(){
		return (dataset, series, item) -> {
			final Number x = dataset.getX(series, item);
			final Number y = dataset.getY(series, item);
			return String.format(Locale.ROOT, "(%d, %.1f%%)", x.intValue(), y.doubleValue() * 100.);
		};
	}

	private NumberAxis createChartXAxis(final String xAxisTitle){
		//x-axis as integer starting from zero
		final NumberAxis xAxis = new NumberAxis(xAxisTitle);
		xAxis.setAutoRangeIncludesZero(false);
		xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		xAxis.setLowerBound(0.);
		xAxis.setAutoRange(true);
		return xAxis;
	}

	private NumberAxis createChartYAxis(final String yAxisTitle){
		//y-axis as percent starting from zero
		final NumberAxis yAxis = new NumberAxis(yAxisTitle);
		yAxis.setAutoRangeIncludesZero(true);
		yAxis.setNumberFormatOverride(new DecimalFormat("#%"));
		yAxis.setLowerBound(0.);
		yAxis.setAutoRange(true);
		return yAxis;
	}

	private void exportToFile(final File outputFile) throws IOException{
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)){
			final boolean hasSyllabeStatistics = syllabeLengthsModeLabel.isEnabled();

			writer.write(totalWordsLabel.getText() + TAB
				+ StringUtils.replaceChars(totalWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(uniqueWordsLabel.getText() + TAB
				+ StringUtils.replaceChars(uniqueWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(compoundWordsLabel.getText() + TAB
				+ StringUtils.replaceChars(compoundWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(contractedWordsLabel.getText() + TAB
				+ StringUtils.replaceChars(contractedWordsOutputLabel.getText(), DictionaryParser.COUNTER_GROUPING_SEPARATOR, ' '));
			writer.newLine();
			writer.write(lengthsModeLabel.getText() + TAB
				+ lengthsModeOutputLabel.getText());
			writer.newLine();
			if(hasSyllabeStatistics){
				writer.write(syllabeLengthsModeLabel.getText() + TAB
					+ syllabeLengthsModeOutputLabel.getText());
				writer.newLine();
				writer.write(mostCommonSyllabesLabel.getText() + TAB
					+ mostCommonSyllabesOutputLabel.getText());
				writer.newLine();
			}
			writer.write(longestWordCharactersLabel.getText() + TAB
				+ longestWordCharactersOutputLabel.getText());
			writer.newLine();
			if(hasSyllabeStatistics){
				writer.write(longestWordSyllabesLabel.getText() + TAB
					+ longestWordSyllabesOutputLabel.getText());
				writer.newLine();
			}

			exportGraph(writer, lengthsPanel);
			exportGraph(writer, syllabesPanel);
			exportGraph(writer, stressesPanel);
		}
	}

	private void exportGraph(final BufferedWriter writer, final Component comp) throws IOException{
		final int index = mainTabbedPane.indexOfComponent(comp);
		final boolean hasData = mainTabbedPane.isEnabledAt(index);
		if(hasData){
			final String name = mainTabbedPane.getTitleAt(index);
			final XYDataset dataset = ((ChartPanel) comp).getChart().getXYPlot().getDataset(0);
			final Iterator<?> xItr = ((XYSeriesCollection)dataset).getSeries(0).getItems().iterator();
			writer.newLine();
			writer.write(name);
			writer.newLine();
			while(xItr.hasNext()){
				final XYDataItem xy = (XYDataItem)xItr.next();
				final double y = xy.getY().doubleValue();
				final int decimals = Frequency.getDecimals(y);
				final String line = String.format(Locale.ROOT, "%d:\t%." + decimals + "f%%",
					xy.getX().intValue(), y * 100.);
				writer.write(line);
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
