package unit731.hunlinter.actions;

import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.dictionary.WordlistWorker;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class DictionaryExtractWordlistAction extends AbstractAction{

	private final WordlistWorker.WorkerType type;
	private final WorkerManager workerManager;
	private final JFrame parentFrame;
	private final PropertyChangeListener propertyChangeListener;

	private final JFileChooser saveResultFileChooser;


	public DictionaryExtractWordlistAction(final WordlistWorker.WorkerType type, final WorkerManager workerManager, final JFrame parentFrame,
			final PropertyChangeListener propertyChangeListener){
		super("dictionary.extractWordlist", new ImageIcon(DictionaryExtractWordlistAction.class.getResource("/dictionary_wordlist.png")));

		putValue(SHORT_DESCRIPTION, "Extract wordlist…");

		Objects.requireNonNull(type);
		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(parentFrame);
		Objects.requireNonNull(propertyChangeListener);

		this.type = type;
		this.workerManager = workerManager;
		this.parentFrame = parentFrame;
		this.propertyChangeListener = propertyChangeListener;

		saveResultFileChooser = new JFileChooser();
		saveResultFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		saveResultFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		workerManager.createWordlistWorker(
			type,
			() -> {
				final int fileChosen = saveResultFileChooser.showSaveDialog(parentFrame);
				return (fileChosen == JFileChooser.APPROVE_OPTION? saveResultFileChooser.getSelectedFile(): null);
			},
			worker -> {
				setEnabled(false);

				worker.addPropertyChangeListener(propertyChangeListener);
				worker.execute();
			},
			worker -> setEnabled(true)
		);
	}

}
