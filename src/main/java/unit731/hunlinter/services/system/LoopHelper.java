package unit731.hunlinter.services.system;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class LoopHelper{

	private LoopHelper(){}


	public static <T> void forEach(final T[] array, final Consumer<T> fun){
		final int size = (array != null? array.length: 0);
		for(int i = 0; i < size; i++)
			fun.accept(array[i]);
	}

	public static <T> void forEach(final Collection<T> collection, final Consumer<T> fun){
		if(collection != null)
			for(final T elem : collection)
				fun.accept(elem);
	}


	public static <T> boolean anyMatch(final T[] array, final Predicate<T> condition){
		final int size = (array != null? array.length: 0);
		for(int i = 0; i < size; i++)
			if(condition.test(array[i]))
				return true;
		return false;
	}

	public static <T> boolean anyMatch(final Collection<T> collection, final Predicate<T> condition){
		if(collection != null)
			for(final T elem : collection)
				if(condition.test(elem))
					return true;
		return false;
	}


	public static <T> void applyIf(final T[] array, final Predicate<T> condition, final Consumer<T> fun){
		final int size = (array != null? array.length: 0);
		for(int i = 0; i < size; i ++){
			final T elem = array[i];
			if(condition.test(elem))
				fun.accept(elem);
		}
	}

	public static <T> void applyIf(final Collection<T> collection, final Predicate<T> condition, final Consumer<T> fun){
		if(collection != null)
			for(final T elem : collection)
				if(condition.test(elem))
					fun.accept(elem);
	}


	public static <T> T[] collectIf(final T[] array, final Predicate<T> condition, final Supplier<T[]> creator){
		T[] collect = creator.get();
		final int size = (array != null? array.length: 0);
		for(int i = 0; i < size; i++){
			final T elem = array[i];
			if(condition.test(elem))
				collect = ArrayUtils.add(collect, elem);
		}
		return collect;
	}


	public static <T> T[] removeIf(final T[] array, final Predicate<T> filter){
		Objects.requireNonNull(array);
		Objects.requireNonNull(filter);

		int index = indexOf(array, filter, 0);
		if(index == -1)
			return ArrayUtils.clone(array);

		final int[] indices = new int[array.length - index];
		indices[0] = index;

		int count;
		for(count = 1; (index = indexOf(array, filter, indices[count - 1] + 1)) != -1; indices[count ++] = index){}

		return ArrayUtils.removeAll(array, Arrays.copyOf(indices, count));
	}

	private static <T> int indexOf(final T[] array, final Predicate<T> filter, int startIndex){
		for(int i = startIndex; i < array.length; i ++)
			if(filter.test(array[i]))
				return i;
		return -1;
	}

}