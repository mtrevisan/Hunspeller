package unit731.hunspeller.collections.radixtree;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.collections.radixtree.dtos.SearchResult;
import unit731.hunspeller.collections.radixtree.exceptions.DuplicateKeyException;
import unit731.hunspeller.collections.radixtree.sequencers.StringSequencer;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeNode;


public class AhoCorasickStringRadixTreeTest{

	private final SecureRandom rng = new SecureRandom();


	@Test
	public void testEmptyTree(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertTrue(tree.isEmpty());
		Assertions.assertEquals(0, tree.size());
	}

	@Test
	public void testSingleInsertion(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);

		Assertions.assertEquals(1, tree.size());
		Assertions.assertTrue(tree.containsKeyPrefixedBy("test"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("tes"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("testt"));
	}

	@Test
	public void testMultipleInsertions(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);
		tree.put("rest", 4);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(1, tree.getPrefixedBy("test").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("tent").intValue());
		Assertions.assertEquals(3, tree.getPrefixedBy("tank").intValue());
		Assertions.assertEquals(4, tree.getPrefixedBy("rest").intValue());
	}

	@Test
	public void testPrepare(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tentest", 21);
		tree.put("tank", 3);
		tree.put("rest", 4);
		tree.prepare();

		Assertions.assertEquals(5, tree.size());
		Assertions.assertEquals(1, tree.getPrefixedBy("test").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("tent").intValue());
		Assertions.assertEquals(3, tree.getPrefixedBy("tank").intValue());
		Assertions.assertEquals(4, tree.getPrefixedBy("rest").intValue());

		Iterator<SearchResult<String, Integer>> itr = tree.search("resting in the test");
		SearchResult<String, Integer> search = itr.next();
		Assertions.assertEquals(0, search.getIndex());
		Assertions.assertEquals(4, search.getNode().getValue().intValue());
		search = itr.next();
		Assertions.assertEquals(17, search.getIndex());
		Assertions.assertEquals(1, search.getNode().getValue().intValue());
		Assertions.assertFalse(itr.hasNext());

		itr = tree.search("blah");
		Assertions.assertFalse(itr.hasNext());
	}

	@Test
	public void testMultipleInsertionOfTheSameKey(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);
		tree.put("rest", 4);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(1, tree.getPrefixedBy("test").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("tent").intValue());
		Assertions.assertEquals(3, tree.getPrefixedBy("tank").intValue());
		Assertions.assertEquals(4, tree.getPrefixedBy("rest").intValue());

