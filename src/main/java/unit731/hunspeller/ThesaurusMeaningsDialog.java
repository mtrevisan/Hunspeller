package unit731.hunspeller;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.JDialog;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.parsers.thesaurus.dtos.MeaningEntry;
import unit731.hunspeller.parsers.thesaurus.dtos.ThesaurusEntry;
import unit731.hunspeller.services.GUIHelper;


public class ThesaurusMeaningsDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(ThesaurusMeaningsDialog.class);

	private static final long serialVersionUID = 667526009330291911L;


	private final ThesaurusEntry synonym;
	private final BiConsumer<List<MeaningEntry>, String> okButtonAction;

	private final List<MeaningEntry> meanings;


	public ThesaurusMeaningsDialog(ThesaurusEntry synonym, BiConsumer<List<MeaningEntry>, String> okButtonAction, Frame parent){
		super(parent, "Change meanings for \"" + synonym.getSynonym() + "\"", true);

		Objects.requireNonNull(parent);
		Objects.requireNonNull(synonym);
		Objects.requireNonNull(okButtonAction);

		initComponents();

		GUIHelper.addCancelByEscapeKey(this);

		this.synonym = synonym;
		this.okButtonAction = okButtonAction;
		meanings = synonym.getMeanings();
		String content = StringUtils.join(meanings, StringUtils.LF);
		meaningsTextArea.setText(content);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      mainScrollPane = new javax.swing.JScrollPane();
      meaningsTextArea = new javax.swing.JTextArea();
      buttonPanel = new javax.swing.JPanel();
      btnOk = new javax.swing.JButton();
      btnCancel = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      meaningsTextArea.setColumns(20);
      meaningsTextArea.setLineWrap(true);
      meaningsTextArea.setRows(5);
      meaningsTextArea.setWrapStyleWord(true);
      mainScrollPane.setViewportView(meaningsTextArea);

      buttonPanel.setPreferredSize(new java.awt.Dimension(600, 45));

      btnOk.setText("Ok");
      btnOk.setMaximumSize(new java.awt.Dimension(65, 23));
      btnOk.setMinimumSize(new java.awt.Dimension(65, 23));
      btnOk.setPreferredSize(new java.awt.Dimension(65, 23));
      btnOk.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnOkActionPerformed(evt);
         }
      });

      btnCancel.setText("Cancel");
      btnCancel.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnCancelActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout buttonPanelLayout = new javax.swing.GroupLayout(buttonPanel);
      buttonPanel.setLayout(buttonPanelLayout);
      buttonPanelLayout.setHorizontalGroup(
         buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(buttonPanelLayout.createSequentialGroup()
            .addGap(217, 217, 217)
            .addComponent(btnOk, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGap(30, 30, 30)
            .addComponent(btnCancel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGap(223, 223, 223))
      );
      buttonPanelLayout.setVerticalGroup(
         buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonPanelLayout.createSequentialGroup()
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(btnOk, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(btnCancel))
            .addContainerGap())
      );

      buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addComponent(buttonPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(mainScrollPane)
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(mainScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void btnOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOkActionPerformed
		try{
			String text = meaningsTextArea.getText();
			okButtonAction.accept(meanings, text);
		}
		catch(IllegalArgumentException e){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Error while changing the meanings for word \"{}\": {}", synonym.getSynonym(), e.getMessage());
		}

		dispose();
   }//GEN-LAST:event_btnOkActionPerformed

   private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
		dispose();
   }//GEN-LAST:event_btnCancelActionPerformed

	private void writeObject(ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(ThesaurusMeaningsDialog.class.getName());
	}

	private void readObject(ObjectInputStream is) throws IOException, ClassNotFoundException{
		throw new NotSerializableException(ThesaurusMeaningsDialog.class.getName());
	}


	public static void main(String args[]){
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		try{
			String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){
			LOGGER.error(null, e);
		}
		//</editor-fold>

		java.awt.EventQueue.invokeLater(() -> {
			try{
				List<MeaningEntry> meanings = Collections.unmodifiableList(Arrays.asList(
					new MeaningEntry("(noun)|bla|bli|blo"),
					new MeaningEntry("(art)|el|la")
				));
				ThesaurusEntry synonym = new ThesaurusEntry("synonym", meanings);
				javax.swing.JFrame parent = new javax.swing.JFrame();
				ThesaurusMeaningsDialog dialog = new ThesaurusMeaningsDialog(synonym, (means, text) -> {}, parent);
				dialog.setLocationRelativeTo(parent);
				dialog.addWindowListener(new java.awt.event.WindowAdapter(){
					@Override
					public void windowClosing(java.awt.event.WindowEvent e){
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
			catch(IllegalArgumentException e){
				LOGGER.error(null, e);
			}
		});
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton btnCancel;
   private javax.swing.JButton btnOk;
   private javax.swing.JPanel buttonPanel;
   private javax.swing.JScrollPane mainScrollPane;
   private javax.swing.JTextArea meaningsTextArea;
   // End of variables declaration//GEN-END:variables
}
