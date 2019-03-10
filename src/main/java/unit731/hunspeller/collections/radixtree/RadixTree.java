package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import unit731.hunspeller.collections.radixtree.exceptions.DuplicateKeyException;
import unit731.hunspeller.collections.radixtree.dtos.SearchResult;
import unit731.hunspeller.collections.radixtree.dtos.TraverseElement;
import unit731.hunspeller.collections.radixtree.dtos.VisitElement;
import unit731.hunspeller.collections.radixtree.sequencers.SequencerInterface;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeTraverser;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeNode;


/**
 * A radix tree.
 * Radix trees are key-value mappings which allow quick lookups on the strings. Radix trees also make it easy to grab the values
 * with a common prefix.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Radix_tree">Wikipedia</a>
 * @see <a href="https://github.com/thegedge/radix-tree">Radix Tree 1</a>
 * @see <a href="https://github.com/oroszgy/radixtree">Radix Tree 2</a>
 *
 * @param <S>	The sequence/key type
 * @param <V>	The type of values stored in the tree
 */
public class RadixTree<S, V extends Serializable>{

	/** The root node in this tree */
	protected RadixTreeNode<S, V> root;
	protected SequencerInterface<S> sequencer;
	protected boolean noDuplicatesAllowed;


	public static <K, T extends Serializable> RadixTree<K, T> createTree(SequencerInterface<K> sequencer){
		return new RadixTree<>(sequencer, false);
	}

	public static <K, T extends Serializable> RadixTree<K, T> createTreeNoDuplicates(SequencerInterface<K> sequencer){
		return new RadixTree<>(sequencer, true);
	}

	protected RadixTree(SequencerInterface<S> sequencer, boolean noDuplicatesAllowed){
		root = RadixTreeNode.createEmptyNode(sequencer.getEmptySequence());
		this.sequencer = sequencer;
		this.noDuplicatesAllowed = noDuplicatesAllowed;
	}

	public void prepare(){
		throw new UnsupportedOperationException("Cannot prepare tree in a non-Aho-Corasick tree");
	}

	public void clear(){
		root.clearChildren();
	}

	public boolean containsKeyPrefixedBy(S keyToCheck){
		RadixTreeNode<S, V> foundNode = findPrefixedBy(keyToCheck);
		return (foundNode != null);
	}

	public boolean containsValuePrefixedBy(S value){
		Objects.requireNonNull(value);

		AtomicBoolean result = new AtomicBoolean();
		Function<VisitElement<S, V>, Boolean> visitor = elem -> {
			V v = elem.getNode().getValue();
			result.set(v == value || v.equals(value));

			return result.get();
		};
		visitPrefixedBy(visitor);

		return result.get();
	}

	public V getPrefixedBy(S keyToCheck){
		RadixTreeNode<S, V> foundNode = findPrefixedBy(keyToCheck);
		return (foundNode != null? foundNode.getValue(): null);
	}

	/**
	 * Perform a search and return all the entries that are contained into the given text.
	 * 
	 * @param text	The text to search into
	 * @return	The iterator of all the entries found inside the given text
	 * @throws NullPointerException	If the given text is <code>null</code>
	 */
	public Iterator<SearchResult<S, V>> search(S text){
		throw new UnsupportedOperationException("Cannot perform search in a non-Aho-Corasick tree");
	}

	public RadixTreeNode<S, V> findPrefixedBy(S keyToCheck){
		return find(keyToCheck, this::visitPrefixedBy);
	}

	/**
	 * Gets a list of values whose associated keys have the given prefix, or are contained into the prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of values
	 * @throws NullPointerException	If the prefix is <code>null</code>
	 */
	public List<V> getValuesPrefixedBy(S prefix){
		return getEntriesPrefixedBy(prefix).stream()
			.map(VisitElement::getNode)
			.map(RadixTreeNode::getValue)
			.collect(Collectors.toList());
	}

	/**
	 * Gets a list of keys with the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of prefixes
	 * @throws NullPointerException	If prefix is <code>null</code>
	 */
	public List<S> getKeysPrefixedBy(S prefix){
		return getEntriesPrefixedBy(prefix).stream()
			.map(VisitElement::getPrefix)
			.collect(Collectors.toList());
	}

