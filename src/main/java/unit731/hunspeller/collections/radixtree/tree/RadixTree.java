package unit731.hunspeller.collections.radixtree.tree;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiFunction;
import unit731.hunspeller.collections.radixtree.sequencers.SequencerInterface;


/**
 * A radix tree.
 * Radix trees are key-value mappings which allow quick lookups on the strings. Radix trees also make it easy to grab the values
 * with a common prefix.
 * If {@link #prepare() prepare} is called, an implementation of the Aho-Corasick string matching algorithm (described in the paper
 * "Efficient String Matching: An Aid to Bibliographic Search", written by Alfred V. Aho and Margaret J. Corasick, Bell Laboratories, 1975)
 * is created, allowing for fast dictionary matching.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Radix_tree">Wikipedia</a>
 * @see <a href="https://github.com/thegedge/radix-tree">Radix Tree 1</a>
 * @see <a href="https://github.com/oroszgy/radixtree">Radix Tree 2</a>
 * @see <a href="https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm">Aho-Corasick algorithm</a>
 * @see <a href="http://www.cs.uku.fi/~kilpelai/BSA05/lectures/slides04.pdf">Biosequence Algorithms, Spring 2005 - Lecture 4: Set Matching and Aho-Corasick Algorithm</a>
 * @see <a href="http://informatika.stei.itb.ac.id/~rinaldi.munir/Stmik/2014-2015/Makalah2015/Makalah_IF221_Strategi_Algoritma_2015_032.pdf">Aho-Corasick Algorithm in Pattern Matching</a>
 * @see <a href="http://www.webgraphviz.com/">WebGraphviz online</a>
 *
 * @param <S>	The sequence/key type
 * @param <V>	The type of values stored in the tree
 */
public class RadixTree<S, V extends Serializable> implements Map<S, V>{

	private class TraverseElement{

		protected final RadixTreeNode<S, V> node;
		protected final S prefix;

		TraverseElement(RadixTreeNode<S, V> node, S prefix){
			this.node = node;
			this.prefix = prefix;
		}

	}

	private class VisitElement extends TraverseElement{

		private final RadixTreeNode<S, V> parent;


		VisitElement(RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent, S prefix){
			super(node, prefix);

			this.parent = parent;
		}

	}


	/** The root node in this tree */
	protected RadixTreeNode<S, V> root;
	protected SequencerInterface<S> sequencer;
	protected boolean noDuplicatesAllowed;

	private boolean prepared;


	public static <K, T extends Serializable> RadixTree<K, T> createTree(SequencerInterface<K> sequencer){
		RadixTree<K, T> tree = new RadixTree<>();
		tree.root = RadixTreeNode.createEmptyNode(sequencer.getEmptySequence());
		tree.sequencer = sequencer;
		return tree;
	}

	public static <K, T extends Serializable> RadixTree<K, T> createTreeNoDuplicates(SequencerInterface<K> sequencer){
		RadixTree<K, T> tree = new RadixTree<>();
		tree.root = RadixTreeNode.createEmptyNode(sequencer.getEmptySequence());
		tree.sequencer = sequencer;
		tree.noDuplicatesAllowed = true;
		return tree;
	}

	protected RadixTree(){}

	/** Initializes the fail transitions of all nodes (except for the root). */
	public void prepare(){
		//process children of the root
		root.forEachChildren(child -> child.setFailNode(root));

		RadixTreeTraverser<S, V> traverser = new RadixTreeTraverser<S, V>(){
			@Override
			public void traverse(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				if(parent == root)
					return;

				S currentKey = node.getKey();
				int keySize = sequencer.length(currentKey);
				for(int i = 0; i < keySize; i ++){
					S subkey = sequencer.subSequence(currentKey, i);

					RadixTreeNode<S, V> state = findDeepestNode(subkey, parent);

					S stateKey = state.getKey();
					int lcpLength = longestCommonPrefixLength(subkey, stateKey);
					if(lcpLength > 0){
						int nodeKeyLength = sequencer.length(stateKey);
						if(lcpLength < nodeKeyLength)
							//split fail
							state.split(lcpLength, sequencer);
						if(lcpLength + i < keySize)
							//split node
							node.split(lcpLength + i, sequencer);

						//link fail to node
						node.setFailNode(state);

						//out(u) += out(f(u))
						node.addAdditionalValues(state);

						break;
					}
				}

				if(node.getFailNode() == null)
					node.setFailNode(root);
			}

			/** Find the deepest node labeled by a proper suffix of the current child */
			private RadixTreeNode<S, V> findDeepestNode(S subkey, RadixTreeNode<S, V> parent){
				RadixTreeNode<S, V> state;
				RadixTreeNode<S, V> fail = parent.getFailNode();
				while((state = transit(fail, subkey)) == null)
					fail = fail.getFailNode();
				return state;
			}

			private RadixTreeNode<S, V> transit(RadixTreeNode<S, V> node, S prefix){
				RadixTreeNode<S, V> result = root;
				Iterator<RadixTreeNode<S, V>> itr = node.iterator();
				while(itr.hasNext()){
					RadixTreeNode<S, V> child = itr.next();
					int lcpLength = longestCommonPrefixLength(child.getKey(), prefix);
					if(lcpLength > 0){
						result = child;
						break;
					}
				}
				return result;
			}
		};
		traverseBFS(traverser);

		prepared = true;
	}

