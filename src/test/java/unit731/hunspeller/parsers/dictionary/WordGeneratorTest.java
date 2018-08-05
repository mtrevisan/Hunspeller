package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.FileService;


//https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline > morph.aff upward
public class WordGeneratorTest{

	private AffixParser affParser;
	private FlagParsingStrategy strategy;
	private WordGenerator wordGenerator;


	@Before
	public void init(){
		affParser = new AffixParser();
		strategy = affParser.getFlagParsingStrategy();
		File dicFile = FileService.getTemporaryUTF8File(StringUtils.EMPTY, ".dic");
		DictionaryParser dicParser = new DictionaryParser(dicFile, StandardCharsets.UTF_8);
		wordGenerator = new WordGenerator(affParser, dicParser, null);
	}

	@Test
	public void conditions() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("SFX A Y 6")
			.add("SFX A 0 a .")
			.add("SFX A 0 b b")
			.add("SFX A 0 c [ab]")
			.add("SFX A 0 d [^ab]")
			.add("SFX A 0 e [^c]")
			.add("SFX A 0 f a[^ab]b")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		String line = "a/A";

		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(4, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "A", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", "", strategy), stems.get(1));
		Assert.assertEquals(new Production("ac", "", strategy), stems.get(2));
		Assert.assertEquals(new Production("ae", "", strategy), stems.get(3));
	}