	/**
	 * Gets a list of entries whose associated keys have the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of values
	 * @throws NullPointerException	If the given prefix is <code>null</code>
	 */
	public List<VisitElement<S, V>> getEntriesPrefixedBy(S prefix){
		Objects.requireNonNull(prefix);

		List<VisitElement<S, V>> result = new ArrayList<>();
		Function<VisitElement<S, V>, Boolean> visitorEntries = elem -> {
			result.add(elem);

			return false;
		};

		visitPrefixedBy(visitorEntries, prefix);

		return result;
	}

	public RadixTreeNode<S, V> findPrefixedTo(S keyToCheck){
		return find(keyToCheck, this::visitPrefixedTo);
	}

	private RadixTreeNode<S, V> find(S keyToCheck, BiConsumer<Function<VisitElement<S, V>, Boolean>, S> visit){
		AtomicReference<RadixTreeNode<S, V>> result = new AtomicReference<>(null);
		Function<VisitElement<S, V>, Boolean> visitor = elem -> {
			if(sequencer.equals(elem.getPrefix(), keyToCheck))
				result.set(elem.getNode());

			return (result.get() != null);
		};
		visit.accept(visitor, keyToCheck);

		return result.get();
	}

	/**
	 * Gets a list of values whose associated keys are a prefix of the given prefix, or are contained into the prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of values
	 * @throws NullPointerException	If the prefix is <code>null</code>
	 */
	public List<V> getValuesPrefixedTo(S prefix){
		List<V> values = new ArrayList<>();
		List<VisitElement<S, V>> entries = getEntriesPrefixedTo(prefix);
		for(VisitElement<S, V> entry : entries)
			values.add(entry.getNode().getValue());
		return values;
	}

	/**
	 * Gets a list of keys that are a prefix of the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of prefixes
	 * @throws NullPointerException	If prefix is <code>null</code>
	 */
	public List<S> getKeysPrefixedTo(S prefix){
		List<S> keys = new ArrayList<>();
		List<VisitElement<S, V>> entries = getEntriesPrefixedTo(prefix);
		for(VisitElement<S, V> entry : entries)
			keys.add(entry.getPrefix());
		return keys;
	}

	/**
	 * Gets a list of entries whose associated keys are a prefix of the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of values
	 * @throws NullPointerException	If the given prefix is <code>null</code>
	 */
	public List<VisitElement<S, V>> getEntriesPrefixedTo(S prefix){
		Objects.requireNonNull(prefix);

		List<VisitElement<S, V>> result = new ArrayList<>();
		Function<VisitElement<S, V>, Boolean> visitorEntries = elem -> {
			result.add(elem);

			return false;
		};

		RadixTree.this.visitPrefixedTo(visitorEntries, prefix);

		return result;
		//FIXME
//		Objects.requireNonNull(prefix);
//
//		List<VisitElement<S, V>> result = new ArrayList<>();
//
//		int prefixAllowedLength = sequencer.length(prefix);
//
//		Stack<VisitElement<S, V>> stack = new Stack<>();
//		stack.push(new VisitElement<>(root, null, root.getKey()));
//		while(!stack.isEmpty()){
//			VisitElement<S, V> elem = stack.pop();
//
//			if(elem.getNode().hasValue() && sequencer.startsWith(prefix, elem.getPrefix()))
//				result.add(elem);
//
//			Collection<RadixTreeNode<S, V>> children = elem.getNode().getChildren();
//			if(children != null){
//				int prefixLen = sequencer.length(elem.getPrefix());
//				for(RadixTreeNode<S, V> child : children)
//					if(prefixLen >= prefixAllowedLength || sequencer.equalsAtIndex(child.getKey(), prefix, 0, prefixLen)){
//						S newPrefix = sequencer.concat(elem.getPrefix(), child.getKey());
//						stack.push(new VisitElement<>(child, elem.getNode(), newPrefix));
//					}
//			}
//		}
//
//		return result;
	}

	public boolean isEmpty(){
		return root.isEmpty();
	}

