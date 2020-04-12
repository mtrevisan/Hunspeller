package unit731.hunlinter.services.datastructures;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.lang.reflect.Array;
import java.util.Iterator;


public class FixedArray<T> implements Iterable<T>{

	public T[] data;
	public int limit;


	public FixedArray(final Class<T> cl){
		this(cl, 0);
	}

	public FixedArray(final Class<T> cl, final int capacity){
		data = (T[])Array.newInstance(cl, capacity);
	}

	public synchronized void add(final T elem){
		data[limit ++] = elem;
	}

	public synchronized void addAll(final T[] array){
		addAll(array, array.length);
	}

	public synchronized void addAll(final FixedArray<T> array){
		addAll(array.data, array.limit);
	}

	private void addAll(final T[] array, final int size){
		System.arraycopy(array, 0, data, limit, size);
		limit += size;
	}

	public synchronized void addAllUnique(final T[] array){
		addAllUnique(array, array.length);
	}

	public synchronized void addAllUnique(final FixedArray<T> array){
		addAllUnique(array.data, array.limit);
	}

	private void addAllUnique(final T[] array, final int size){
		for(int i = 0; i < size; i ++)
			if(!contains(array[i]))
				data[limit ++] = array[i];
	}

	public boolean contains(final T elem){
		return (indexOf(elem) >= 0);
	}

	public synchronized void remove(final T elem){
		int index = limit;
		while(limit > 0 && (index = lastIndexOf(elem, index)) >= 0){
			final int delta = limit - index - 1;
			if(delta > 0)
				System.arraycopy(data, index + 1, data, index, delta);
			data[-- limit] = null;
		}
	}

	@Override
	public synchronized Iterator<T> iterator(){
		return new Iterator<T>(){
			private int idx = 0;

			public boolean hasNext(){
				return (idx < limit);
			}

			public T next(){
				return data[idx ++];
			}
		};
	}

	public int indexOf(final T elem){
		return indexOf(elem, 0);
	}

	public synchronized int indexOf(final T elem, final int startIndex){
		if(elem == null){
			for(int i = startIndex; i < data.length; i ++)
				if(data[i] == null)
					return i;
		}
		else{
			for(int i = startIndex; i < data.length; i ++)
				if(elem.equals(data[i]))
					return i;
		}
		return -1;
	}

	public synchronized int lastIndexOf(final T elem, final int startIndex){
		if(elem == null){
			for(int i = startIndex - 1; i >= 0; i --)
				if(data[i] == null)
					return i;
		}
		else{
			for(int i = startIndex - 1; i >= 0; i --)
				if(elem.equals(data[i]))
					return i;
		}
		return -1;
	}

	private Class<?> getDataType(){
		return data.getClass().getComponentType();
	}

	public synchronized boolean isEmpty(){
		return (limit == 0);
	}

	/** NOTE: this method should NOT be called at all because it is inefficient */
	public synchronized T[] extractCopyOrNull(){
		if(isEmpty())
			return null;

		final Class<?> type = getDataType();
		final T[] reducedData = (T[])Array.newInstance(type, limit);
		System.arraycopy(data, 0, reducedData, 0, limit);
		return reducedData;
	}

	public synchronized void reset(){
		limit = 0;
	}

	public synchronized void clear(){
		data = null;
		limit = -1;
	}

	@Override
	public synchronized boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final FixedArray<?> rhs = (FixedArray<?>)obj;
		return new EqualsBuilder()
			.append(data, rhs.data)
			.append(limit, rhs.limit)
			.isEquals();
	}

	@Override
	public synchronized int hashCode(){
		return new HashCodeBuilder()
			.append(data)
			.append(limit)
			.toHashCode();
	}

}
