package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import net.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.regexgenerator.HunspellRegexWordGenerator;


//https://github.com/hunspell/hunspell/tree/master/tests/v1cmdline > circumfix.aff upward
public class WordGeneratorTest{

	private final AffixParser affParser = new AffixParser();
	private FlagParsingStrategy strategy;
	private DictionaryParser dicParser;


	@Test
	public void affFormat() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"# Testing also whitespace and comments.",
			"OCONV 7 # space, space",
			"OCONV	a A # tab, space, space",
			"OCONV	á	Á # tab, tab, space",
			"OCONV	b	B	# tab, tab, tab",
			"OCONV  c  C		# 2xspace, 2xspace, 2xtab",
			"OCONV	 d 	D # tab+space, space+tab, space",
			"OCONV e E #",
			"OCONV é É 	",
			"",
			" # space",
			"  # 2xspace",
			"	# tab",
			"		# 2xtab",
			" 	# space+tab",
			"	 # tab+space");
		affParser.parse(affFile);

		Map<String, String> outputConversionTable = affParser.getData(AffixTag.OUTPUT_CONVERSION_TABLE);

		Map<String, String> expected = new HashMap<>();
		expected.put("a", "A");
		expected.put("á", "Á");
		expected.put("b", "B");
		expected.put("c", "C");
		expected.put("d", "D");
		expected.put("e", "E");
		expected.put("é", "É");
		Assert.assertEquals(expected, outputConversionTable);
	}


	@Test
	public void flagUTF8() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG UTF-8",
			"SFX A Y 1",
			"SFX A 0 s/ÖüÜ .",
			"SFX Ö Y 1",
			"SFX Ö 0 bar .",
			"SFX ü Y 1",
			"SFX ü 0 baz .",
			"PFX Ü Y 1",
			"PFX Ü 0 un .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "foo/AÜ";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(8, stems.size());
		//base production
		Assert.assertEquals(new Production("foo", "AÜ", "st:foo", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("foos", "ÖüÜ", "st:foo", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("foosbar", "Ü", "st:foo", strategy), stems.get(2));
		Assert.assertEquals(new Production("foosbaz", "Ü", "st:foo", strategy), stems.get(3));
		//lastfold productions
		Assert.assertEquals(new Production("unfoo", null, "st:foo", strategy), stems.get(4));
		Assert.assertEquals(new Production("unfoos", null, "st:foo", strategy), stems.get(5));
		Assert.assertEquals(new Production("unfoosbar", null, "st:foo", strategy), stems.get(6));
		Assert.assertEquals(new Production("unfoosbaz", null, "st:foo", strategy), stems.get(7));
	}

	@Test
	public void flagNumerical() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG num",
			"SFX 999 Y 1",
			"SFX 999 0 s/214,216,54321 .",
			"SFX 214 Y 1",
			"SFX 214 0 bar .",
			"SFX 216 Y 1",
			"SFX 216 0 baz .",
			"PFX 54321 Y 1",
			"PFX 54321 0 un .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "foo/999,54321";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(8, stems.size());
		//base production
		Assert.assertEquals(new Production("foo", "999,54321", "st:foo", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("foos", "54321,214,216", "st:foo", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("foosbar", "54321", "st:foo", strategy), stems.get(2));
		Assert.assertEquals(new Production("foosbaz", "54321", "st:foo", strategy), stems.get(3));
		//lastfold productions
		Assert.assertEquals(new Production("unfoo", null, "st:foo", strategy), stems.get(4));
		Assert.assertEquals(new Production("unfoos", null, "st:foo", strategy), stems.get(5));
		Assert.assertEquals(new Production("unfoosbar", null, "st:foo", strategy), stems.get(6));
		Assert.assertEquals(new Production("unfoosbaz", null, "st:foo", strategy), stems.get(7));
	}

	@Test
	public void flagASCII() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 s/123 .",
			"SFX 1 Y 1",
			"SFX 1 0 bar .",
			"SFX 2 Y 1",
			"SFX 2 0 baz .",
			"PFX 3 Y 1",
			"PFX 3 0 un .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "foo/A3";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(8, stems.size());
		//base production
		Assert.assertEquals(new Production("foo", "A3", "st:foo", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("foos", "123", "st:foo", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("foosbar", "3", "st:foo", strategy), stems.get(2));
		Assert.assertEquals(new Production("foosbaz", "3", "st:foo", strategy), stems.get(3));
		//lastfold productions
		Assert.assertEquals(new Production("unfoo", null, "st:foo", strategy), stems.get(4));
		Assert.assertEquals(new Production("unfoos", null, "st:foo", strategy), stems.get(5));
		Assert.assertEquals(new Production("unfoosbar", null, "st:foo", strategy), stems.get(6));
		Assert.assertEquals(new Production("unfoosbaz", null, "st:foo", strategy), stems.get(7));
	}

	@Test
	public void flagDoubleASCII() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX zx Y 1",
			"SFX zx 0 s/g?1G09 .",
			"SFX g? Y 1",
			"SFX g? 0 bar .",
			"SFX 1G Y 1",
			"SFX 1G 0 baz .",
			"PFX 09 Y 1",
			"PFX 09 0 un .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "foo/zx09";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(8, stems.size());
		//base production
		Assert.assertEquals(new Production("foo", "zx09", "st:foo", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("foos", "1Gg?09", "st:foo", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("foosbaz", "09", "st:foo", strategy), stems.get(2));
		Assert.assertEquals(new Production("foosbar", "09", "st:foo", strategy), stems.get(3));
		//lastfold productions
		Assert.assertEquals(new Production("unfoo", null, "st:foo", strategy), stems.get(4));
		Assert.assertEquals(new Production("unfoos", null, "st:foo", strategy), stems.get(5));
		Assert.assertEquals(new Production("unfoosbaz", null, "st:foo", strategy), stems.get(6));
		Assert.assertEquals(new Production("unfoosbar", null, "st:foo", strategy), stems.get(7));
	}


	@Test
	public void conditions() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 6",
			"SFX A 0 a .",
			"SFX A 0 b b",
			"SFX A 0 c [ab]",
			"SFX A 0 d [^ab]",
			"SFX A 0 e [^c]",
			"SFX A 0 f a[^ab]b");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "a/A";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(4, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "A", "st:a", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", null, "st:a", strategy), stems.get(1));
		Assert.assertEquals(new Production("ac", null, "st:a", strategy), stems.get(2));
		Assert.assertEquals(new Production("ae", null, "st:a", strategy), stems.get(3));
	}


	@Test
	public void stems1() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2P1",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "aa/S1";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(5, stems.size());
		//base production
		Assert.assertEquals(new Production("aa", "S1", "st:aa", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aas1", "P1S2", "st:aa", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("aas1s2", "P1", "st:aa", strategy), stems.get(2));
		//lastfold productions
		Assert.assertEquals(new Production("p1aas1", null, "st:aa", strategy), stems.get(3));
		Assert.assertEquals(new Production("p1aas1s2", null, "st:aa", strategy), stems.get(4));
	}

	@Test
	public void stems2() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2/P1",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "aa/S1";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(4, stems.size());
		//base production
		Assert.assertEquals(new Production("aa", "S1", "st:aa", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aas1", "S2", "st:aa", strategy), stems.get(1));
		//twofold productions
		//lastfold productions
		Assert.assertEquals(new Production("aas1s2", "P1", "st:aa", strategy), stems.get(2));
		Assert.assertEquals(new Production("p1aas1s2", null, "st:aa", strategy), stems.get(3));
	}

	@Test
	public void stems3() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "aa/S1P1";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(6, stems.size());
		//base production
		Assert.assertEquals(new Production("aa", "S1P1", "st:aa", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aas1", "P1S2", "st:aa", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("aas1s2", "P1", "st:aa", strategy), stems.get(2));
		//lastfold productions
		Assert.assertEquals(new Production("p1aa", null, "st:aa", strategy), stems.get(3));
		Assert.assertEquals(new Production("p1aas1", null, "st:aa", strategy), stems.get(4));
		Assert.assertEquals(new Production("p1aas1s2", null, "st:aa", strategy), stems.get(5));
	}

	@Test
	public void stems4() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2",
			"SFX S2 Y 1",
			"SFX S2 0 s2",
			"PFX P1 Y 1",
			"PFX P1 0 p1");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "aa/P1S1";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(6, stems.size());
		//base production
		Assert.assertEquals(new Production("aa", "P1S1", "st:aa", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aas1", "P1S2", "st:aa", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("aas1s2", "P1", "st:aa", strategy), stems.get(2));
		//lastfold productions
		Assert.assertEquals(new Production("p1aa", null, "st:aa", strategy), stems.get(3));
		Assert.assertEquals(new Production("p1aas1", null, "st:aa", strategy), stems.get(4));
		Assert.assertEquals(new Production("p1aas1s2", null, "st:aa", strategy), stems.get(5));
	}

	@Test
	public void stems5() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 a",
			"SFX B Y 1",
			"SFX B 0 b/A",
			"SFX C Y 1",
			"SFX C 0 c/E",
			"SFX D Y 1",
			"SFX D 0 d/AE",
			"PFX E Y 1",
			"PFX E 0 e");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "a/ABCDE";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(14, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "ABCDE", "st:a", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", "E", "st:a", strategy), stems.get(1));
		Assert.assertEquals(new Production("ab", "AE", "st:a", strategy), stems.get(2));
		Assert.assertEquals(new Production("ac", "E", "st:a", strategy), stems.get(3));
		Assert.assertEquals(new Production("ad", "AE", "st:a", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new Production("aba", "E", "st:a", strategy), stems.get(5));
		Assert.assertEquals(new Production("ada", "E", "st:a", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new Production("ea", null, "st:a", strategy), stems.get(7));
		Assert.assertEquals(new Production("eaa", null, "st:a", strategy), stems.get(8));
		Assert.assertEquals(new Production("eab", null, "st:a", strategy), stems.get(9));
		Assert.assertEquals(new Production("eac", null, "st:a", strategy), stems.get(10));
		Assert.assertEquals(new Production("ead", null, "st:a", strategy), stems.get(11));
		Assert.assertEquals(new Production("eaba", null, "st:a", strategy), stems.get(12));
		Assert.assertEquals(new Production("eada", null, "st:a", strategy), stems.get(13));
	}


	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidFullstrip() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A a b a");
		affParser.parse(affFile);
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "a/A";
		wordGenerator.applyRules(line);
	}

	@Test
	public void stemsValidFullstrip() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FULLSTRIP",
			"SFX A Y 1",
			"SFX A a b a");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "a/A";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(2, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "A", "st:a", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("b", null, "st:a", strategy), stems.get(1));
	}


	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidTwofold1() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"FLAG long",
			"SFX S1 Y 1",
			"SFX S1 0 s1/S2P1",
			"SFX S2 Y 1",
			"SFX S2 0 s2/S3",
			"SFX S3 Y 1",
			"SFX S3 0 s3",
			"PFX P1 Y 1",
			"PFX P1 0 p1/P2",
			"PFX P2 Y 1",
			"PFX P2 0 p2");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "aa/S1";
		wordGenerator.applyRules(line);
	}

	@Test(expected = IllegalArgumentException.class)
	public void stemsInvalidTwofold() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"SFX A Y 1",
			"SFX A 0 a",
			"SFX B Y 1",
			"SFX B 0 b/A",
			"SFX C Y 1",
			"SFX C 0 c/E",
			"SFX D Y 1",
			"SFX D 0 d/AE",
			"PFX E Y 1",
			"PFX E 0 e",
			"PFX F Y 1",
			"PFX F 0 f/A",
			"PFX G Y 1",
			"PFX G 0 g/E",
			"PFX H Y 1",
			"PFX H 0 h/AE");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "a/ABCDEFGH";
		wordGenerator.applyRules(line);
	}


	@Test
	public void complexPrefixes1() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 a",
			"PFX B Y 1",
			"PFX B 0 b/A",
			"PFX C Y 1",
			"PFX C 0 c/E",
			"PFX D Y 1",
			"PFX D 0 d/AE",
			"SFX E Y 1",
			"SFX E 0 e");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "a/ABCDE";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(14, stems.size());
		//base production
		Assert.assertEquals(new Production("a", "ABCDE", "st:a", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("aa", "E", "st:a", strategy), stems.get(1));
		Assert.assertEquals(new Production("ba", "AE", "st:a", strategy), stems.get(2));
		Assert.assertEquals(new Production("ca", "E", "st:a", strategy), stems.get(3));
		Assert.assertEquals(new Production("da", "AE", "st:a", strategy), stems.get(4));
		//twofold productions
		Assert.assertEquals(new Production("aba", "E", "st:a", strategy), stems.get(5));
		Assert.assertEquals(new Production("ada", "E", "st:a", strategy), stems.get(6));
		//lastfold productions
		Assert.assertEquals(new Production("ae", null, "st:a", strategy), stems.get(7));
		Assert.assertEquals(new Production("aae", null, "st:a", strategy), stems.get(8));
		Assert.assertEquals(new Production("bae", null, "st:a", strategy), stems.get(9));
		Assert.assertEquals(new Production("cae", null, "st:a", strategy), stems.get(10));
		Assert.assertEquals(new Production("dae", null, "st:a", strategy), stems.get(11));
		Assert.assertEquals(new Production("abae", null, "st:a", strategy), stems.get(12));
		Assert.assertEquals(new Production("adae", null, "st:a", strategy), stems.get(13));
	}

	@Test
	public void complexPrefixes() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 tek .",
			"PFX B Y 1",
			"PFX B 0 met/A .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "ouro/B";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(3, stems.size());
		//base production
		Assert.assertEquals(new Production("ouro", "B", "st:ouro", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("metouro", "A", "st:ouro", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("tekmetouro", null, "st:ouro", strategy), stems.get(2));
		//lastfold productions
	}

	@Test
	public void complexPrefixesUTF8() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 ⲧⲉⲕ .",
			"PFX B Y 1",
			"PFX B 0 ⲙⲉⲧ/A .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "ⲟⲩⲣⲟ/B";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(3, stems.size());
		//base production
		Assert.assertEquals(new Production("ⲟⲩⲣⲟ", "B", "st:ⲟⲩⲣⲟ", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("ⲙⲉⲧⲟⲩⲣⲟ", "A", "st:ⲟⲩⲣⲟ", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("ⲧⲉⲕⲙⲉⲧⲟⲩⲣⲟ", null, "st:ⲟⲩⲣⲟ", strategy), stems.get(2));
		//lastfold productions
	}

	@Test(expected = IllegalArgumentException.class)
	public void complexPrefixesInvalidTwofold() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"PFX A Y 1",
			"PFX A 0 a",
			"PFX B Y 1",
			"PFX B 0 b/A",
			"PFX C Y 1",
			"PFX C 0 c/E",
			"PFX D Y 1",
			"PFX D 0 d/AE",
			"SFX E Y 1",
			"SFX E 0 e",
			"SFX F Y 1",
			"SFX F 0 f/A",
			"SFX G Y 1",
			"SFX G 0 g/E",
			"SFX H Y 1",
			"SFX H 0 h/AE");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "a/ABCDEFGH";
		wordGenerator.applyRules(line);
	}


	@Test
	public void needAffix3() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"NEEDAFFIX X",
			"SFX A Y 1",
			"SFX A 0 s/XB .",
			"SFX B Y 1",
			"SFX B 0 baz .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "foo/A";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(2, stems.size());
		//base production
		Assert.assertEquals(new Production("foo", "A", "st:foo", strategy), stems.get(0));
		//onefold productions
		//twofold productions
		Assert.assertEquals(new Production("foosbaz", "X", "st:foo", strategy), stems.get(1));
		//lastfold productions
	}

	@Test
	public void needAffix5() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"NEEDAFFIX X",
			"SFX A Y 2",
			"SFX A 0 -suf/B .",
			"SFX A 0 -pseudosuf/XB .",
			"SFX B Y 1",
			"SFX B 0 -bar .",
			"PFX C Y 2",
			"PFX C 0 pre- .",
			"PFX C 0 pseudopre-/X .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "foo/AC";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(12, stems.size());
		//base production
		Assert.assertEquals(new Production("foo", "AC", "st:foo", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("foo-suf", "BC", "st:foo", strategy), stems.get(1));
		//twofold productions
		Assert.assertEquals(new Production("foo-suf-bar", "C", "st:foo", strategy), stems.get(2));
		Assert.assertEquals(new Production("foo-pseudosuf-bar", "CX", "st:foo", strategy), stems.get(3));
		//lastfold productions
		Assert.assertEquals(new Production("pre-foo", null, "st:foo", strategy), stems.get(4));
		Assert.assertEquals(new Production("pre-foo-suf", null, "st:foo", strategy), stems.get(5));
		Assert.assertEquals(new Production("pseudopre-foo-suf", "X", "st:foo", strategy), stems.get(6));
		Assert.assertEquals(new Production("pre-foo-pseudosuf", "X", "st:foo", strategy), stems.get(7));
		Assert.assertEquals(new Production("pre-foo-suf-bar", null, "st:foo", strategy), stems.get(8));
		Assert.assertEquals(new Production("pseudopre-foo-suf-bar", "X", "st:foo", strategy), stems.get(9));
		Assert.assertEquals(new Production("pre-foo-pseudosuf-bar", "X", "st:foo", strategy), stems.get(10));
		Assert.assertEquals(new Production("pseudopre-foo-pseudosuf-bar", "X", "st:foo", strategy), stems.get(11));
	}

	
	@Test
	public void circumfix() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"CIRCUMFIX X",
			"PFX A Y 1",
			"PFX A 0 leg/X .",
			"PFX B Y 1",
			"PFX B 0 legesleg/X .",
			"SFX C Y 3",
			"SFX C 0 obb .",
			"SFX C 0 obb/AX .",
			"SFX C 0 obb/BX .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "nagy/C";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(6, stems.size());
		//base production
		Assert.assertEquals(new Production("nagy", "C", "st:nagy", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("nagyobb", null, "st:nagy", strategy), stems.get(1));
		Assert.assertEquals(new Production("nagyobb", "AX", "st:nagy", strategy), stems.get(2));
		Assert.assertEquals(new Production("nagyobb", "BX", "st:nagy", strategy), stems.get(3));
		//twofold productions
		//lastfold productions
		Assert.assertEquals(new Production("legnagyobb", "X", "st:nagy", strategy), stems.get(4));
		Assert.assertEquals(new Production("legeslegnagyobb", "X", "st:nagy", strategy), stems.get(5));
	}


	public void morphologicalAnalisys() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"PFX P Y 1",
			"PFX P   0 un . dp:pfx_un sp:un",
			"SFX S Y 1",
			"SFX S   0 s . is:plur",
			"SFX Q Y 1",
			"SFX Q   0 s . is:sg_3",
			"SFX R Y 1",
			"SFX R   0 able/PS . ds:der_able");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "drink/S	po:noun";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(2, stems.size());
		//base production
		Assert.assertEquals(new Production("drink", "S", "st:drink po:noun", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("drinks", null, "st:drink po:noun is:plur", strategy), stems.get(1));
		//twofold productions
		//lastfold productions


		line = "drink/RQ	po:verb	al:drank	al:drunk	ts:present";
		stems = wordGenerator.applyRules(line);

		Assert.assertEquals(6, stems.size());
		//base production
		Assert.assertEquals(new Production("drink", "RQ", "st:drink po:verb al:drank al:drunk ts:present", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("drinkable", "PS", "st:drink po:verb al:drank al:drunk ts:present ds:der_able", strategy), stems.get(1));
		Assert.assertEquals(new Production("drinks", null, "st:drink po:verb al:drank al:drunk ts:present is:sg_3", strategy), stems.get(2));
		//twofold productions
		Assert.assertEquals(new Production("drinkables", "P", "st:drink po:verb al:drank al:drunk ts:present ds:der_able is:plur", strategy), stems.get(3));
		//lastfold productions
		Assert.assertEquals(new Production("undrinkable", null, "dp:pfx_un sp:un st:drink po:verb al:drank al:drunk ts:present ds:der_able", strategy), stems.get(4));
		Assert.assertEquals(new Production("undrinkables", null, "dp:pfx_un sp:un st:drink po:verb al:drank al:drunk ts:present ds:der_able is:plur", strategy), stems.get(5));
	}


	@Test
	public void alias1() throws IOException{
		File affFile = FileService.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"AF 2",
			"AF AB",
			"AF A",
			"SFX A Y 1",
			"SFX A 0 x .",
			"SFX B Y 1",
			"SFX B 0 y/2 .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		WordGenerator wordGenerator = new WordGenerator(affParser, null, null);

		String line = "foo/1";
		List<Production> stems = wordGenerator.applyRules(line);

		Assert.assertEquals(4, stems.size());
		//base production
		Assert.assertEquals(new Production("foo", "AB", "st:foo", strategy), stems.get(0));
		//onefold productions
		Assert.assertEquals(new Production("foox", null, "st:foo", strategy), stems.get(1));
		Assert.assertEquals(new Production("fooy", "A", "st:foo", strategy), stems.get(2));
		//twofold productions
		Assert.assertEquals(new Production("fooyx", null, "st:foo", strategy), stems.get(3));
		//lastfold productions
	}


	@Test
	public void compoundRule_BjörnJacke() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE vw",
			"SFX A Y 5",
			"SFX A 0 e .",
			"SFX A 0 er .",
			"SFX A 0 en .",
			"SFX A 0 em .",
			"SFX A 0 es .");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		File dicFile = FileService.getTemporaryUTF8File(language, ".dic",
			"3",
			"arbeits/v",
			"scheu/Aw",
			"farbig/A");
		dicParser = new DictionaryParser(dicFile, affParser.getLanguage(), affParser.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affParser, dicParser, null);

		Waiter waiter = new Waiter();
		String line = "vw";
		BiConsumer<List<String>, Long> fnDeferring = (words, wordCount) -> {
			waiter.assertEquals(1, words.size());
			waiter.assertEquals(1l, wordCount);
			waiter.assertEquals("arbeitsscheu", words.get(0));
			waiter.resume();
		};
		wordGenerator.applyCompoundRules(line, fnDeferring);

		waiter.await(2_000l);
	}

	@Test
	public void compoundRuleSimple() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE ABC");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		File dicFile = FileService.getTemporaryUTF8File(language, ".dic",
			"3",
			"a/A",
			"b/B",
			"c/BC");
		dicParser = new DictionaryParser(dicFile, affParser.getLanguage(), affParser.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affParser, dicParser, null);

		Waiter waiter = new Waiter();
		String line = "ABC";
		BiConsumer<List<String>, Long> fnDeferring = (words, wordCount) -> {
			waiter.assertEquals(2, words.size());
			waiter.assertEquals(2l, wordCount);
			waiter.assertEquals("abc", words.get(0));
			waiter.assertEquals("acc", words.get(1));
			waiter.resume();
		};
		wordGenerator.applyCompoundRules(line, fnDeferring);

		waiter.await(2_000l);
	}

	@Test
	public void compoundRuleInfinite() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE A*B*C*");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		File dicFile = FileService.getTemporaryUTF8File(language, ".dic",
			"3",
			"a/A",
			"b/B",
			"c/BC");
		dicParser = new DictionaryParser(dicFile, affParser.getLanguage(), affParser.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affParser, dicParser, null);

		Waiter waiter = new Waiter();
		String line = "A*B*C*";
		BiConsumer<List<String>, Long> fnDeferring = (words, wordTrueCount) -> {
			waiter.assertEquals(37, words.size());
			waiter.assertEquals(HunspellRegexWordGenerator.INFINITY, wordTrueCount);
			List<String> expected = Arrays.asList("a", "b", "c", "aa", "ab", "ac", "bb", "bc", "cb", "cc", "aaa", "aab", "aac", "abb",
				"abc", "acb", "acc", "bbb", "bbc", "bcb", "bcc", "cbb", "cbc", "ccb", "ccc", "aaaa", "aaab", "aaac", "aabb", "aabc", "aacb", "aacc",
				"abbb", "abbc", "abcb", "abcc", "acbb");
			waiter.assertEquals(expected, words);
			waiter.resume();
		};
		wordGenerator.applyCompoundRules(line, fnDeferring, 37);

		waiter.await(2_000l);
	}

	@Test
	public void compoundRuleZeroOrOne() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE A?B?C?");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		File dicFile = FileService.getTemporaryUTF8File(language, ".dic",
			"3",
			"a/A",
			"b/B",
			"c/BC");
		dicParser = new DictionaryParser(dicFile, affParser.getLanguage(), affParser.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affParser, dicParser, null);

		Waiter waiter = new Waiter();
		String line = "A?B?C?";
		BiConsumer<List<String>, Long> fnDeferring = (words, wordTrueCount) -> {
			waiter.assertEquals(9, words.size());
			waiter.assertEquals(9l, wordTrueCount);
			List<String> expected = Arrays.asList("a", "b", "c", "ab", "ac", "bc", "cc", "abc", "acc");
			waiter.assertEquals(expected, words);
			waiter.resume();
		};
		wordGenerator.applyCompoundRules(line, fnDeferring, 37);

		waiter.await(200_000l);
	}

	@Test
	public void compoundRuleLongFlag() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG long",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE (aa)?(bb)?(cc)?");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		File dicFile = FileService.getTemporaryUTF8File(language, ".dic",
			"3",
			"a/aa",
			"b/bb",
			"c/bbcc");
		dicParser = new DictionaryParser(dicFile, affParser.getLanguage(), affParser.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affParser, dicParser, null);

		Waiter waiter = new Waiter();
		String line = "(aa)?(bb)?(cc)?";
		BiConsumer<List<String>, Long> fnDeferring = (words, wordTrueCount) -> {
			waiter.assertEquals(9, words.size());
			waiter.assertEquals(9l, wordTrueCount);
			List<String> expected = Arrays.asList("a", "b", "c", "ab", "ac", "bc", "cc", "abc", "acc");
			waiter.assertEquals(expected, words);
			waiter.resume();
		};
		wordGenerator.applyCompoundRules(line, fnDeferring, 37);

		waiter.await(200_000l);
	}

	@Test
	public void compoundRuleNumericalFlag() throws IOException, TimeoutException{
		String language = "xxx";
		File affFile = FileService.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"FLAG num",
			"COMPOUNDMIN 1",
			"COMPOUNDRULE 1",
			"COMPOUNDRULE (1)?(2)?(3)?");
		affParser.parse(affFile);
		strategy = affParser.getFlagParsingStrategy();
		File dicFile = FileService.getTemporaryUTF8File(language, ".dic",
			"3",
			"a/1",
			"b/2",
			"c/2,3");
		dicParser = new DictionaryParser(dicFile, affParser.getLanguage(), affParser.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affParser, dicParser, null);

		Waiter waiter = new Waiter();
		String line = "(1)?(2)?(3)?";
		BiConsumer<List<String>, Long> fnDeferring = (words, wordTrueCount) -> {
//words.forEach(word -> System.out.println(" \""+word+"\","));
			waiter.assertEquals(9, words.size());
			waiter.assertEquals(9l, wordTrueCount);
			List<String> expected = Arrays.asList("a", "b", "c", "ab", "ac", "bc", "cc", "abc", "acc");
			waiter.assertEquals(expected, words);
			waiter.resume();
		};
		wordGenerator.applyCompoundRules(line, fnDeferring, 37);

		waiter.await(200_000l);
	}

}
