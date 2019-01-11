package unit731.hunspeller.parsers.dictionary.workers.core;

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.services.TimeWatch;


public abstract class WorkerBase<S, T> extends SwingWorker<Void, Void>{

	protected String workerName;

	protected Charset charset;

	protected BiConsumer<S, T> readLineProcessor;
	protected BiConsumer<BufferedWriter, Pair<Integer, S>> writeLineProcessor;
	protected Runnable completed;
	protected Runnable cancelled;

	protected TimeWatch watch = TimeWatch.start();


	public String getWorkerName(){
		return workerName;
	}

}
