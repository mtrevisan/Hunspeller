/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.workers.core;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;
import java.util.function.Consumer;


public class WorkerData{

	private final String workerName;

	private boolean parallelProcessing;
	private boolean cancelOnException;

	private Runnable completed;
	private Consumer<Exception> cancelled;


	WorkerData(final String workerName){
		Objects.requireNonNull(workerName);

		this.workerName = workerName;
	}

	public final String getWorkerName(){
		return workerName;
	}

	public final WorkerData withSequentialProcessing(){
		parallelProcessing = false;
		return this;
	}

	public final WorkerData withParallelProcessing(){
		parallelProcessing = true;
		return this;
	}

	public final WorkerData withCancelOnException(){
		cancelOnException = true;
		return this;
	}

	public final WorkerData withDataCompletedCallback(final Runnable completed){
		this.completed = completed;
		return this;
	}

	public final WorkerData withDataCancelledCallback(final Consumer<Exception> cancelled){
		this.cancelled = cancelled;
		return this;
	}

	final void callCompletedCallback(){
		if(completed != null)
			completed.run();
	}

	final void callCancelledCallback(final Exception exception){
		if(cancelled != null)
			cancelled.accept(exception);
	}

	final boolean isParallelProcessing(){
		return parallelProcessing;
	}

	final boolean isCancelOnException(){
		return cancelOnException;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final WorkerData rhs = (WorkerData)obj;
		return new EqualsBuilder()
			.append(workerName, rhs.workerName)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(workerName)
			.toHashCode();
	}

}
