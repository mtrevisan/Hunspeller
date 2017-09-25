package unit731.hunspeller.collections.trie;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencer;


/**
 * An implementation of a compact Trie.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Radix_tree">Radix Tree</a>
 * @see <a href="http://www.csse.monash.edu.au/~lloyd/tildeAlgDS/Tree/PATRICIA">PATRICIA Trie</a>
 * @see <a href="https://github.com/ClickerMonkey/TrieHard">TrieHard</a>
 * 
 * @param <S>	The sequence type.
 * @param <T>	The data type.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Trie<S, T>{

	/** The matching logic used for retrieving values from a Trie or for determining the existence of values given an input/key sequence */
	public enum TrieMatch{
		/**
		 * An exact match requires the input sequence to be an exact match to the sequences stored in the Trie. If the sequence "meow" is
		 * stored in the Trie, then it can only match on "meow".
		 */
		EXACT,
		/**
		 * A start-with match requires the input sequence to be a superset of the sequences stored in the Trie. If the sequence "meow" is
		 * stored in the Trie, then it can match on "meow", "meowa", "meowab", etc.
		 */
		STARTS_WITH
	}


	private final TrieNode<S, T> root = TrieNode.makeRoot();
	private TrieSequencer<S> sequencer;


	public Trie(TrieSequencer<S> sequencer){
		this.sequencer = sequencer;
	}

	public void clear(){
		root.clear();
	}

	public boolean isEmpty(){
		return root.isEmpty();
	}

	/**
	 * Puts the value in the Trie with the given sequence.
	 *
	 * @param sequence	Sequence with which the specified value is to be associated
	 * @param value		The value to place in the Trie
	 * @return	The previous value associated with <tt>sequence</tt>, or <tt>null</tt> if there was no mapping for <tt>sequence</tt>.
	 *		(A <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with <tt>sequence</tt>)
	 * @throws NullPointerException if the specified <tt>sequence</tt> or <tt>value</tt> is <tt>null</tt>
	 */
	public T put(S sequence, T value){
		Objects.requireNonNull(sequence);
		Objects.requireNonNull(value);

		T previousValue = null;
		int sequenceOffset = 0;
		int sequenceLength = sequencer.lengthOf(sequence);
		Object stem = sequencer.hashOf(sequence, sequenceOffset);
		TrieNode<S, T> node = root.getChild(stem);
		if(node == null){
			node = new TrieNode<>(sequence, sequenceOffset, sequenceLength, value);
			root.addChild(stem, node);
		}
		else
			while(node != null){
				int nodeLength = node.getEndIndex() - node.getStartIndex();
				int max = Math.min(nodeLength, sequenceLength - sequenceOffset);
				int matches = sequencer.matches(node.getSequence(), node.getStartIndex(), sequence, sequenceOffset, max);

				sequenceOffset += matches;

				//mismatch in current node
				if(matches != max){
					node.split(matches, node.getValue(), sequencer);

					createAndAttachNode(sequence, sequenceOffset, sequenceLength, value, node);

					break;
				}

				//partial match to the current node
				if(max < nodeLength){
					node.split(max, value, sequencer);
					node.setSequence(sequence);

					break;
				}

				//full match to sequence, replace value and sequence
				if(sequenceOffset == sequenceLength){
					node.setSequence(sequence);

					previousValue = node.setValue(value);
					break;
				}

				//full match, end of the query, or node
				if(!node.hasChildren()){
					createAndAttachNode(sequence, sequenceOffset, sequenceLength, value, node);

					break;
				}

				//full match, end of node
				stem = sequencer.hashOf(sequence, sequenceOffset);
				TrieNode<S, T> nextNode = node.getChild(stem);
				if(nextNode == null)
					createAndAttachNode(sequence, sequenceOffset, sequenceLength, value, node);

				//full match, query or node remaining
				node = nextNode;
			}
		return previousValue;
	}

	private void createAndAttachNode(S sequence, int startIndex, int endIndex, T value, TrieNode<S, T> parent){
		TrieNode<S, T> newNode = new TrieNode<>(sequence, startIndex, endIndex, value);

		Object stem = sequencer.hashOf(sequence, startIndex);
		parent.addChild(stem, newNode);
	}

	/**
	 * Gets the value that matches the given sequence.
	 *
	 * @param sequence	The sequence to match.
	 * @return	The value for the given sequence, or the default value of the Trie if no match was found. The default value of a Trie is by default
	 *		null.
	 */
	public T get(S sequence){
		TrieNode<S, T> node = searchAndApply(sequence, TrieMatch.EXACT, null);
		return (node != null? node.getValue(): null);
	}

	/**
	 * Removes the sequence from the Trie and returns it's value. The sequence must be an exact match, otherwise nothing will be removed.
	 *
	 * @param sequence	The sequence to remove.
	 * @return	The data of the removed sequence, or null if no sequence was removed.
	 */
	public T remove(S sequence){
		TrieNode<S, T> node = searchAndApply(sequence, TrieMatch.EXACT, (parent, stem) -> parent.removeChild(stem));
		return (node != null? node.getValue(): null);
	}

	public Collection<TrieNode<S, T>> collectPrefixes(S sequence){
		Collection<TrieNode<S, T>> prefixes = new ArrayList<>();
		searchAndApply(sequence, TrieMatch.STARTS_WITH, (parent, stem) -> {
			TrieNode<S, T> node = parent.getChild(stem);
			if(!prefixes.contains(node))
				prefixes.add(node);
		});
		return prefixes;
	}

	/**
	 * Searches in the Trie based on the sequence query.
	 *
	 * @param sequence	The query sequence.
	 * @param match		The matching logic.
	 * @param callback	The callback to be executed on match found.
	 * @return	The node that best matched the query based on the logic.
	 */
	private TrieNode<S, T> searchAndApply(S sequence, TrieMatch matchType, BiConsumer<TrieNode<S, T>, Object> callback){
		Objects.requireNonNull(sequence);

		int sequenceLength = sequencer.lengthOf(sequence);
		int sequenceOffset = root.getEndIndex();
		//if the sequence is empty, return null
		if(sequenceLength == 0 || sequenceLength < sequenceOffset)
			return null;

		Object stem = sequencer.hashOf(sequence, sequenceOffset);
		TrieNode<S, T> parent = root;
		TrieNode<S, T> node = root.getChild(stem);
		while(node != null){
			int nodeLength = node.getEndIndex() - node.getStartIndex();
			int max = Math.min(nodeLength, sequenceLength - sequenceOffset);
			int matches = sequencer.matches(node.getSequence(), node.getStartIndex(), sequence, sequenceOffset, max);

			sequenceOffset += matches;

			if(matches != max || matches == max && max != nodeLength)
				//not found
				node = null;
			else if(sequenceOffset == sequenceLength || !node.hasChildren()){
				if(callback != null && node.isLeaf() && sequencer.startsWith(sequence, node.getSequence()))
					callback.accept(parent, stem);

				break;
			}
			else{
				//call callback for each leaf node found so far
				if(callback != null && node.isLeaf() && sequencer.startsWith(sequence, node.getSequence()))
					callback.accept(parent, stem);

				stem = sequencer.hashOf(sequence, sequenceOffset);
				TrieNode<S, T> next = node.getChild(stem);

				//if there is no next, node could be a STARTS_WITH match
				if(next == null)
					break;

				parent = node;
				node = next;
			}
		}

		//EXACT matches
		if(node != null && matchType == TrieMatch.EXACT){
			//check length of last node against query
			int endIndex = node.getEndIndex();
			if(!node.isLeaf() || endIndex != sequenceLength)
				return null;

			//check actual sequence values
			S seq = node.getSequence();
			if(sequencer.lengthOf(seq) != sequenceLength || sequencer.matches(seq, 0, sequence, 0, endIndex) != sequenceLength)
				return null;
		}

		return node;
	}

	/**
	 * Search the given string and return an object if it lands on a sequence, essentially testing if the sequence exists in the trie.
	 *
	 * @param sequence	The sequence to search for
	 * @return Whether the sequence is fully contained into this trie
	 */
	public boolean containsKey(S sequence){
		return (get(sequence) != null);
	}

	/**
	 * Apply a function to each leaf, traversing the tree in level order.
	 * 
	 * @param callback	Function that will be executed for each leaf of the trie
	 */
	public void forEachLeaf(Consumer<TrieNode<S, T>> callback){
		Objects.requireNonNull(callback);

		find(root, node -> {
			if(node.isLeaf())
				callback.accept(node);
			return false;
		});
	}

	/**
	 * Apply a function to each node, traversing the tree in level order, until the callback responds <code>true</code>.
	 * 
	 * @param root			Node to start with
	 * @param callback	Function that will be executed for each node of the trie, it has to return <code>true</code> if a node matches
	 * @return	<code>true</code> if the node is found
	 */
	private boolean find(TrieNode<S, T> root, Function<TrieNode<S, T>, Boolean> callback){
		Objects.requireNonNull(callback);

		boolean found = false;
		Stack<TrieNode<S, T>> level = new Stack<>();
		level.push(root);
		while(!level.empty()){
			TrieNode<S, T> node = level.pop();

			node.forEachChild(level::push);

			if(callback.apply(node)){
				found = true;
				break;
			}
		}
		return found;
	}

}
