package unit731.hunlinter.actions;

import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class DictionaryLinterAction extends AbstractAction{

	private static final long serialVersionUID = -167987952493042223L;


	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;


	public DictionaryLinterAction(final WorkerManager workerManager, final PropertyChangeListener propertyChangeListener){
		super("dictionary.linter",
			new ImageIcon(DictionaryLinterAction.class.getResource("/dictionary_correctness.png")));

		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		workerManager.createDictionaryLinterWorker(
			worker -> {
				setEnabled(false);

				worker.addPropertyChangeListener(propertyChangeListener);
				worker.execute();
			},
			worker -> setEnabled(true)
		);
	}

}
