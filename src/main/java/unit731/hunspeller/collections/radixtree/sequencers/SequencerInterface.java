package unit731.hunspeller.collections.radixtree.sequencers;


public interface SequencerInterface<S>{

	S getEmptySequence();

	int length(S sequence);

	/**
	 * Tests if this sequence starts with the specified prefix.
	 *
	 * @param sequence	The sequence.
	 * @param prefix	The prefix.
	 * @return	{@code true} if the sequence represented by the argument is a prefix of the sequence represented by this sequence; {@code false} otherwise.
	 *				Note also that {@code true} will be returned if the argument is an empty sequence or is equal to this {@code RadixTreeKey} object as
	 *				determined by the {@link #equals} method.
	 */
	boolean startsWith(S sequence, S prefix);

	/**
	 * Tests if this sequence ends with the specified prefix.
	 *
	 * @param sequence	The sequence.
	 * @param suffix	The suffix.
	 * @return	{@code true} if the sequence represented by the argument is a suffix of the sequence represented by this sequence; {@code false} otherwise.
	 *				Note also that {@code true} will be returned if the argument is an empty sequence or is equal to this {@code RadixTreeKey} object as
	 *				determined by the {@link #equals} method.
	 */
	boolean endsWith(S sequence, S suffix);

	boolean equals(S sequenceA, S sequenceB);

	default boolean equalsAtIndex(S sequenceA, S sequenceB, int index){
		return equalsAtIndex(sequenceA, sequenceB, index, index);
	}

	boolean equalsAtIndex(S sequenceA, S sequenceB, int indexA, int indexB);

	/**
	 * Returns a sequence that is a subsequence of this sequence.
	 * The subsequence begins at the specified {@code beginIndex} and extends to the end of the sequence.
	 *
	 * @param sequence	The sequence.
	 * @param index	The index.
	 * @return	The specified `character` at index `index`.
	 * @exception IndexOutOfBoundsException	If the {@code beginIndex} is negative, or the end of the sequence is larger than the length of
	 *														this sequence, or {@code beginIndex} is larger than the length of the sequence.
	 */
	default S characterAt(S sequence, int index){
		return subSequence(sequence, index, index + 1);
	}

	/**
	 * Returns a sequence that is a subsequence of this sequence.
	 * The subsequence begins at the specified {@code beginIndex} and extends to the end of the sequence.
	 *
	 * @param sequence	The sequence.
	 * @param beginIndex	The beginning index, inclusive.
	 * @return	The specified substring.
	 * @exception IndexOutOfBoundsException	If the {@code beginIndex} is negative, or the end of the sequence is larger than the length of
	 *														this sequence, or {@code beginIndex} is larger than the length of the sequence.
	 */
	default S subSequence(S sequence, int beginIndex){
		return (beginIndex > 0? subSequence(sequence, beginIndex, length(sequence)): sequence);
	}

	/**
	 * Returns a sequence that is a subsequence of this sequence.
	 * The subsequence begins at the specified {@code beginIndex} and extends to the character at index {@code endIndex - 1}.
	 *
	 * @param sequence	The sequence.
	 * @param beginIndex	The beginning index, inclusive.
	 * @param endIndex	The ending index, exclusive.
	 * @return	The specified substring.
	 * @exception IndexOutOfBoundsException	If the {@code beginIndex} is negative, or {@code endIndex} is larger than the length of
	 *														this sequence, or {@code beginIndex} is larger than {@code endIndex}.
	 */
	S subSequence(S sequence, int beginIndex, int endIndex);

	S concat(S sequence, S other);

	String toString(S sequence);

}
