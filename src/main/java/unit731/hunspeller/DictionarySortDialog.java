package unit731.hunspeller;

import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import unit731.hunspeller.gui.DictionarySortCellRenderer;
import unit731.hunspeller.gui.GUIUtils;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;


public class DictionarySortDialog extends JDialog{

	private static final long serialVersionUID = -4815599935456195094L;


	private final DictionaryParser dicParser;


	public DictionarySortDialog(DictionaryParser dicParser, String message, Frame parent){
		super(parent, "Dictionary sorter", true);

		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(message);

		this.dicParser = dicParser;

		initComponents();

		init(message);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      lblMessage = new javax.swing.JLabel();
      mainScrollPane = new javax.swing.JScrollPane();
      entriesList = new javax.swing.JList<>();
      btnNextUnsortedArea = new javax.swing.JButton();
      btnPreviousUnsortedArea = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      lblMessage.setText("...");

      mainScrollPane.setBackground(java.awt.Color.white);
      mainScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

      entriesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      mainScrollPane.setViewportView(entriesList);

      btnNextUnsortedArea.setText("▼");
      btnNextUnsortedArea.setToolTipText("Next unsorted area");
      btnNextUnsortedArea.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnNextUnsortedAreaActionPerformed(evt);
         }
      });

      btnPreviousUnsortedArea.setText("▲");
      btnPreviousUnsortedArea.setToolTipText("Previous unsorted area");
      btnPreviousUnsortedArea.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnPreviousUnsortedAreaActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(mainScrollPane)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lblMessage)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 422, Short.MAX_VALUE)
                  .addComponent(btnNextUnsortedArea)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(btnPreviousUnsortedArea)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(btnNextUnsortedArea)
               .addComponent(btnPreviousUnsortedArea)
               .addComponent(lblMessage))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(mainScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	private void init(String message){
		final ListCellRenderer<String> dicCellRenderer = new DictionarySortCellRenderer(dicParser::getBoundaryIndex,
			GUIUtils.getCurrentFont());
		setCellRenderer(dicCellRenderer);

		lblMessage.setText(message);
	}

   private void btnNextUnsortedAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextUnsortedAreaActionPerformed
		int lineIndex = entriesList.getFirstVisibleIndex();
		int boundaryIndex = dicParser.getNextBoundaryIndex(lineIndex);
		if(boundaryIndex >= 0){
			int visibleLines = entriesList.getLastVisibleIndex() - entriesList.getFirstVisibleIndex();
			boundaryIndex = Math.min(boundaryIndex + visibleLines, entriesList.getModel().getSize() - 1);
		}
		else
			boundaryIndex = 0;
		entriesList.ensureIndexIsVisible(boundaryIndex);
   }//GEN-LAST:event_btnNextUnsortedAreaActionPerformed

   private void btnPreviousUnsortedAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviousUnsortedAreaActionPerformed
		int lineIndex = entriesList.getFirstVisibleIndex();
		int boundaryIndex = dicParser.getPreviousBoundaryIndex(lineIndex);
		if(boundaryIndex < 0){
			boundaryIndex = dicParser.getPreviousBoundaryIndex(entriesList.getModel().getSize());
			int visibleLines = entriesList.getLastVisibleIndex() - entriesList.getFirstVisibleIndex();
			boundaryIndex = Math.min(boundaryIndex + visibleLines, entriesList.getModel().getSize() - 1);
		}
		entriesList.ensureIndexIsVisible(boundaryIndex);
   }//GEN-LAST:event_btnPreviousUnsortedAreaActionPerformed

	public void setCellRenderer(ListCellRenderer<String> renderer){
		entriesList.setCellRenderer(renderer);
	}

	public void addListSelectionListener(ListSelectionListener listener){
		entriesList.addListSelectionListener(listener);
	}

	public void setListData(String[] listData){
		entriesList.setListData(listData);

		//initialize dictionary
		dicParser.calculateDictionaryBoundaries();
	}

	public int getSelectedIndex(){
		return entriesList.getSelectedIndex();
	}

	private void writeObject(ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	private void readObject(ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton btnNextUnsortedArea;
   private javax.swing.JButton btnPreviousUnsortedArea;
   private javax.swing.JList<String> entriesList;
   private javax.swing.JLabel lblMessage;
   private javax.swing.JScrollPane mainScrollPane;
   // End of variables declaration//GEN-END:variables

}
