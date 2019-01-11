package unit731.hunspeller.parsers.dictionary.workers.core;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.util.function.BiConsumer;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.services.TimeWatch;


public abstract class WorkerBase<S, T> extends SwingWorker<Void, Void>{

	protected WorkerData workerData;

	protected BiConsumer<S, T> readLineProcessor;
	protected BiConsumer<BufferedWriter, Pair<Integer, S>> writeLineProcessor;

	protected TimeWatch watch = TimeWatch.start();


	public String getWorkerName(){
		return workerData.workerName;
	}

	protected File getDicFile(){
		return workerData.dicParser.getDicFile();
	}

	protected Charset getCharset(){
		return workerData.dicParser.getCharset();
	}

	protected Runnable getCompleted(){
		return workerData.completed;
	}

	protected Runnable getCancelled(){
		return workerData.cancelled;
	}

	protected boolean isParallelProcessing(){
		return workerData.parallelProcessing;
	}

	protected boolean isPreventExceptionRelaunch(){
		return workerData.preventExceptionRelaunch;
	}

}