	public int size(){
		AtomicInteger result = new AtomicInteger(0);
		Function<VisitElement<S, V>, Boolean> visitor = elem -> {
			result.incrementAndGet();

			return false;
		};
		visitPrefixedBy(visitor);

		return result.get();
	}

	public Set<Map.Entry<S, V>> entrySetPrefixedBy(){
		List<VisitElement<S, V>> entries = getEntriesPrefixedBy(sequencer.getEmptySequence());
		return entries.stream()
			.map(entry -> new AbstractMap.SimpleEntry<>(entry.getPrefix(), entry.getNode().getValue()))
			.collect(Collectors.toSet());
	}

	public Set<S> keySetPrefixedBy(){
		Set<S> keys = new HashSet<>();
		List<VisitElement<S, V>> entries = getEntriesPrefixedBy(sequencer.getEmptySequence());
		for(VisitElement<S, V> entry : entries)
			keys.add(entry.getPrefix());
		return keys;
	}

	public Collection<V> valuesPrefixedBy(){
		Set<V> values = new HashSet<>();
		List<VisitElement<S, V>> entries = getEntriesPrefixedBy(sequencer.getEmptySequence());
		for(VisitElement<S, V> entry : entries)
			values.add(entry.getNode().getValue());
		return values;
	}

	/**
	 * NOTE: Calling this method will un-{@link #prepare() prepare} the tree, that is, it will not be an Aho-Corasick tree anymore.
	 * 
	 * @param map	Map of key-value pair to add to the tree
	 * @throws NullPointerException	If the given map is <code>null</code>
	 * @throws DuplicateKeyException	If a duplicated key is inserted and the tree does not allow it
	 */
	public void putAll(Map<? extends S, ? extends V> map){
		Objects.requireNonNull(map);

		for(Map.Entry<? extends S, ? extends V> entry : map.entrySet())
			put(entry.getKey(), entry.getValue());
	}

	/**
	 * NOTE: Calling this method will un-{@link #prepare() prepare} the tree, that is, it will not be an Aho-Corasick tree anymore.
	 * 
	 * @param key	The key to add to the tree
	 * @param value	The value associated to the key
	 * @throws NullPointerException	If the given key or value is <code>null</code>
	 * @throws DuplicateKeyException	If a duplicated key is inserted and the tree does not allow it
	 */
	public V put(S key, V value){
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);