	public boolean isAhoCorasickTree(){
		return prepared;
	}

	@Override
	public void clear(){
		root.clearChildren();
	}

	public void clearFailTransitions(){
		RadixTreeTraverser<S, V> traverser = (wholeKey, node, parent) -> {
			node.setFailNode(null);
			node.clearAdditionalValues();
		};
		traverseBFS(traverser);

		prepared = false;
	}

	@Override
	public boolean containsKey(Object keyToCheck){
		@SuppressWarnings("unchecked")
		RadixTreeNode<S, V> foundNode = findPrefixedBy((S)keyToCheck);
		return (foundNode != null);
	}

	@Override
	public boolean containsValue(Object value){
		Objects.requireNonNull(value);

		RadixTreeVisitor<S, V, Boolean> visitor = new RadixTreeVisitor<S, V, Boolean>(false){
			@Override
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				V v = node.getValue();
				result = (v == value || v.equals(value));

				return result;
			}
		};
		visitPrefixedBy(visitor);

		return visitor.getResult();
	}

	@Override
	public V get(Object keyToCheck){
		@SuppressWarnings("unchecked")
		RadixTreeNode<S, V> foundNode = findPrefixedBy((S)keyToCheck);
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
		Objects.requireNonNull(text);

		if(!prepared)
			throw new IllegalStateException("Cannot perform search until prepare() is called");

		Iterator<SearchResult<S, V>> itr = new Iterator<SearchResult<S, V>>(){

			private RadixTreeNode<S, V> lastMatchedNode = root;
			private int currentIndex = 0;


			@Override
			public boolean hasNext(){
				try{
					RadixTreeNode<S, V> node = lastMatchedNode;
					for(int i = currentIndex; i < sequencer.length(text); i ++){
						node = node.getNextNode(text, i, sequencer);
						if(node == null)
							node = root;
						else if(node.hasValue())
							return true;
						else
							i += sequencer.length(node.getKey()) - 1;
					}
				}
				catch(NoSuchElementException e){}
				return false;
			}

			@Override
			public SearchResult<S, V> next(){
				for(int i = currentIndex; i < sequencer.length(text); i ++){
					lastMatchedNode = lastMatchedNode.getNextNode(text, i, sequencer);
					if(lastMatchedNode == null)
						lastMatchedNode = root;
					else if(lastMatchedNode.hasValue()){
						currentIndex = i + 1;
						return new SearchResult<>(lastMatchedNode, i);
					}
					else
						i += sequencer.length(lastMatchedNode.getKey()) - 1;
				}

				throw new NoSuchElementException();
			}

			@Override
			public void remove(){
				throw new UnsupportedOperationException();
			}
		};
		return itr;
	}

	public RadixTreeNode<S, V> findPrefixedBy(S keyToCheck){
		RadixTreeVisitor<S, V, RadixTreeNode<S, V>> visitor = new RadixTreeVisitor<S, V, RadixTreeNode<S, V>>(null){
			@Override
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				if(sequencer.equals(wholeKey, keyToCheck))
					result = node;

				return (result != null);
			}
		};
		visitPrefixedBy(visitor, keyToCheck);

		return visitor.getResult();
	}

	/**
	 * Gets a list of entries whose associated keys have the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of values
	 * @throws NullPointerException	If the given prefix is <code>null</code>
	 */
	public List<Map.Entry<S, V>> getEntriesPrefixedBy(S prefix){
		Objects.requireNonNull(prefix);

		RadixTreeVisitor<S, V, List<Map.Entry<S, V>>> visitorEntries = new RadixTreeVisitor<S, V, List<Map.Entry<S, V>>>(new ArrayList<>()){
			@Override
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				V value = node.getValue();
				Map.Entry<S, V> entry = new AbstractMap.SimpleEntry<>(wholeKey, value);
				result.add(entry);

				return false;
			}
		};

		visitorEntries.getResult().clear();
		visitPrefixedBy(visitorEntries, prefix);

		return visitorEntries.getResult();
	}

	/**
	 * Gets a list of values whose associated keys have the given prefix, or are contained into the prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of values
	 * @throws NullPointerException	If the prefix is <code>null</code>
	 */
	public List<V> getValuesPrefixedBy(S prefix){
		List<V> values = new ArrayList<>();
		List<Map.Entry<S, V>> entries = getEntriesPrefixedBy(prefix);
		for(Map.Entry<S, V> entry : entries)
			values.add(entry.getValue());
		return values;
	}

	/**
	 * Gets a list of keys with the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of prefixes
	 * @throws NullPointerException	If prefix is <code>null</code>
	 */
	public List<S> getKeysPrefixedBy(S prefix){
		List<S> keys = new ArrayList<>();
		List<Map.Entry<S, V>> entries = getEntriesPrefixedBy(prefix);
		for(Map.Entry<S, V> entry : entries)
			keys.add(entry.getKey());
		return keys;
	}

	public RadixTreeNode<S, V> find(S keyToCheck){
		RadixTreeVisitor<S, V, RadixTreeNode<S, V>> visitor = new RadixTreeVisitor<S, V, RadixTreeNode<S, V>>(null){
			@Override
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				if(sequencer.equals(wholeKey, keyToCheck))
					result = node;

				return (result != null);
			}
		};
		visit(visitor, keyToCheck);

		return visitor.getResult();
	}

	/**
	 * Gets a list of entries whose associated keys are a prefix of the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of values
	 * @throws NullPointerException	If the given prefix is <code>null</code>
	 */
	public List<Map.Entry<S, V>> getEntries(S prefix){
		Objects.requireNonNull(prefix);

		RadixTreeVisitor<S, V, List<Map.Entry<S, V>>> visitorEntries = new RadixTreeVisitor<S, V, List<Map.Entry<S, V>>>(new ArrayList<>()){
			@Override
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				V value = node.getValue();
				Map.Entry<S, V> entry = new AbstractMap.SimpleEntry<>(wholeKey, value);
				result.add(entry);

				return false;
			}
		};

		visitorEntries.getResult().clear();
		visit(visitorEntries, prefix);

		return visitorEntries.getResult();
	}

	/**
	 * Gets a list of values whose associated keys are a prefix of the given prefix, or are contained into the prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of values
	 * @throws NullPointerException	If the prefix is <code>null</code>
	 */
	public List<V> getValues(S prefix){
		List<V> values = new ArrayList<>();
		List<Map.Entry<S, V>> entries = getEntries(prefix);
		for(Map.Entry<S, V> entry : entries)
			values.add(entry.getValue());
		return values;
	}

	/**
	 * Gets a list of keys that are a prefix of the given prefix.
	 *
	 * @param prefix	The prefix to look for
	 * @return	The list of prefixes
	 * @throws NullPointerException	If prefix is <code>null</code>
	 */
	public List<S> getKeys(S prefix){
		List<S> keys = new ArrayList<>();
		List<Map.Entry<S, V>> entries = getEntries(prefix);
		for(Map.Entry<S, V> entry : entries)
			keys.add(entry.getKey());
		return keys;
	}

	@Override
	public boolean isEmpty(){
		return root.isEmpty();
	}

	@Override
	public int size(){
		RadixTreeVisitor<S, V, Integer> visitor = new RadixTreeVisitor<S, V, Integer>(0){
			@Override
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				result ++;

				return false;
			}
		};
		visitPrefixedBy(visitor);

		return visitor.getResult();
	}

	@Override
	public Set<Map.Entry<S, V>> entrySet(){
		List<Map.Entry<S, V>> entries = getEntriesPrefixedBy(sequencer.getEmptySequence());
		return new HashSet<>(entries);
	}

	@Override
	public Set<S> keySet(){
		Set<S> keys = new HashSet<>();
		List<Map.Entry<S, V>> entries = getEntriesPrefixedBy(sequencer.getEmptySequence());
		for(Map.Entry<S, V> entry : entries)
			keys.add(entry.getKey());
		return keys;
	}

	@Override
	public Collection<V> values(){
		Set<V> values = new HashSet<>();
		List<Map.Entry<S, V>> entries = getEntriesPrefixedBy(sequencer.getEmptySequence());
		for(Map.Entry<S, V> entry : entries)
			values.add(entry.getValue());
		return values;
	}

	/**
	 * NOTE: Calling this method will un-{@link #prepare() prepare} the tree, that is, it will not be an Aho-Corasick tree anymore.
	 * 
	 * @param map	Map of key-value pair to add to the tree
	 * @throws NullPointerException	If the given map is <code>null</code>
	 * @throws DuplicateKeyException	If a duplicated key is inserted and the tree does not allow it
	 */
	@Override
	public void putAll(Map<? extends S, ? extends V> map){
		Objects.requireNonNull(map);

		if(prepared)
			clearFailTransitions();

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
	@Override
	public V put(S key, V value){
		Objects.requireNonNull(key);
		Objects.requireNonNull(value);

		if(prepared)
			clearFailTransitions();

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
		for(RadixTreeNode<S, V> child : node)
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
	private int longestCommonPrefixLength(S keyA, S keyB){
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
	@Override
	@SuppressWarnings("unchecked")
	public V remove(Object key){
		Objects.requireNonNull(key);

		if(prepared)
			clearFailTransitions();

		RadixTreeVisitor<S, V, V> visitor = new RadixTreeVisitor<S, V, V>(null){
			@Override
			public boolean visit(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
				if(sequencer.equals(wholeKey, (S)key)){
					result = node.getValue();

					removeNode(node, parent);
				}

				return (result != null);
			}
		};
		visitPrefixedBy(visitor, (S)key);

		return visitor.getResult();
	}

	private void removeNode(RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
		Collection<RadixTreeNode<S, V>> children = node.getChildren();

		//if there is no children of the node we need to delete it from the its parent children list
		if(children == null || children.isEmpty()){
			S key = node.getKey();
			Iterator<RadixTreeNode<S, V>> itr = parent.iterator();
			while(itr.hasNext())
				if(sequencer.equals(itr.next().getKey(), key)){
					itr.remove();
					break;
				}

			//if parent is not real node and has only one child then they need to be merged.
			Collection<RadixTreeNode<S, V>> parentChildren = parent.getChildren();
			if(parentChildren != null && parentChildren.size() == 1 && !parent.hasValue() && parent != root)
				parentChildren.iterator().next().mergeWithAncestor(parent, sequencer);
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
		Queue<TraverseElement> queue = new ArrayDeque<>();
		queue.add(new TraverseElement(root, root.getKey()));
		while(!queue.isEmpty()){
			TraverseElement elem = queue.remove();
			RadixTreeNode<S, V> parent = elem.node;
			S prefix = elem.prefix;

			for(RadixTreeNode<S, V> child : parent){
				S wholeSequence = sequencer.concat(prefix, child.getKey());
				traverser.traverse(wholeSequence, child, parent);

				queue.add(new TraverseElement(child, wholeSequence));
			}
		}
	}

	/**
	 * Traverses this radix tree using the given visitor.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 */
	public void visitPrefixedBy(RadixTreeVisitor<S, V, ?> visitor){
		visitPrefixedBy(visitor, sequencer.getEmptySequence());
	}

	/**
	 * Traverses this radix tree using the given visitor and starting at the given prefix.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 * @param prefixAllowed	The prefix used to restrict visitation
	 */
	public void visitPrefixedBy(RadixTreeVisitor<S, V, ?> visitor, S prefixAllowed){
		Objects.requireNonNull(visitor);
		Objects.requireNonNull(prefixAllowed);

		BiFunction<S, S, Boolean> condition = (prefix, preAllowed) -> sequencer.startsWith(prefix, preAllowed);
		visit(visitor, prefixAllowed, condition);
	}

	/**
	 * Traverses this radix tree using the given visitor.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 */
	public void visit(RadixTreeVisitor<S, V, ?> visitor){
		visit(visitor, sequencer.getEmptySequence());
	}

	/**
	 * Traverses this radix tree using the given visitor and starting at the given prefix.
	 * Note that the tree will be traversed in lexicographical order.
	 *
	 * @param visitor	The visitor
	 * @param prefixAllowed	The prefix used to restrict visitation
	 */
	public void visit(RadixTreeVisitor<S, V, ?> visitor, S prefixAllowed){
		Objects.requireNonNull(visitor);
		Objects.requireNonNull(prefixAllowed);

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
	private void visit(RadixTreeVisitor<S, V, ?> visitor, S prefixAllowed, BiFunction<S, S, Boolean> condition){
		Stack<VisitElement> stack = new Stack<>();
		stack.push(new VisitElement(root, null, root.getKey()));
		while(!stack.isEmpty()){
			VisitElement elem = stack.pop();
			RadixTreeNode<S, V> node = elem.node;
			S prefix = elem.prefix;

			if(node.hasValue() && condition.apply(prefix, prefixAllowed)){
				boolean stop = visitor.visit(prefix, node, elem.parent);
				if(stop)
					break;
			}

			int prefixLen = sequencer.length(prefix);
			for(RadixTreeNode<S, V> child : node){
				S newPrefix = sequencer.concat(prefix, child.getKey());
				if(prefixLen >= sequencer.length(prefixAllowed) || sequencer.equalsAtIndex(newPrefix, prefixAllowed, prefixLen))
					stack.push(new VisitElement(child, node, newPrefix));
			}
		}
	}

}
