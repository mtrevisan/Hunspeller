package unit731.hunspeller.collections.trie;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencer;


/**
 * @param <S>	The sequence type.
 * @param <T>	The data type.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = {"sequence", "startIndex", "endIndex"})
public class TrieNode<S, T>{

	@Getter @Setter private S sequence;
	@Getter private int startIndex;
	@Getter private int endIndex;

	@Getter private T value;
	private Map<Object, TrieNode<S, T>> children;


	public TrieNode(S sequence, int startIndex, int endIndex, T value){
		this.sequence = sequence;
		this.startIndex = startIndex;
		this.endIndex = endIndex;

		this.value = value;
	}

	public static <S, T> TrieNode<S, T> makeRoot(){
		return new TrieNode<>();
	}

	public void clear(){
		sequence = null;
		startIndex = 0;
		endIndex = 0;

		value = null;
		children = null;
	}

	public T setValue(T value){
		T previousValue = this.value;
		this.value = value;
		return previousValue;
	}

	public boolean isLeaf(){
		return (value != null);
	}

	public TrieNode<S, T> getChild(Object stem){
		return Optional.ofNullable(children)
			.map(c -> c.get(stem))
			.orElse(null);
	}

	public void addChild(Object stem, TrieNode<S, T> node){
		children = Optional.ofNullable(children)
			.orElseGet(HashMap::new);
		children.put(stem, node);
	}

	public TrieNode<S, T> removeChild(Object stem){
		TrieNode<S, T> removedNode = null;
		if(children != null){
			TrieNode<S, T> node = children.get(stem);
			//when there are no children, remove this node from it's parent
			if(node != null && node.children == null){
				removedNode = children.remove(stem);

				if(children.isEmpty())
					//claim memory
					children = null;
			}
		}
		return removedNode;
	}

	public void forEachChild(Consumer<TrieNode<S, T>> callback){
		if(children != null)
			children.values()
				.forEach(callback::accept);
	}

	public boolean isEmpty(){
		return (value == null && !hasChildren());
	}

	public boolean hasChildren(){
		return (children != null);
	}

	/**
	 * Splits this node at the given relative index and returns the TrieNode with
	 * the sequence starting at index. The returned TrieNode has this node's
	 * sequence, value, and children. The returned TrieNode is also the only
	 * child of this node when this method returns.
	 *
	 * @param index	The relative index (starting at 0 and going to end - start - 1) in the sequence.
	 * @param value	The new value of this node.
	 * @param sequencer	The sequencer to use to determine the place of the node in the children's list
	 * @return	The reference to the child node created that's sequence starts at index.
	 */
	public TrieNode<S, T> split(int index, T value, TrieSequencer<S> sequencer){
		TrieNode<S, T> lowerNode = new TrieNode<>(sequence, startIndex + index, endIndex, value);
		lowerNode.children = children;
		children = null;
		endIndex = startIndex + index;

		Object stem = sequencer.hashOf(sequence, endIndex);
		addChild(stem, lowerNode);

		return lowerNode;
	}

}
