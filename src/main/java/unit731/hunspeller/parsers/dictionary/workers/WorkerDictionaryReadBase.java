package unit731.hunspeller.parsers.dictionary.workers;

import java.beans.PropertyChangeListener;
import java.util.function.Consumer;
import javax.swing.SwingWorker;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.WorkerDictionaryRead;


public class WorkerDictionaryReadBase{

	private WorkerDictionaryRead worker;


	public final void createWorker(Backbone backbone, Consumer<String> body, Runnable done){
		worker = new WorkerDictionaryRead(backbone.getDictionaryFile(), backbone.getCharset(), body, done);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener){
		worker.addPropertyChangeListener(listener);
	}

	public void execute(){
		worker.execute();
	}

	public SwingWorker.StateValue getState(){
		return worker.getState();
	}

	public void cancel(){
		worker.cancel(true);
	}

	public boolean isDone(){
		return worker.isDone();
	}

}
