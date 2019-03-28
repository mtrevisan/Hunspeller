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
	public void emptyTree(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertTrue(tree.isEmpty());
		Assertions.assertEquals(0, tree.size());
	}

	@Test
	public void singleInsertion(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);

		Assertions.assertEquals(1, tree.size());
		Assertions.assertTrue(tree.containsKey("test", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("tes", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("testt", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void multipleInsertions(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);
		tree.put("rest", 4);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(1, tree.get("test", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("tent", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(3, tree.get("tank", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(4, tree.get("rest", RadixTree.PrefixType.PREFIXED_BY).intValue());
	}

	@Test
	public void prepare(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tentest", 21);
		tree.put("tank", 3);
		tree.put("rest", 4);
		tree.prepare();

		Assertions.assertEquals(5, tree.size());
		Assertions.assertEquals(1, tree.get("test", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("tent", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(3, tree.get("tank", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(4, tree.get("rest", RadixTree.PrefixType.PREFIXED_BY).intValue());

		Iterator<SearchResult<String, Integer>> itr = tree.searchPrefixedBy("resting in the test");
		SearchResult<String, Integer> search = itr.next();
		Assertions.assertEquals(0, search.getIndex());
		Assertions.assertEquals(4, search.getNode().getValue().intValue());
		search = itr.next();
		Assertions.assertEquals(17, search.getIndex());
		Assertions.assertEquals(1, search.getNode().getValue().intValue());
		Assertions.assertFalse(itr.hasNext());

		itr = tree.searchPrefixedBy("blah");
		Assertions.assertFalse(itr.hasNext());
	}

	@Test
	public void multipleInsertionOfTheSameKey(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);
		tree.put("rest", 4);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(1, tree.get("test", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("tent", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(3, tree.get("tank", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(4, tree.get("rest", RadixTree.PrefixType.PREFIXED_BY).intValue());

		tree.put("test", 9);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(9, tree.get("test", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("tent", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(3, tree.get("tank", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(4, tree.get("rest", RadixTree.PrefixType.PREFIXED_BY).intValue());
	}

	@Test
	public void prefixFetch(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("rest", 3);
		tree.put("tank", 4);

		Assertions.assertEquals(4, tree.size());
		assertEqualsWithSort(tree.getValues(StringUtils.EMPTY, RadixTree.PrefixType.PREFIXED_BY), new ArrayList<>(tree.values(RadixTree.PrefixType.PREFIXED_BY)));
		assertEqualsWithSort(new Integer[]{1, 2, 4}, tree.getValues("t", RadixTree.PrefixType.PREFIXED_BY).toArray(new Integer[3]));
		assertEqualsWithSort(new Integer[]{1, 2}, tree.getValues("te", RadixTree.PrefixType.PREFIXED_BY).toArray(new Integer[2]));
		Assertions.assertArrayEquals(new Object[0], tree.getValues("asd", RadixTree.PrefixType.PREFIXED_BY).toArray());
	}

	@Test
	public void spook(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("pook", 1);
		tree.put("spook", 2);

		Assertions.assertEquals(2, tree.size());
		assertEqualsWithSort(tree.keySet(RadixTree.PrefixType.PREFIXED_BY).toArray(new String[2]), new String[]{
			"pook",
			"spook"
		});
	}

	@Test
	public void removal(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);

		Assertions.assertEquals(3, tree.size());
		Assertions.assertTrue(tree.containsKey("tent", RadixTree.PrefixType.PREFIXED_BY));

		tree.remove("key");

		Assertions.assertEquals(3, tree.size());
		Assertions.assertTrue(tree.containsKey("tent", RadixTree.PrefixType.PREFIXED_BY));

		tree.remove("tent");

		Assertions.assertEquals(2, tree.size());
		Assertions.assertEquals(1, tree.get("test", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertFalse(tree.containsKey("tent", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(3, tree.get("tank", RadixTree.PrefixType.PREFIXED_BY).intValue());
	}

	@Test
	public void manyInsertions(){
		RadixTree<String, BigInteger> tree = AhoCorasickTree.createTree(new StringSequencer());

		//n in [100, 500]
		int n = rng.nextInt(401) + 100;

		List<BigInteger> strings = generateRandomStrings(n);
		strings.forEach(x -> tree.put(x.toString(32), x));

		Assertions.assertEquals(strings.size(), tree.size());
		strings.forEach(x -> Assertions.assertTrue(tree.containsKey(x.toString(32), RadixTree.PrefixType.PREFIXED_BY)));
		assertEqualsWithSort(strings, new ArrayList<>(tree.values(RadixTree.PrefixType.PREFIXED_BY)));
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

		Assertions.assertTrue(tree.containsKey("a", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("ab", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("c", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void duplicatedEntry(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abc", 1);
		tree.put("abc", 2);

		Assertions.assertFalse(tree.containsKey("a", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("ab", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("abc", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(2, tree.get("abc", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertFalse(tree.containsKey("c", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void collectPrefixes(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("a", 1);
		tree.put("ab", 2);
		tree.put("bc", 3);
		tree.put("cd", 4);
		tree.put("abc", 5);

		List<Integer> prefixes = tree.getValues("abcd", RadixTree.PrefixType.PREFIXED_TO);
		Integer[] datas = prefixes.stream()
			.sorted()
			.toArray(Integer[]::new);
		Assertions.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void emptyConstructor(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertTrue(tree.isEmpty());
		Assertions.assertFalse(tree.containsKey("word", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey(StringUtils.EMPTY, RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void defaultValueConstructor(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(tree.get("meow", RadixTree.PrefixType.PREFIXED_BY));

		tree.put("meow", Boolean.TRUE);

		Assertions.assertEquals(Boolean.TRUE, tree.get("meow", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertNull(tree.get("world", RadixTree.PrefixType.PREFIXED_BY));
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
		Assertions.assertEquals(1, tree.getValues("java.lang.Integer", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(1, tree.getValues("java.lang.Long", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(2, tree.getValues("java.lang.Boolean", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(2, tree.getValues("java.io.InputStream", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(1, tree.getValues("java.util.ArrayList", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(2, tree.getValues("java.util.concurrent.ConcurrentHashMap", RadixTree.PrefixType.PREFIXED_TO).size());
	}

	@Test
	public void hasStartsWithMatch(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(1, tree.getValues("wowzacowza", RadixTree.PrefixType.PREFIXED_TO).size());
	}

	@Test
	public void hasExactMatch(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertTrue(tree.containsKey("wowza", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void getStartsWithMatch(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(1, tree.getValues("wowzacowza", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(1, tree.getValues("bookshelfmania", RadixTree.PrefixType.PREFIXED_TO).size());
	}

	@Test
	public void getExactMatch(){
		RadixTree<String, Boolean> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(Boolean.FALSE, tree.get("wowza", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(Boolean.TRUE, tree.get("bookshelf", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertNull(tree.get("bookshelf2", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void removeBack(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("hello", 0);
		tree.put("hello world", 1);

		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(1, tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY).intValue());

		Integer r1 = tree.remove("hello world");

		Assertions.assertNotNull(r1);
		Assertions.assertEquals(1, r1.intValue());

		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertNull(tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void removeFront(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("hello", 0);
		tree.put("hello world", 1);

		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(1, tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY).intValue());

		Integer r0 = tree.remove("hello world");

		Assertions.assertNotNull(r0);
		Assertions.assertEquals(1, r0.intValue());

		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertNull(tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void removeFrontManyChildren(){
		RadixTree<String, Integer> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("hello", 0);
		tree.put("hello world", 1);
		tree.put("hello, clarice", 2);

		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(1, tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("hello, clarice", RadixTree.PrefixType.PREFIXED_BY).intValue());

		Integer r0 = tree.remove("hello world");

		Assertions.assertNotNull(r0);
		Assertions.assertEquals(1, r0.intValue());

		Assertions.assertNull(tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("hello, clarice", RadixTree.PrefixType.PREFIXED_BY).intValue());
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
	public void searchForPartialParentAndLeafKeyWhenOverlapExists(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(0, tree.getValues("abe", RadixTree.PrefixType.PREFIXED_BY).size());
		Assertions.assertEquals(0, tree.getValues("abd", RadixTree.PrefixType.PREFIXED_BY).size());
	}

	@Test
	public void searchForLeafNodesWhenOverlapExists(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(1, tree.getValues("abcd", RadixTree.PrefixType.PREFIXED_BY).size());
		Assertions.assertEquals(1, tree.getValues("abce", RadixTree.PrefixType.PREFIXED_BY).size());
	}

	@Test
	public void searchForStringSmallerThanSharedParentWhenOverlapExists(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(2, tree.getValues("ab", RadixTree.PrefixType.PREFIXED_BY).size());
		Assertions.assertEquals(2, tree.getValues("a", RadixTree.PrefixType.PREFIXED_BY).size());
	}

	@Test
	public void searchForStringEqualToSharedParentWhenOverlapExists(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(2, tree.getValues("abc", RadixTree.PrefixType.PREFIXED_BY).size());
	}

	@Test
	public void insert(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("bat", "bat");
		tree.put("ape", "ape");
		tree.put("bath", "bath");
		tree.put("banana", "banana");

		Assertions.assertEquals(new RadixTreeNode<>("ple", "apple"), tree.find("apple", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(new RadixTreeNode<>("t", "bat"), tree.find("bat", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(new RadixTreeNode<>("e", "ape"), tree.find("ape", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(new RadixTreeNode<>("h", "bath"), tree.find("bath", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(new RadixTreeNode<>("nana", "banana"), tree.find("banana", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void insertExistingUnrealNodeConvertsItToReal(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assertions.assertFalse(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));

		tree.put("apple", "apple");

		Assertions.assertTrue(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void duplicatesAllowed(){
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
	public void duplicatesNotAllowed(){
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
	public void insertWithRepeatingPatternsInKey(){
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
	public void deleteNodeWithNoChildren(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		Assertions.assertNotNull(tree.remove("apple"));
	}

	@Test
	public void deleteNodeWithOneChild(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");

		Assertions.assertNotNull(tree.remove("apple"));
		Assertions.assertTrue(tree.containsKey("applepie", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void deleteNodeWithMultipleChildren(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assertions.assertNotNull(tree.remove("apple"));
		Assertions.assertTrue(tree.containsKey("applepie", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("applecrisp", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void cantDeleteSomethingThatDoesntExist(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(tree.remove("apple"));
	}

	@Test
	public void cantDeleteSomethingThatWasAlreadyDeleted(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.remove("apple");

		Assertions.assertNull(tree.remove("apple"));
	}

	@Test
	public void childrenNotAffectedWhenOneIsDeleted(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		tree.remove("apple");

		Assertions.assertTrue(tree.containsKey("appleshack", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("applepie", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("ape", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void siblingsNotAffectedWhenOneIsDeleted(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ball", "ball");

		tree.remove("apple");

		Assertions.assertTrue(tree.containsKey("ball", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void cantDeleteUnrealNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertNull(tree.remove("ap"));
	}

	@Test
	public void cantFindRootNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(tree.find("", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void findSimpleInsert(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		Assertions.assertNotNull(tree.find("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void containsSimpleInsert(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");

		Assertions.assertTrue(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void findChildInsert(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assertions.assertNotNull(tree.find("appletree", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertNotNull(tree.find("appleshackcream", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertNotNull(tree.containsKey("ape", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void getPrefixes(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("h", "h");
		tree.put("hey", "hey");
		tree.put("hell", "hell");
		tree.put("hello", "hello");
		tree.put("hat", "hat");
		tree.put("cat", "cat");

		Assertions.assertFalse(tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).isEmpty());
		Assertions.assertTrue(tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("h"));
		Assertions.assertTrue(tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hell"));
		Assertions.assertTrue(tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hello"));
		Assertions.assertTrue(!tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("he"));
		Assertions.assertTrue(!tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hat"));
		Assertions.assertTrue(!tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("cat"));
		Assertions.assertTrue(!tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hey"));
		Assertions.assertTrue(tree.getValues("animal", RadixTree.PrefixType.PREFIXED_TO).isEmpty());
	}

	@Test
	public void containsChildInsert(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assertions.assertTrue(tree.containsKey("appletree", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("appleshackcream", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("ape", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void cantFindNonexistantNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(tree.find("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void doesntContainNonexistantNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertFalse(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void cantFindUnrealNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertNull(tree.find("ap", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void doesntContainUnrealNode(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertFalse(tree.containsKey("ap", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	public void searchPrefix_LimitGreaterThanPossibleResults(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValues("app", RadixTree.PrefixType.PREFIXED_BY);
		Assertions.assertEquals(4, result.size());

		Assertions.assertTrue(result.contains("appleshack"));
		Assertions.assertTrue(result.contains("appleshackcream"));
		Assertions.assertTrue(result.contains("applepie"));
		Assertions.assertTrue(result.contains("apple"));
	}

	@Test
	public void searchPrefix_LimitLessThanPossibleResults(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValues("appl", RadixTree.PrefixType.PREFIXED_BY);
		Assertions.assertEquals(4, result.size());

		Assertions.assertTrue(result.contains("appleshack"));
		Assertions.assertTrue(result.contains("applepie"));
		Assertions.assertTrue(result.contains("apple"));
	}

	@Test
	public void getSize(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		Assertions.assertTrue(tree.size() == 5);
	}

	@Test
	public void deleteReducesSize(){
		RadixTree<String, String> tree = AhoCorasickTree.createTree(new StringSequencer());

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");

		tree.remove("appleshack");

		Assertions.assertTrue(tree.size() == 1);
	}

}
