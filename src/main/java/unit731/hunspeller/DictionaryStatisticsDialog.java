package unit731.hunspeller;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.Frequency;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.CategorySeries;
import org.knowm.xchart.CategorySeries.CategorySeriesRenderStyle;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler;
import unit731.hunspeller.parsers.dictionary.dtos.DictionaryStatistics;


@Slf4j
public class DictionaryStatisticsDialog extends JDialog{

	private static final long serialVersionUID = 5762751368059394067l;

	private static final DecimalFormat CHI_SQUARE_FORMATTER = (DecimalFormat)NumberFormat.getInstance(Locale.US);
	static{
		DecimalFormatSymbols symbols = CHI_SQUARE_FORMATTER.getDecimalFormatSymbols();
		symbols.setGroupingSeparator(' ');
		CHI_SQUARE_FORMATTER.setDecimalFormatSymbols(symbols);
		CHI_SQUARE_FORMATTER.setMinimumFractionDigits(4);
		CHI_SQUARE_FORMATTER.setMaximumFractionDigits(4);
	}


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
      wordLengthsPanel = createChartPanel("Word length distribution", "Word length", "Frequency");
      wordSyllabesPanel = createChartPanel("Word syllabe distribution", "Word syllabe", "Frequency");
      totalProductionsOutputLabel = new javax.swing.JLabel();
      wordsLengthsModeLabel = new javax.swing.JLabel();
      wordsLengthsModeOutputLabel = new javax.swing.JLabel();
      wordsSyllabesModeLabel = new javax.swing.JLabel();
      wordsSyllabesModeOutputLabel = new javax.swing.JLabel();
      poissonDistributionPValueLabel = new javax.swing.JLabel();
      poissonDistributionPValueOutputLabel = new javax.swing.JLabel();
      jLabel1 = new javax.swing.JLabel();
      jLabel2 = new javax.swing.JLabel();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      totalProductionsLabel.setText("Total productions:");

