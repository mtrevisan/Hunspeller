package unit731.hunspeller.parsers.dictionary.workers.core;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.*;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;


public abstract class WorkerDictionaryBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerDictionaryBase.class);


	private WorkerDictionary worker;


	public final void createReadWorker(final WorkerData workerData, final BiConsumer<String, Integer> lineProcessor){
		worker = WorkerDictionary.createReadWorker(workerData, lineProcessor);
	}

	public final void createWriteWorker(final WorkerData workerData, final BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor,
			final File outputFile){
		worker = WorkerDictionary.createWriteWorker(workerData, lineProcessor, outputFile);
	}

	public void addPropertyChangeListener(final PropertyChangeListener listener){
		worker.addPropertyChangeListener(listener);
	}

	public abstract String getWorkerName();

	public void execute(){
		clear();

		worker.execute();
	}

	public void executeInline(){
		clear();

		worker.doInBackground();
	}

	public void clear(){}

	public SwingWorker.StateValue getState(){
		return worker.getState();
	}

	public void pause(){
		worker.pause();
	}

	public void resume(){
		worker.resume();
	}

	public void cancel(){
		worker.cancel(true);
	}

	public boolean isCancelled(){
		return worker.isCancelled();
	}

	public boolean isDone(){
		return worker.isDone();
	}

	public void askUserToAbort(Component parentComponent, Runnable cancelTask, Runnable resumeTask){
		Objects.requireNonNull(parentComponent);
		Objects.requireNonNull(cancelTask);
		Objects.requireNonNull(resumeTask);

		worker.pause();

		Object[] options = {"Abort", "Cancel"};
		int answer = JOptionPane.showOptionDialog(parentComponent, "Do you really want to abort the " + worker.getWorkerName() + " task?", "Warning!",
			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if(answer == JOptionPane.YES_OPTION){
			worker.cancel(true);

			cancelTask.run();

			LOGGER.info(Backbone.MARKER_APPLICATION, worker.getWorkerName() + " aborted");
		}
		else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION){
			worker.resume();

			resumeTask.run();
		}
	}

}