		tree.put("test", 9);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(9, tree.getPrefixedBy("test").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("tent").intValue());
		Assertions.assertEquals(3, tree.getPrefixedBy("tank").intValue());
		Assertions.assertEquals(4, tree.getPrefixedBy("rest").intValue());
	}

	@Test
	public void testPrefixFetch(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("rest", 3);
		tree.put("tank", 4);

		Assertions.assertEquals(4, tree.size());
		assertEqualsWithSort(tree.getValuesPrefixedBy(StringUtils.EMPTY), new ArrayList<>(tree.valuesPrefixedBy()));
		assertEqualsWithSort(new Integer[]{1, 2, 4}, tree.getValuesPrefixedBy("t").toArray(new Integer[3]));
		assertEqualsWithSort(new Integer[]{1, 2}, tree.getValuesPrefixedBy("te").toArray(new Integer[2]));
		Assertions.assertArrayEquals(new Object[0], tree.getValuesPrefixedBy("asd").toArray());
	}

	@Test
	public void testSpook(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("pook", 1);
		tree.put("spook", 2);

		Assertions.assertEquals(2, tree.size());
		assertEqualsWithSort(tree.keySetPrefixedBy().toArray(new String[2]), new String[]{
			"pook",
			"spook"
		});
	}

	@Test
	public void testRemoval(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);

		Assertions.assertEquals(3, tree.size());
		Assertions.assertTrue(tree.containsKeyPrefixedBy("tent"));

		tree.removePrefixedBy("key");

		Assertions.assertEquals(3, tree.size());
		Assertions.assertTrue(tree.containsKeyPrefixedBy("tent"));

		tree.removePrefixedBy("tent");

		Assertions.assertEquals(2, tree.size());
		Assertions.assertEquals(1, tree.getPrefixedBy("test").intValue());
		Assertions.assertFalse(tree.containsKeyPrefixedBy("tent"));
		Assertions.assertEquals(3, tree.getPrefixedBy("tank").intValue());
	}

	@Test
	public void testManyInsertions(){
		RadixTree<String, BigInteger> tree = AhoCorasickTree.createTree(new StringSequencer());

		//n in [100, 500]
		int n = rng.nextInt(401) + 100;

		List<BigInteger> strings = generateRandomStrings(n);
		strings.forEach(x -> tree.put(x.toString(32), x));

		Assertions.assertEquals(strings.size(), tree.size());
		strings.forEach(x -> Assertions.assertTrue(tree.containsKeyPrefixedBy(x.toString(32))));
		assertEqualsWithSort(strings, new ArrayList<>(tree.valuesPrefixedBy()));
	}

	private List<BigInteger> generateRandomStrings(int n){
		List<BigInteger> strings = new ArrayList<>();
		while(n -- > 0){
			BigInteger bigint = new BigInteger(20, rng);
			if(!strings.contains(bigint))
				strings.add(bigint);
		}
		return strings;
	}


	@Test
	public void contains(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abc", 1);
		tree.put("abb", 2);
		tree.put("ac", 3);
		tree.put("a", 4);

		Assertions.assertTrue(tree.containsKeyPrefixedBy("a"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("ab"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("c"));
	}

	@Test
	public void duplicatedEntry(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abc", 1);
		tree.put("abc", 2);

		Assertions.assertFalse(tree.containsKeyPrefixedBy("a"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("ab"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("abc"));
		Assertions.assertEquals(2, tree.getPrefixedBy("abc").intValue());
		Assertions.assertFalse(tree.containsKeyPrefixedBy("c"));
	}

	@Test
	public void collectPrefixes(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("a", 1);
		tree.put("ab", 2);
		tree.put("bc", 3);
		tree.put("cd", 4);
		tree.put("abc", 5);

		List<Integer> prefixes = tree.getValuesPrefixedTo("abcd");
		Integer[] datas = prefixes.stream()
			.sorted()
			.toArray(Integer[]::new);
		Assertions.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void emptyConstructor(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertTrue(tree.isEmpty());
		Assertions.assertFalse(tree.containsKeyPrefixedBy("word"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy(StringUtils.EMPTY));
	}

	@Test
	public void defaultValueConstructor(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(tree.getPrefixedBy("meow"));

		tree.put("meow", Boolean.TRUE);

		Assertions.assertEquals(Boolean.TRUE, tree.getPrefixedBy("meow"));
		Assertions.assertNull(tree.getPrefixedBy("world"));
	}

	@Test
	public void simplePut(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertTrue(tree.isEmpty());

		tree.put("java.lang.", Boolean.TRUE);
		tree.put("java.i", Boolean.TRUE);
		tree.put("java.io.", Boolean.TRUE);
		tree.put("java.util.concurrent.", Boolean.TRUE);
		tree.put("java.util.", Boolean.FALSE);
		tree.put("java.lang.Boolean", Boolean.FALSE);

		Assertions.assertFalse(tree.isEmpty());
		Assertions.assertEquals(1, tree.getValuesPrefixedTo("java.lang.Integer").size());
		Assertions.assertEquals(1, tree.getValuesPrefixedTo("java.lang.Long").size());
		Assertions.assertEquals(2, tree.getValuesPrefixedTo("java.lang.Boolean").size());
		Assertions.assertEquals(2, tree.getValuesPrefixedTo("java.io.InputStream").size());
		Assertions.assertEquals(1, tree.getValuesPrefixedTo("java.util.ArrayList").size());
		Assertions.assertEquals(2, tree.getValuesPrefixedTo("java.util.concurrent.ConcurrentHashMap").size());
	}

	@Test
	public void hasStartsWithMatch(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(1, tree.getValuesPrefixedTo("wowzacowza").size());
	}

	@Test
	public void hasExactMatch(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertTrue(tree.containsKeyPrefixedBy("wowza"));
	}

	@Test
	public void getStartsWithMatch(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(1, tree.getValuesPrefixedTo("wowzacowza").size());
		Assertions.assertEquals(1, tree.getValuesPrefixedTo("bookshelfmania").size());
	}

	@Test
	public void getExactMatch(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(Boolean.FALSE, tree.getPrefixedBy("wowza"));
		Assertions.assertEquals(Boolean.TRUE, tree.getPrefixedBy("bookshelf"));
		Assertions.assertNull(tree.getPrefixedBy("bookshelf2"));
	}

	@Test
	public void removeBack(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("hello", 0);
		tree.put("hello world", 1);

		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertEquals(1, tree.getPrefixedBy("hello world").intValue());

		Integer r1 = tree.removePrefixedBy("hello world");

		Assertions.assertNotNull(r1);
		Assertions.assertEquals(1, r1.intValue());

		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertNull(tree.getPrefixedBy("hello world"));
	}

	@Test
	public void removeFront(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("hello", 0);
		tree.put("hello world", 1);

		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertEquals(1, tree.getPrefixedBy("hello world").intValue());

		Integer r0 = tree.removePrefixedBy("hello world");

		Assertions.assertNotNull(r0);
		Assertions.assertEquals(1, r0.intValue());

		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertNull(tree.getPrefixedBy("hello world"));
	}

	@Test
	public void removeFrontManyChildren(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("hello", 0);
		tree.put("hello world", 1);
		tree.put("hello, clarice", 2);

		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertEquals(1, tree.getPrefixedBy("hello world").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("hello, clarice").intValue());

		Integer r0 = tree.removePrefixedBy("hello world");

		Assertions.assertNotNull(r0);
		Assertions.assertEquals(1, r0.intValue());

		Assertions.assertNull(tree.getPrefixedBy("hello world"));
		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("hello, clarice").intValue());
	}


	public static <T extends Comparable<? super T>> void assertEqualsWithSort(List<T> a, List<T> b){
		Collections.sort(a);
		Collections.sort(b);
		Assertions.assertEquals(a, b);
	}

	public static <T extends Comparable<? super T>> void assertEqualsWithSort(T[] a, T[] b){
		Arrays.sort(a);
		Arrays.sort(b);
		Assertions.assertArrayEquals(a, b);
	}


	@Test
	public void testSearchForPartialParentAndLeafKeyWhenOverlapExists(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(0, tree.getValuesPrefixedBy("abe").size());
		Assertions.assertEquals(0, tree.getValuesPrefixedBy("abd").size());
	}

	@Test
	public void testSearchForLeafNodesWhenOverlapExists(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(1, tree.getValuesPrefixedBy("abcd").size());
		Assertions.assertEquals(1, tree.getValuesPrefixedBy("abce").size());
	}

	@Test
	public void testSearchForStringSmallerThanSharedParentWhenOverlapExists(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(2, tree.getValuesPrefixedBy("ab").size());
		Assertions.assertEquals(2, tree.getValuesPrefixedBy("a").size());
	}

	@Test
	public void testSearchForStringEqualToSharedParentWhenOverlapExists(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(2, tree.getValuesPrefixedBy("abc").size());
	}

	@Test
	public void testInsert(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("bat", "bat");
		tree.put("ape", "ape");
		tree.put("bath", "bath");
		tree.put("banana", "banana");

		Assertions.assertEquals(new RadixTreeNode<>("ple", "apple"), tree.findPrefixedBy("apple"));
		Assertions.assertEquals(new RadixTreeNode<>("t", "bat"), tree.findPrefixedBy("bat"));
		Assertions.assertEquals(new RadixTreeNode<>("e", "ape"), tree.findPrefixedBy("ape"));
		Assertions.assertEquals(new RadixTreeNode<>("h", "bath"), tree.findPrefixedBy("bath"));
		Assertions.assertEquals(new RadixTreeNode<>("nana", "banana"), tree.findPrefixedBy("banana"));
	}

	@Test
	public void testInsertExistingUnrealNodeConvertsItToReal(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assertions.assertFalse(tree.containsKeyPrefixedBy("apple"));

		tree.put("apple", "apple");

		Assertions.assertTrue(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testDuplicatesAllowed(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		try{
			tree.put("apple", "apple2");

			Assertions.assertTrue(true);
		}
		catch(DuplicateKeyException e){
			Assertions.fail("Duplicate should have been allowed");
		}
	}

	@Test
	public void testDuplicatesNotAllowed(){
		RadixTree<String, String> tree = AhoCorasickTree.createTreeNoDuplicates(new StringSequencer());

		tree.put("apple", "apple");

		try{
			tree.put("apple", "apple2");

			Assertions.fail("Duplicate should not have been allowed");
		}
		catch(DuplicateKeyException e){
			Assertions.assertEquals("Duplicate key: 'apple'", e.getMessage());
		}
	}

	@Test
	public void testInsertWithRepeatingPatternsInKey(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("xbox 360", "xbox 360");
		tree.put("xbox", "xbox");
		tree.put("xbox 360 games", "xbox 360 games");
		tree.put("xbox games", "xbox games");
		tree.put("xbox xbox 360", "xbox xbox 360");
		tree.put("xbox xbox", "xbox xbox");
		tree.put("xbox 360 xbox games", "xbox 360 xbox games");
		tree.put("xbox games 360", "xbox games 360");
		tree.put("xbox 360 360", "xbox 360 360");
		tree.put("xbox 360 xbox 360", "xbox 360 xbox 360");
		tree.put("360 xbox games 360", "360 xbox games 360");
		tree.put("xbox xbox 361", "xbox xbox 361");

		Assertions.assertEquals(12, tree.size());
	}

	@Test
	public void testDeleteNodeWithNoChildren(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		Assertions.assertNotNull(tree.removePrefixedBy("apple"));
	}

	@Test
	public void testDeleteNodeWithOneChild(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");

		Assertions.assertNotNull(tree.removePrefixedBy("apple"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("applepie"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testDeleteNodeWithMultipleChildren(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assertions.assertNotNull(tree.removePrefixedBy("apple"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("applepie"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("applecrisp"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testCantDeleteSomethingThatDoesntExist(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(tree.removePrefixedBy("apple"));
	}

	@Test
	public void testCantDeleteSomethingThatWasAlreadyDeleted(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.removePrefixedBy("apple");

		Assertions.assertNull(tree.removePrefixedBy("apple"));
	}

	@Test
	public void testChildrenNotAffectedWhenOneIsDeleted(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		tree.removePrefixedBy("apple");

		Assertions.assertTrue(tree.containsKeyPrefixedBy("appleshack"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("applepie"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("ape"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testSiblingsNotAffectedWhenOneIsDeleted(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ball", "ball");

		tree.removePrefixedBy("apple");

		Assertions.assertTrue(tree.containsKeyPrefixedBy("ball"));
	}

	@Test
	public void testCantDeleteUnrealNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertNull(tree.removePrefixedBy("ap"));
	}

	@Test
	public void testCantFindRootNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(tree.findPrefixedBy(""));
	}

	@Test
	public void testFindSimpleInsert(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		Assertions.assertNotNull(tree.findPrefixedBy("apple"));
	}

	@Test
	public void testContainsSimpleInsert(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		Assertions.assertTrue(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testFindChildInsert(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assertions.assertNotNull(tree.findPrefixedBy("appletree"));
		Assertions.assertNotNull(tree.findPrefixedBy("appleshackcream"));
		Assertions.assertNotNull(tree.containsKeyPrefixedBy("ape"));
	}

	@Test
	public void testGetPrefixes(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("h", "h");
		tree.put("hey", "hey");
		tree.put("hell", "hell");
		tree.put("hello", "hello");
		tree.put("hat", "hat");
		tree.put("cat", "cat");

		Assertions.assertFalse(tree.getValuesPrefixedTo("helloworld").isEmpty());
		Assertions.assertTrue(tree.getValuesPrefixedTo("helloworld").contains("h"));
		Assertions.assertTrue(tree.getValuesPrefixedTo("helloworld").contains("hell"));
		Assertions.assertTrue(tree.getValuesPrefixedTo("helloworld").contains("hello"));
		Assertions.assertTrue(!tree.getValuesPrefixedTo("helloworld").contains("he"));
		Assertions.assertTrue(!tree.getValuesPrefixedTo("helloworld").contains("hat"));
		Assertions.assertTrue(!tree.getValuesPrefixedTo("helloworld").contains("cat"));
		Assertions.assertTrue(!tree.getValuesPrefixedTo("helloworld").contains("hey"));
		Assertions.assertTrue(tree.getValuesPrefixedTo("animal").isEmpty());
	}

	@Test
	public void testContainsChildInsert(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assertions.assertTrue(tree.containsKeyPrefixedBy("appletree"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("appleshackcream"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("ape"));
	}

	@Test
	public void testCantFindNonexistantNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(tree.findPrefixedBy("apple"));
	}

	@Test
	public void testDoesntContainNonexistantNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertFalse(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testCantFindUnrealNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertNull(tree.findPrefixedBy("ap"));
	}

	@Test
	public void testDoesntContainUnrealNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertFalse(tree.containsKeyPrefixedBy("ap"));
	}

	@Test
	public void testSearchPrefix_LimitGreaterThanPossibleResults(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValuesPrefixedBy("app");
		Assertions.assertEquals(4, result.size());

		Assertions.assertTrue(result.contains("appleshack"));
		Assertions.assertTrue(result.contains("appleshackcream"));
		Assertions.assertTrue(result.contains("applepie"));
		Assertions.assertTrue(result.contains("apple"));
	}

	@Test
	public void testSearchPrefix_LimitLessThanPossibleResults(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValuesPrefixedBy("appl");
		Assertions.assertEquals(4, result.size());

		Assertions.assertTrue(result.contains("appleshack"));
		Assertions.assertTrue(result.contains("applepie"));
		Assertions.assertTrue(result.contains("apple"));
	}

	@Test
	public void testGetSize(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		Assertions.assertTrue(tree.size() == 5);
	}

	@Test
	public void testDeleteReducesSize(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");

		tree.removePrefixedBy("appleshack");

		Assertions.assertTrue(tree.size() == 1);
	}

}
