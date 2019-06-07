package unit731.hunspeller.services.regexgenerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class HunspellRegexWordGeneratorTest{

	@Test
	public void allOne(){
		String[] regex = new String[]{"abc", "de", "a"};

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 6);

		List<List<String>> expected = Collections.singletonList(
			Arrays.asList("abc", "de", "a")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	public void oneForEach(){
		String[] regex = new String[]{"abc", "de", "?", "a", "*"};

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 6);

		List<List<String>> expected = Arrays.asList(
			Collections.singletonList("abc"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de", "a"),
			Arrays.asList("abc", "a", "a"),
			Arrays.asList("abc", "de", "a", "a")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	public void allZeroOrOne(){
		String[] regex = new String[]{"abc", "?", "de", "?", "a", "?"};

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 7);

		List<List<String>> expected = Arrays.asList(
			Collections.singletonList("a"),
			Collections.singletonList("de"),
			Arrays.asList("de", "a"),
			Collections.singletonList("abc"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "de", "a")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	public void allZeroOrOneWithZeroMinimum(){
		String[] regex = new String[]{"abc", "?", "de", "?", "a", "?"};

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(0, 7);

		List<List<String>> expected = Arrays.asList(
			Collections.emptyList(),
			Collections.singletonList("a"),
			Collections.singletonList("de"),
			Arrays.asList("de", "a"),
			Collections.singletonList("abc"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	public void allZeroOrOneWithTwoMinimum(){
		String[] regex = new String[]{"abc", "?", "de", "?", "a", "?"};

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(2, 7);

		List<List<String>> expected = Arrays.asList(
			Arrays.asList("de", "a"),
			Arrays.asList("abc", "a"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "de", "a")
		);
		Assertions.assertEquals(expected, words);
	}

	@Test
	public void allZeroOrMore(){
		String[] regex = new String[]{"abc", "*", "de", "*", "a", "*"};

		HunspellRegexWordGenerator generator = new HunspellRegexWordGenerator(regex);
		List<List<String>> words = generator.generateAll(1, 7);

		List<List<String>> expected = Arrays.asList(
			Collections.singletonList("abc"),
			Collections.singletonList("de"),
			Collections.singletonList("a"),
			Arrays.asList("abc", "abc"),
			Arrays.asList("abc", "de"),
			Arrays.asList("abc", "a"),
			Arrays.asList("de", "de")
		);
		Assertions.assertEquals(expected, words);
	}

}
