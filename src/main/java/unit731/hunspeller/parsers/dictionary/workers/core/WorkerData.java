package unit731.hunspeller.parsers.dictionary.workers.core;

import java.util.Objects;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;


public class WorkerData{

	final String workerName;
	final DictionaryParser dicParser;
	Runnable completed;
	Runnable cancelled;

	final boolean parallelProcessing;
	final boolean preventExceptionRelaunch;


	public static final WorkerData create(final String workerName, final DictionaryParser dicParser){
		return new WorkerData(workerName, dicParser, false, false);
	}

	public static final WorkerData createPreventExceptionRelaunch(final String workerName, final DictionaryParser dicParser){
		return new WorkerData(workerName, dicParser, false, true);
	}

	public static final WorkerData createParallel(final String workerName, final DictionaryParser dicParser){
		return new WorkerData(workerName, dicParser, true, false);
	}

	public static final WorkerData createParallelPreventExceptionRelaunch(final String workerName, final DictionaryParser dicParser){
		return new WorkerData(workerName, dicParser, true, true);
	}

	private WorkerData(final String workerName, final DictionaryParser dicParser, final boolean parallelProcessing,
			final boolean preventExceptionRelaunch){
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicParser);

		this.workerName = workerName;
		this.dicParser = dicParser;
		this.parallelProcessing = parallelProcessing;
		this.preventExceptionRelaunch = preventExceptionRelaunch;
	}

	public void setCompletedCallback(final Runnable completed){
		Objects.requireNonNull(completed);

		this.completed = completed;
	}

	public void setCancelledCallback(final Runnable cancelled){
		Objects.requireNonNull(cancelled);

		this.cancelled = cancelled;
	}

	public void validate() throws NullPointerException{
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(dicParser.getDicFile());
		Objects.requireNonNull(dicParser.getCharset());
	}

}
