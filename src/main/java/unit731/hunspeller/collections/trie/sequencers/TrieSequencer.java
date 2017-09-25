package unit731.hunspeller.collections.trie.sequencers;


public interface TrieSequencer<T>{

	/**
	 * Calculates the length of the sequence.
	 * 
	 * @param sequence	The sequence.
	 * @return	The sequence length.
	 */
	int lengthOf(T sequence);

	/**
	 * Calculates the length of the sequence.
	 * 
	 * @param sequence	The sequence.
	 * @param prefix		The prefix the sequence should be compared to.
	 * @return	Whether the sequence starts with the given text.
	 */
	boolean startsWith(T sequence, T prefix);

	/**
	 * Calculates the true sequence.
	 * 
	 * @param sequence	The sequence.
	 * @param startIndex	The starting index.
	 * @param endIndex	The ending index.
	 * @return	The true sequence.
	 */
	T getTrueSequence(T sequence, int startIndex, int endIndex);

	/**
	 * Calculates the hash of the element at the given index in the given sequence. The hash is used as a key to quickly retrieve entries.
	 * Typical implementations based on characters return the ASCII value of the character, since it yields dense numerical values.
	 * The more dense the hashes returned (the smaller the difference between the minimum and maximum returnable hash means it's more dense),
	 * the less space that is wasted.
	 *
	 * @param sequence	The sequence.
	 * @param index		The index of the element to calculate the hash of.
	 * @return	The hash of the element in the sequence at the index.
	 */
	Object hashOf(T sequence, int index);

	/**
	 * Determines the maximum number of elements that match between sequences A and B where comparison starts at the given indices up to
	 * the given count.
	 *
	 * @param sequenceA	The first sequence to count matches on.
	 * @param indexA		The offset into the first sequence.
	 * @param sequenceB	The second sequence to count matches on.
	 * @param indexB		The offset into the second sequence.
	 * @param maxCount	The maximum number of matches to search for.
	 * @return	A number between <tt>0</tt> (inclusive) and <tt>maxCount</tt> (inclusive) that is the number of matches between the two sequence
	 *		sections.
	 */
	int matches(T sequenceA, int indexA, T sequenceB, int indexB, int maxCount);

}