	@Test
	public void stems() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("SFX A Y 1")
			.add("SFX A 0 a")
			.add("SFX B Y 1")
			.add("SFX B 0 b/A")
			.add("SFX C Y 1")
			.add("SFX C 0 c/E")
			.add("SFX D Y 1")
			.add("SFX D 0 d/AE")
			.add("PFX E Y 1")
			.add("PFX E 0 e")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		String line = "a/ABCDE";

		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(12, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "ABCDE", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", "E", strategy), stems.get(1));
		Assert.assertEquals(new Production("ab", "AE", strategy), stems.get(2));
		Assert.assertEquals(new Production("ac", "E", strategy), stems.get(3));
		Assert.assertEquals(new Production("ad", "AE", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new Production("aba", "", strategy), stems.get(5));
		Assert.assertEquals(new Production("ada", "", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new Production("ea", "", strategy), stems.get(7));
		Assert.assertEquals(new Production("eaa", "", strategy), stems.get(8));
		Assert.assertEquals(new Production("eac", "", strategy), stems.get(10));
		Assert.assertEquals(new Production("ead", "", strategy), stems.get(11));
	}

	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidTwofold() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("SFX A Y 1")
			.add("SFX A 0 a")
			.add("SFX B Y 1")
			.add("SFX B 0 b/A")
			.add("SFX C Y 1")
			.add("SFX C 0 c/E")
			.add("SFX D Y 1")
			.add("SFX D 0 d/AE")
			.add("PFX E Y 1")
			.add("PFX E 0 e")
			.add("PFX F Y 1")
			.add("PFX F 0 f/A")
			.add("PFX G Y 1")
			.add("PFX G 0 g/E")
			.add("PFX H Y 1")
			.add("PFX H 0 h/AE")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		String line = "a/ABCDEFGH";

		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(27, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "ABCDEFGH", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", "EFGH", strategy), stems.get(1));
		Assert.assertEquals(new Production("ab", "AEFGH", strategy), stems.get(2));
		Assert.assertEquals(new Production("ac", "EFGH", strategy), stems.get(3));
		Assert.assertEquals(new Production("ad", "AEFGH", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new Production("aba", "", strategy), stems.get(5));
		Assert.assertEquals(new Production("ada", "", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new Production("ea", "", strategy), stems.get(7));
		Assert.assertEquals(new Production("fa", "A", strategy), stems.get(8));
		Assert.assertEquals(new Production("ga", "E", strategy), stems.get(9));
		Assert.assertEquals(new Production("ha", "AE", strategy), stems.get(10));
		Assert.assertEquals(new Production("eaa", "", strategy), stems.get(11));
		Assert.assertEquals(new Production("faa", "A", strategy), stems.get(12));
		Assert.assertEquals(new Production("gaa", "E", strategy), stems.get(13));
		Assert.assertEquals(new Production("haa", "AE", strategy), stems.get(14));
		Assert.assertEquals(new Production("eab", "", strategy), stems.get(15));
		Assert.assertEquals(new Production("fab", "A", strategy), stems.get(16));
		Assert.assertEquals(new Production("gab", "E", strategy), stems.get(17));
		Assert.assertEquals(new Production("hab", "AE", strategy), stems.get(18));
		Assert.assertEquals(new Production("eac", "", strategy), stems.get(19));
		Assert.assertEquals(new Production("fac", "A", strategy), stems.get(20));
		Assert.assertEquals(new Production("gac", "E", strategy), stems.get(21));
		Assert.assertEquals(new Production("hac", "AE", strategy), stems.get(22));
		Assert.assertEquals(new Production("ead", "", strategy), stems.get(23));
		Assert.assertEquals(new Production("fad", "A", strategy), stems.get(24));
		Assert.assertEquals(new Production("gad", "E", strategy), stems.get(25));
		Assert.assertEquals(new Production("had", "AE", strategy), stems.get(26));
	}

	@Test
	public void stemsComplexPrefixes() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("COMPLEXPREFIXES")
			.add("PFX A Y 1")
			.add("PFX A 0 a")
			.add("PFX B Y 1")
			.add("PFX B 0 b/A")
			.add("PFX C Y 1")
			.add("PFX C 0 c/E")
			.add("PFX D Y 1")
			.add("PFX D 0 d/AE")
			.add("SFX E Y 1")
			.add("SFX E 0 e")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		String line = "a/ABCDE";

		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(12, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "ABCDE", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", "E", strategy), stems.get(1));
		Assert.assertEquals(new Production("ba", "AE", strategy), stems.get(2));
		Assert.assertEquals(new Production("ca", "E", strategy), stems.get(3));
		Assert.assertEquals(new Production("da", "AE", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new Production("aba", "", strategy), stems.get(5));
		Assert.assertEquals(new Production("ada", "", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new Production("ae", "", strategy), stems.get(7));
		Assert.assertEquals(new Production("aae", "", strategy), stems.get(8));
		Assert.assertEquals(new Production("cae", "", strategy), stems.get(10));
		Assert.assertEquals(new Production("dae", "", strategy), stems.get(11));
	}

	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidTwofoldComplexPrefixes() throws IOException{
		StringJoiner sj = new StringJoiner("\n");
		String content = sj.add("SET UTF-8")
			.add("COMPLEXPREFIXES")
			.add("PFX A Y 1")
			.add("PFX A 0 a")
			.add("PFX B Y 1")
			.add("PFX B 0 b/A")
			.add("PFX C Y 1")
			.add("PFX C 0 c/E")
			.add("PFX D Y 1")
			.add("PFX D 0 d/AE")
			.add("SFX E Y 1")
			.add("SFX E 0 e")
			.add("SFX F Y 1")
			.add("SFX F 0 f/A")
			.add("SFX G Y 1")
			.add("SFX G 0 g/E")
			.add("SFX H Y 1")
			.add("SFX H 0 h/AE")
			.toString();
		File affFile = FileService.getTemporaryUTF8File(content);
		affParser.parse(affFile);
		String line = "a/ABCDEFGH";

		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(27, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "ABCDEFGH", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", "EFGH", strategy), stems.get(1));
		Assert.assertEquals(new Production("ba", "AEFGH", strategy), stems.get(2));
		Assert.assertEquals(new Production("ca", "EFGH", strategy), stems.get(3));
		Assert.assertEquals(new Production("da", "AEFGH", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new Production("aba", "", strategy), stems.get(5));
		Assert.assertEquals(new Production("ada", "", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new Production("ae", "", strategy), stems.get(7));
		Assert.assertEquals(new Production("af", "A", strategy), stems.get(8));
		Assert.assertEquals(new Production("ag", "E", strategy), stems.get(9));
		Assert.assertEquals(new Production("ah", "AE", strategy), stems.get(10));
		Assert.assertEquals(new Production("aae", "", strategy), stems.get(11));
		Assert.assertEquals(new Production("aaf", "A", strategy), stems.get(12));
		Assert.assertEquals(new Production("aag", "E", strategy), stems.get(13));
		Assert.assertEquals(new Production("aah", "AE", strategy), stems.get(14));
		Assert.assertEquals(new Production("bae", "", strategy), stems.get(15));
		Assert.assertEquals(new Production("baf", "A", strategy), stems.get(16));
		Assert.assertEquals(new Production("bag", "E", strategy), stems.get(17));
		Assert.assertEquals(new Production("bah", "AE", strategy), stems.get(18));
		Assert.assertEquals(new Production("cae", "", strategy), stems.get(19));
		Assert.assertEquals(new Production("caf", "A", strategy), stems.get(20));
		Assert.assertEquals(new Production("cag", "E", strategy), stems.get(21));
		Assert.assertEquals(new Production("cah", "AE", strategy), stems.get(22));
		Assert.assertEquals(new Production("dae", "", strategy), stems.get(23));
		Assert.assertEquals(new Production("daf", "A", strategy), stems.get(24));
		Assert.assertEquals(new Production("dag", "E", strategy), stems.get(25));
		Assert.assertEquals(new Production("dah", "AE", strategy), stems.get(26));
	}

}
