package unit731.hunlinter.services.fsa.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.FSAFlags;
import unit731.hunlinter.services.fsa.FSATestUtils;
import unit731.hunlinter.services.fsa.serializers.CFSA2Serializer;
import unit731.hunlinter.services.fsa.serializers.FSASerializer;
import unit731.hunlinter.services.system.TimeWatch;
import unit731.hunlinter.services.text.StringHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class SerializerTestBase{

	protected FSASerializer createSerializer(){
		return new CFSA2Serializer();
	}


	@Test
	public void testA() throws IOException{
		List<String> input = Collections.singletonList("a");
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		checkSerialization(in, s);
	}

	@Test
	public void testArcsSharing() throws IOException{
		List<String> input = Arrays.asList("acf", "adg", "aeh", "bdg", "beh");
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		checkSerialization(in, s);
	}

	@Test
	public void testFSA5SerializerSimple() throws IOException{
		List<String> input = Arrays.asList("a", "aba", "ac", "b", "ba", "c");
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		checkSerialization(in, s);
	}

	@Test
	public void testNotMinimal() throws IOException{
		List<String> input = Arrays.asList("aba", "b", "ba");
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		checkSerialization(in, s);
	}

	@Test
	public void testFSA5Bug0() throws IOException{
		checkCorrect(Arrays.asList("3-D+A+JJ", "3-D+A+NN", "4-F+A+NN", "z+A+NN"));
	}

	@Test
	public void testFSA5Bug1() throws IOException{
		checkCorrect(Arrays.asList("+NP", "n+N", "n+NP"));
	}

	private void checkCorrect(List<String> input) throws IOException{
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		checkSerialization(in, s);
	}

	@Test
	public void testEmptyInput() throws IOException{
		List<byte[]> input = Collections.emptyList();
		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(input);

		checkSerialization(input, s);
	}

	@Test
	public void test_abc() throws IOException{
		testBuiltIn(FSA.read(SerializerTestBase.class.getResourceAsStream("/services/fsa/builders/abc.fsa")));
	}

	@Test
	public void test_minimal() throws IOException{
		testBuiltIn(FSA.read(SerializerTestBase.class.getResourceAsStream("/services/fsa/builders/minimal.fsa")));
	}

	@Test
	public void test_minimal2() throws IOException{
		testBuiltIn(FSA.read(SerializerTestBase.class.getResourceAsStream("/services/fsa/builders/minimal2.fsa")));
	}

	@Test
	public void test_en_tst() throws IOException{
		testBuiltIn(FSA.read(SerializerTestBase.class.getResourceAsStream("/services/fsa/builders/en_tst.dict")));
	}

	private void testBuiltIn(FSA fsa) throws IOException{
TimeWatch watch = TimeWatch.start();
		List<byte[]> input = new ArrayList<>();
		for(ByteBuffer bb : fsa)
			input.add(Arrays.copyOf(bb.array(), bb.remaining()));
		Collections.sort(input, LexicographicalComparator.lexicographicalComparator());

		FSABuilder builder = new FSABuilder();
		FSA root = builder.build(input);

		//check if the DFSA is correct first
		FSATestUtils.checkCorrect(input, root);

		//check serialization
		checkSerialization(input, root);
	}

	private void checkSerialization(List<byte[]> input, FSA root) throws IOException{
		FSASerializer serializer = createSerializer();
		checkSerialization0(serializer, input, root);
		if(serializer.getFlags().contains(FSAFlags.NUMBERS))
			checkSerialization0(serializer.serializeWithNumbers(), input, root);
	}

	private void checkSerialization0(FSASerializer serializer, List<byte[]> in, FSA root) throws IOException{
		byte[] fsaData = serializer.serialize(root, new ByteArrayOutputStream(), null).toByteArray();

		FSA fsa = FSA.read(new ByteArrayInputStream(fsaData));
		FSATestUtils.checkCorrect(in, fsa);
	}

	@Test
	public void testAutomatonWithNodeNumbers() throws IOException{
		Assertions.assertTrue(createSerializer().getFlags().contains(FSAFlags.NUMBERS));

		List<String> input = Arrays.asList("a", "aba", "ac", "b", "ba", "c");
		List<byte[]> in = input.stream()
			.sorted()
			.map(StringHelper::getRawBytes)
			.collect(Collectors.toList());

		FSABuilder builder = new FSABuilder();
		FSA s = builder.build(in);

		byte[] fsaData = createSerializer()
			.serializeWithNumbers()
			.serialize(s, new ByteArrayOutputStream(), null)
			.toByteArray();

		FSA fsa = FSA.read(new ByteArrayInputStream(fsaData));

		//ensure we have the NUMBERS flag set
		Assertions.assertTrue(fsa.getFlags().contains(FSAFlags.NUMBERS));

		//get all numbers from nodes
		byte[] buffer = new byte[128];
		List<String> result = new ArrayList<>();
		FSATestUtils.walkNode(buffer, 0, fsa, fsa.getRootNode(), 0, result);

		Collections.sort(result);
		Assertions.assertEquals(Arrays.asList("0 a", "1 aba", "2 ac", "3 b", "4 ba", "5 c"), result);
	}

}
