package unit731.hunspeller.services;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;


/**
 * The hamming distance between two strings of equal length is the number of positions at which the corresponding symbols are different.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Hamming_distance">Hamming distance</a>
 */
public class HammingDistance{

	private static final MessageFormat DIFFERENT_LENGTHS = new MessageFormat("Strings ''{0}'' and ''{1}'' must have the same length");


	private HammingDistance(){}

	/**
	 * Find the Hamming Distance between two strings with the same length.
	 *
	 * <p>The distance starts with zero, and for each occurrence of a different character in either String, it increments the distance
	 * by 1, and finally return its value.</p>
	 *
	 * <p>Since the Hamming Distance can only be calculated between strings of equal length, input of different lengths
	 * will throw IllegalArgumentException</p>
	 *
	 * <pre>
	 * distance.apply("", "")               = 0
	 * distance.apply("pappa", "pappa")     = 0
	 * distance.apply("1011101", "1011111") = 1
	 * distance.apply("ATCG", "ACCC")       = 2
	 * distance.apply("karolin", "kerstin"  = 3
	 * </pre>
	 *
	 * @param left	the first CharSequence, must not be <code>null</code>
	 * @param right	the second CharSequence, must not be <code>null</code>
	 * @return	the hamming distance between the given strings
	 * @throws IllegalArgumentException	if either input is <code>null</code> or if they do not have the same length
	 */
	public static int getDistance(CharSequence left, CharSequence right){
		Objects.requireNonNull(left);
		Objects.requireNonNull(right);
		if(left.length() != right.length())
			throw new IllegalArgumentException(DIFFERENT_LENGTHS.format(new Object[]{left, right}));

		return (int)IntStream.range(0, left.length())
			.filter(idx -> left.charAt(idx) != right.charAt(idx))
			.count();
	}

	public static Pair<Character, Character> findFirstDifference(CharSequence left, CharSequence right){
		return findFirstDifference(left, right, 0);
	}

	public static Pair<Character, Character> findFirstDifference(CharSequence left, CharSequence right, int offset){
		Objects.requireNonNull(left);
		Objects.requireNonNull(right);
		if(left.length() != right.length())
			throw new IllegalArgumentException(DIFFERENT_LENGTHS.format(new Object[0]));

		boolean found = false;
		char chrLeft = 0;
		char chrRight = 0;
		for(int i = offset; i < left.length(); i ++){
			chrLeft = left.charAt(i);
			chrRight = right.charAt(i);
			if(chrLeft != chrRight){
				found = true;
				break;
			}
		}
		return (found? Pair.of(chrLeft, chrRight): null);
	}

}
