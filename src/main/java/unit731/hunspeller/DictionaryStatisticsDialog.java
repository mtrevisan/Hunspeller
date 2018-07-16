package unit731.hunspeller;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler;
import unit731.hunspeller.parsers.dictionary.dtos.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.valueobjects.Frequency;


@Slf4j
public class DictionaryStatisticsDialog extends JDialog{

	private static final long serialVersionUID = 5762751368059394067l;


	private final DictionaryStatistics statistics;


	public DictionaryStatisticsDialog(DictionaryStatistics statistics, Frame parent) throws InterruptedException, InvocationTargetException{
		super(parent, "Dictionary statistics", true);

		Objects.requireNonNull(statistics);
		Objects.requireNonNull(parent);

		this.statistics = statistics;

		initComponents();

		addClearOnClose();

		addCancelByEscapeKey();


		fillStatisticDatas();
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      totalProductionsLabel = new javax.swing.JLabel();
      mainTabbedPane = new javax.swing.JTabbedPane();
      lengthsPanel = createChartPanel("Word length distribution", "Word length", "Frequency");
      syllabesPanel = createChartPanel("Word syllabe distribution", "Word syllabe", "Frequency");
      stressesPanel = createChartPanel("Word stress distribution", "Word stressed syllabe index (from last)", "Frequency");
      totalProductionsOutputLabel = new javax.swing.JLabel();
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
      uniqueWordsLabel = new javax.swing.JLabel();
      uniqueWordsOutputLabel = new javax.swing.JLabel();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      totalProductionsLabel.setText("Total productions:");

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

      totalProductionsOutputLabel.setText("...");

      lengthsModeLabel.setText("Words' length mode:");

      lengthsModeOutputLabel.setText("...");

      syllabeLengthsModeLabel.setText("Words' syllabe mode:");

      syllabeLengthsModeOutputLabel.setText("...");

      mostCommonSyllabesLabel.setText("Most common syllabes:");

      mostCommonSyllabesOutputLabel.setText("...");

      longestWordCharactersLabel.setText("Longest word(s) (by characters):");

      longestWordCharactersOutputLabel.setText("...");

      longestWordSyllabesLabel.setText("Longest word(s) (by syllabes):");

      longestWordSyllabesOutputLabel.setText("...");

      uniqueWordsLabel.setText("Unique words:");

      uniqueWordsOutputLabel.setText("...");

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lengthsModeLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(lengthsModeOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(totalProductionsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(totalProductionsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
               .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                  .addGroup(layout.createSequentialGroup()
                     .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 660, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                           .addComponent(longestWordCharactersLabel)
                           .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                           .addComponent(longestWordCharactersOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                     .addGap(0, 0, Short.MAX_VALUE))
                  .addGroup(layout.createSequentialGroup()
                     .addComponent(uniqueWordsLabel)
                     .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                     .addComponent(uniqueWordsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(totalProductionsLabel)
               .addComponent(totalProductionsOutputLabel))
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
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(uniqueWordsLabel)
               .addComponent(uniqueWordsOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE)
            .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 329, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	private void addClearOnClose(){
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e){
				statistics.clear();
			}
		});
	}

	/** Force the escape key to call the same action as pressing the Cancel button. */
	private void addCancelByEscapeKey(){
		AbstractAction cancelAction = new AbstractAction(){
			private static final long serialVersionUID = -5644390861803492172l;

			@Override
			public void actionPerformed(ActionEvent e){
				dispose();
			}
		};
		KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		getRootPane().registerKeyboardAction(cancelAction, escapeKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	private void fillStatisticDatas(){
		Frequency<Integer> lengthsFrequencies = statistics.getLengthsFrequencies();
		Frequency<Integer> syllabeLengthsFrequencies = statistics.getSyllabeLengthsFrequencies();
		Frequency<Integer> stressesFrequencies = statistics.getStressFromLastFrequencies();
		long totalProductions = statistics.getTotalProductions();
		List<String> mostCommonSyllabes = statistics.getMostCommonSyllabes(5);
		int longestWordCharsCount = statistics.getLongestWordCountByCharacters();
		List<String> longestWordsChars = statistics.getLongestWordsByCharacters();
		int longestWordSyllabesCount = statistics.getLongestWordCountBySyllabes();
		List<String> longestSyllabesChars = statistics.getLongestWordsBySyllabes();
		double uniqueWords = statistics.uniqueWords();

		if(totalProductions > 0){
			totalProductionsOutputLabel.setText(HunspellerFrame.COUNTER_FORMATTER.format(totalProductions));
			lengthsModeOutputLabel.setText(String.join(", ", lengthsFrequencies.getMode().stream().map(String::valueOf).collect(Collectors.toList())));
			syllabeLengthsModeOutputLabel.setText(String.join(", ", syllabeLengthsFrequencies.getMode().stream().map(String::valueOf).collect(Collectors.toList())));
			mostCommonSyllabesOutputLabel.setText(String.join(", ", mostCommonSyllabes));
			longestWordCharactersOutputLabel.setText(String.join(", ", longestWordsChars) + " (" + longestWordCharsCount + ")");
			longestWordSyllabesOutputLabel.setText(String.join(", ", longestSyllabesChars) + " (" + longestWordSyllabesCount + ")");
			uniqueWordsOutputLabel.setText(DictionaryStatistics.PERCENT_FORMATTER.format(uniqueWords));

			CategoryChart wordLengthsChart = (CategoryChart)((XChartPanel)lengthsPanel).getChart();
			addSeriesToChart(wordLengthsChart, lengthsFrequencies, totalProductions);

			CategoryChart wordSyllabesChart = (CategoryChart)((XChartPanel)syllabesPanel).getChart();
			addSeriesToChart(wordSyllabesChart, syllabeLengthsFrequencies, totalProductions);

			CategoryChart wordStressesChart = (CategoryChart)((XChartPanel)stressesPanel).getChart();
			addSeriesToChart(wordStressesChart, stressesFrequencies, totalProductions);
		}
	}

	private JPanel createChartPanel(String title, String xAxisTitle, String yAxisTitle){
		CategoryChart chart = new CategoryChartBuilder()
			.title(title)
			.xAxisTitle(xAxisTitle)
//			.yAxisTitle(yAxisTitle)
			.theme(Styler.ChartTheme.Matlab)
			.build();

		CategoryStyler styler = chart.getStyler();
		styler.setAvailableSpaceFill(0.98);
		styler.setOverlapped(true);
		styler.setLegendVisible(false);
		styler.setXAxisMin(0.);
		styler.setYAxisMin(0.);
		styler.setYAxisDecimalPattern("#%");
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

		chart.addSeries("series", xData, yData);
	}


	public static void main(String args[]){
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		try{
			String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex){
			log.error(null, ex);
		}
		//</editor-fold>

		java.awt.EventQueue.invokeLater(() -> {
			try{
				DictionaryStatistics stats = new DictionaryStatistics(StandardCharsets.UTF_8);
				stats.addLengthAndSyllabeLengthAndStressFromLast(0, 3, 1);
				stats.addLengthAndSyllabeLengthAndStressFromLast(1, 1, 0);
				stats.addLengthAndSyllabeLengthAndStressFromLast(0, 2, 1);
				stats.addLengthAndSyllabeLengthAndStressFromLast(2, 3, 1);
				javax.swing.JFrame parent = new javax.swing.JFrame();
				DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(stats, parent);
				dialog.setLocationRelativeTo(parent);
				dialog.addWindowListener(new java.awt.event.WindowAdapter(){
					@Override
					public void windowClosing(java.awt.event.WindowEvent e){
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
			catch(IllegalArgumentException | InterruptedException | InvocationTargetException ex){
				log.error(null, ex);
			}
		});
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
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
   private javax.swing.JLabel totalProductionsLabel;
   private javax.swing.JLabel totalProductionsOutputLabel;
   private javax.swing.JLabel uniqueWordsLabel;
   private javax.swing.JLabel uniqueWordsOutputLabel;
   // End of variables declaration//GEN-END:variables
}