		try{
			V previousValue = put(key, value, root);

			return previousValue;
		}
		catch(DuplicateKeyException e){
			throw new DuplicateKeyException("Duplicate key: '" + sequencer.toString(key) + "'");
		}
	}

	/**
	 * Insert the value with the given key from the subtree rooted at the given node.
	 *
	 * @param key	The key
	 * @param value	The value
	 * @param node	The node to start searching from
	 * @return	The old value associated with the given key, or <code>null</code> if there was no mapping for <code>key</code>
	 * @throws DuplicateKeyException	If a duplicated key is inserted and the tree does not allow it
	 */
	private V put(S key, V value, RadixTreeNode<S, V> node){
		V ret = null;

		S nodeKey = node.getKey();
		int lcpLength = longestCommonPrefixLength(key, nodeKey);
		int keyLength = sequencer.length(key);
		int nodeKeyLength = sequencer.length(nodeKey);
		if(lcpLength == nodeKeyLength && lcpLength == keyLength)
			ret = putExactMatch(node, value);
		else if(lcpLength == 0 || lcpLength < keyLength && lcpLength >= nodeKeyLength)
			ret = putKeyLonger(key, lcpLength, keyLength, node, value);
		else if(lcpLength < nodeKeyLength)
			ret = putSplitNode(node, lcpLength, keyLength, value, key);
		else
			putAddChild(key, lcpLength, keyLength, value, node);

		return ret;
	}

	private V putExactMatch(RadixTreeNode<S, V> node, V value) throws DuplicateKeyException{
		//found a node with an exact match
		V ret = node.getValue();
		if(noDuplicatesAllowed && ret != null)
			throw new DuplicateKeyException();

		node.setValue(value);
		return ret;
	}

	private V putKeyLonger(S key, int lcpLength, int keyLength, RadixTreeNode<S, V> node, V value){
		V ret = null;
		//key is longer than the prefix located at this node, so we need to see if there's a child that can possibly share a prefix,
		//and if not, we just add a new node to this node
		S leftoverKey = sequencer.subSequence(key, lcpLength, keyLength);
		boolean found = false;
		Collection<RadixTreeNode<S, V>> children = node.getChildren();
		if(children != null)
			for(RadixTreeNode<S, V> child : children)
				if(sequencer.equalsAtIndex(child.getKey(), leftoverKey, 0)){
					found = true;
					ret = put(leftoverKey, value, child);
					break;
				}
		if(!found){
			//no child exists with any prefix of the given key, so add a new one
			RadixTreeNode<S, V> n = new RadixTreeNode<>(leftoverKey, value);
			node.addChild(n);
		}
		return ret;
	}

	private V putSplitNode(RadixTreeNode<S, V> node, int lcpLength, int keyLength, V value, S key){
		V ret = null;
		//key and node.getPrefix() share a prefix, so split node
		node.split(lcpLength, sequencer);
		if(lcpLength == keyLength){
			//the largest prefix is equal to the key, so set this node's value
			ret = node.getValue();
			node.setValue(value);
		}
		else{
			//there's a leftover suffix on the key, so add another child
			putAddChild(key, lcpLength, keyLength, value, node);
			node.setValue(null);
		}
		return ret;
	}

	private void putAddChild(S key, int lcpLength, int keyLength, V value, RadixTreeNode<S, V> node){
		//node.getPrefix() is a prefix of key, so add as child
		S leftoverKey = sequencer.subSequence(key, lcpLength, keyLength);
		RadixTreeNode<S, V> n = new RadixTreeNode<>(leftoverKey, value);
		node.addChild(n);
	}

	/**
	 * Finds the length of the Longest Common Prefix
	 *
	 * @param keyA	Character sequence A
	 * @param keyB	Character sequence B
	 * @return	The length of largest prefix of <code>A</code> and <code>B</code>
	 * @throws IllegalArgumentException	If either <code>A</code> or <code>B</code> is <code>null</code>
	 */
	protected int longestCommonPrefixLength(S keyA, S keyB){
		int len = 0;
		int size = Math.min(sequencer.length(keyA), sequencer.length(keyB));
		while(len < size){
			if(!sequencer.equalsAtIndex(keyA, keyB, len))
				break;

			len ++;
		}
		return len;
	}

	/**
	 * NOTE: Calling this method will un-{@link #prepare() prepare} the tree, that is, it will not be an Aho-Corasick tree anymore.
	 * 
	 * @param key	The key to remove from the tree
	 * @throws NullPointerException	If the given key is <code>null</code>
	 */
	public V removePrefixedBy(S key){
		Objects.requireNonNull(key);

		AtomicReference<V> result = new AtomicReference<>(null);
		Function<VisitElement<S, V>, Boolean> visitor = elem -> {
			if(sequencer.equals(elem.getPrefix(), key)){
				result.set(elem.getNode().getValue());

				removeNode(elem.getNode(), elem.getParent());
			}

			return (result.get() != null);
		};
		visitPrefixedBy(visitor, key);

		return result.get();
	}

	private void removeNode(RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
		Collection<RadixTreeNode<S, V>> children = node.getChildren();

		//if there is no children of the node we need to delete it from the its parent children list
		if(children == null){
			S key = node.getKey();
			Collection<RadixTreeNode<S, V>> parentChildren = parent.getChildren();
			if(parentChildren != null){
				Iterator<RadixTreeNode<S, V>> itr = parentChildren.iterator();
				while(itr.hasNext())
					if(sequencer.equals(itr.next().getKey(), key)){
						itr.remove();
						break;
					}

				//if parent is not real node and has only one child then they need to be merged.
				if(parentChildren.size() == 1 && !parent.hasValue() && parent != root)
					parentChildren.iterator().next().mergeWithAncestor(parent, sequencer);
			}
		}
		else if(children.size() == 1)
			//we need to merge the only child of this node with itself
			children.iterator().next().mergeWithAncestor(node, sequencer);
		else
			node.setValue(null);
	}


	/**
	 * Performa a BFS traversal on the tree, calling the traverser for each node found
	 *
	 * @param traverser	The traverser
	 */
	public void traverseBFS(RadixTreeTraverser<S, V> traverser){
		Queue<TraverseElement<S, V>> queue = new ArrayDeque<>();
		queue.add(new TraverseElement<>(root, root.getKey()));
		while(!queue.isEmpty()){
			TraverseElement<S, V> elem = queue.remove();
			RadixTreeNode<S, V> parent = elem.getNode();
			S prefix = elem.getPrefix();

			Collection<RadixTreeNode<S, V>> parentChildren = parent.getChildren();
			if(parentChildren != null)
				for(RadixTreeNode<S, V> child : parentChildren){
					S wholeSequence = sequencer.concat(prefix, child.getKey());
					traverser.traverse(wholeSequence, child, parent);

					queue.add(new TraverseElement<>(child, wholeSequence));
				}
		}
	}

	/**
	 * Traverses this radix tree using the given visitor.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 */
	public void visitPrefixedBy(Function<VisitElement<S, V>, Boolean> visitor){
		visitPrefixedBy(visitor, sequencer.getEmptySequence());
	}

	/**
	 * Traverses this radix tree using the given visitor and starting at the given prefix.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 * @param prefixAllowed	The prefix used to restrict visitation
	 */
	public void visitPrefixedBy(Function<VisitElement<S, V>, Boolean> visitor, S prefixAllowed){
		BiFunction<S, S, Boolean> condition = (prefix, preAllowed) -> sequencer.startsWith(prefix, preAllowed);
		visit(visitor, prefixAllowed, condition);
	}

	/**
	 * Traverses this radix tree using the given visitor.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 */
	public void visitPrefixedTo(Function<VisitElement<S, V>, Boolean> visitor){
		RadixTree.this.visitPrefixedTo(visitor, sequencer.getEmptySequence());
	}

	/**
	 * Traverses this radix tree using the given visitor and starting at the given prefix.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 * @param prefixAllowed	The prefix used to restrict visitation
	 */
	public void visitPrefixedTo(Function<VisitElement<S, V>, Boolean> visitor, S prefixAllowed){
		BiFunction<S, S, Boolean> condition = (prefix, preAllowed) -> sequencer.startsWith(preAllowed, prefix);
		visit(visitor, prefixAllowed, condition);
	}

	/**
	 * Traverses this radix tree using the given visitor and starting at the given prefix.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 * @param prefixAllowed	The prefix used to restrict visitation
	 * @param condition	Condition that has to be verified in order to match
	 * @throws NullPointerException	If the given visitor or prefix allowed is <code>null</code>
	 */
	private void visit(Function<VisitElement<S, V>, Boolean> visitor, S prefixAllowed, BiFunction<S, S, Boolean> condition){
		Objects.requireNonNull(visitor);
		Objects.requireNonNull(prefixAllowed);
		Objects.requireNonNull(condition);

		int prefixAllowedLength = sequencer.length(prefixAllowed);

		Stack<VisitElement<S, V>> stack = new Stack<>();
		stack.push(new VisitElement<>(root, null, root.getKey()));
		while(!stack.isEmpty()){
			VisitElement<S, V> elem = stack.pop();

			if(elem.getNode().hasValue() && condition.apply(elem.getPrefix(), prefixAllowed) && visitor.apply(elem))
				break;

			Collection<RadixTreeNode<S, V>> children = elem.getNode().getChildren();
			if(children != null){
				int prefixLen = sequencer.length(elem.getPrefix());
				for(RadixTreeNode<S, V> child : children)
					if(prefixLen >= prefixAllowedLength || sequencer.equalsAtIndex(child.getKey(), prefixAllowed, 0, prefixLen)){
						S newPrefix = sequencer.concat(elem.getPrefix(), child.getKey());
						stack.push(new VisitElement<>(child, elem.getNode(), newPrefix));
					}
			}
		}
	}

}