      javax.swing.GroupLayout wordLengthsPanelLayout = new javax.swing.GroupLayout(wordLengthsPanel);
      wordLengthsPanel.setLayout(wordLengthsPanelLayout);
      wordLengthsPanelLayout.setHorizontalGroup(
         wordLengthsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 655, Short.MAX_VALUE)
      );
      wordLengthsPanelLayout.setVerticalGroup(
         wordLengthsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 301, Short.MAX_VALUE)
      );

      mainTabbedPane.addTab("Word lengths", wordLengthsPanel);

      javax.swing.GroupLayout wordSyllabesPanelLayout = new javax.swing.GroupLayout(wordSyllabesPanel);
      wordSyllabesPanel.setLayout(wordSyllabesPanelLayout);
      wordSyllabesPanelLayout.setHorizontalGroup(
         wordSyllabesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 655, Short.MAX_VALUE)
      );
      wordSyllabesPanelLayout.setVerticalGroup(
         wordSyllabesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGap(0, 301, Short.MAX_VALUE)
      );

      mainTabbedPane.addTab("Word syllabes", wordSyllabesPanel);

      totalProductionsOutputLabel.setText("...");

      wordsLengthsModeLabel.setText("Words' lengths' mode:");

      wordsLengthsModeOutputLabel.setText("...");

      wordsSyllabesModeLabel.setText("Words' syllabes' mode:");

      wordsSyllabesModeOutputLabel.setText("...");

      poissonDistributionPValueLabel.setText("Fitting to a Poisson distribution:");

      poissonDistributionPValueOutputLabel.setText("...");

      jLabel1.setText("jLabel1");

      jLabel2.setText("jLabel2");

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(wordsLengthsModeLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(wordsLengthsModeOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(totalProductionsLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(totalProductionsOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(wordsSyllabesModeLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(wordsSyllabesModeOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(poissonDistributionPValueLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(poissonDistributionPValueOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(mainTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 660, javax.swing.GroupLayout.PREFERRED_SIZE)
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(31, 31, 31)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 484, javax.swing.GroupLayout.PREFERRED_SIZE)))
                  .addGap(0, 0, Short.MAX_VALUE)))
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
               .addComponent(wordsLengthsModeLabel)
               .addComponent(wordsLengthsModeOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(wordsSyllabesModeLabel)
               .addComponent(wordsSyllabesModeOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(poissonDistributionPValueLabel)
               .addComponent(poissonDistributionPValueOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(jLabel1)
               .addComponent(jLabel2))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 25, Short.MAX_VALUE)
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
		Frequency lengthsFrequencies = statistics.getLengthsFrequencies();
		Frequency syllabesFrequencies = statistics.getSyllabesFrequencies();

		totalProductionsOutputLabel.setText(Long.toString(statistics.getTotalProductions()));
		wordsLengthsModeOutputLabel.setText(((Long)lengthsFrequencies.getMode().get(0)).toString());
		wordsSyllabesModeOutputLabel.setText(((Long)syllabesFrequencies.getMode().get(0)).toString());
		double lengthsChiSquare = DictionaryStatistics.chiSquareTest(lengthsFrequencies);
		DictionaryStatistics.ChiSquareConclusion conclusion = DictionaryStatistics.ChiSquareConclusion.determineConclusion(lengthsChiSquare);
		poissonDistributionPValueOutputLabel.setText(conclusion.getDescription() + " (" + CHI_SQUARE_FORMATTER.format(lengthsChiSquare) + ")");
		double syllabesChiSquare = DictionaryStatistics.chiSquareTest(syllabesFrequencies);
		conclusion = DictionaryStatistics.ChiSquareConclusion.determineConclusion(syllabesChiSquare);
		jLabel2.setText(conclusion.getDescription() + " (" + CHI_SQUARE_FORMATTER.format(syllabesChiSquare) + ")");

		CategoryChart wordLengthsChart = (CategoryChart)((XChartPanel)wordLengthsPanel).getChart();
		addSeriesToChart(wordLengthsChart, "series", lengthsFrequencies, statistics.getTotalProductions());

		CategoryChart wordSyllabesChart = (CategoryChart)((XChartPanel)wordSyllabesPanel).getChart();
		addSeriesToChart(wordSyllabesChart, "series", syllabesFrequencies, statistics.getTotalProductions());
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

	private void addSeriesToChart(CategoryChart chart, String seriesName, Frequency freqs, long totalCount){
		List<Double> equivalentPoissonDistribution = Arrays.asList(ArrayUtils.toObject(DictionaryStatistics.getEquivalentPoissonDistribution(freqs)));

		int index = 0;
		List<Integer> xData = new ArrayList<>();
		List<Double> yData = new ArrayList<>();
		Iterator<Map.Entry<Comparable<?>, Long>> itr = freqs.entrySetIterator();
		while(itr.hasNext()){
			Map.Entry<Comparable<?>, Long> elem = itr.next();
			xData.add(((Long)elem.getKey()).intValue());
			yData.add(elem.getValue().doubleValue() / totalCount);

			equivalentPoissonDistribution.set(index, equivalentPoissonDistribution.get(index) / totalCount);
			index ++;
		}

		chart.addSeries(seriesName, xData, yData);

		CategorySeries seriesPoisson = chart.addSeries("poisson", xData, equivalentPoissonDistribution);
		seriesPoisson.setChartCategorySeriesRenderStyle(CategorySeriesRenderStyle.Line);
		seriesPoisson.setMarkerColor(Color.red);
		seriesPoisson.setLineColor(Color.red);
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
				DictionaryStatistics stats = new DictionaryStatistics();
				stats.addLengthAndSyllabes(0, 3);
				stats.addLengthAndSyllabes(1, 1);
				stats.addLengthAndSyllabes(0, 2);
				stats.addLengthAndSyllabes(2, 3);
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
   private javax.swing.JLabel jLabel1;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JTabbedPane mainTabbedPane;
   private javax.swing.JLabel poissonDistributionPValueLabel;
   private javax.swing.JLabel poissonDistributionPValueOutputLabel;
   private javax.swing.JLabel totalProductionsLabel;
   private javax.swing.JLabel totalProductionsOutputLabel;
   private javax.swing.JPanel wordLengthsPanel;
   private javax.swing.JPanel wordSyllabesPanel;
   private javax.swing.JLabel wordsLengthsModeLabel;
   private javax.swing.JLabel wordsLengthsModeOutputLabel;
   private javax.swing.JLabel wordsSyllabesModeLabel;
   private javax.swing.JLabel wordsSyllabesModeOutputLabel;
   // End of variables declaration//GEN-END:variables
}
