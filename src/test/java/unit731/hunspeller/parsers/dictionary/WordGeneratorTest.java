package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.FileService;


public class WordGeneratorTest{

	@Test
	public void stems() throws IOException{
		String content = "SET UTF-8\n"
			+ "SFX A Y 1\n"
			+ "SFX A 0 a\n"
			+ "SFX B Y 1\n"
			+ "SFX B 0 b/A\n"
			+ "SFX C Y 1\n"
			+ "SFX C 0 c/E\n"
			+ "SFX D Y 1\n"
			+ "SFX D 0 d/AE\n"
			+ "PFX E Y 1\n"
			+ "PFX E 0 e\n"
			+ "PFX F Y 1\n"
			+ "PFX F 0 f/A\n"
			+ "PFX G Y 1\n"
			+ "PFX G 0 g/E\n"
			+ "PFX H Y 1\n"
			+ "PFX H 0 h/AE";
		File affFile = FileService.getTemporaryUTF8File(content);
		AffixParser parser = new AffixParser();
		parser.parse(affFile);
		WordGenerator generator = new WordGenerator(parser);
		String line = "a/ABCDEFGH";
		FlagParsingStrategy strategy = parser.getFlagParsingStrategy();
		DictionaryEntry dicEntry = new DictionaryEntry(line, strategy);

		List<RuleProductionEntry> stems = generator.applyRules(dicEntry);

		Assert.assertEquals(35, stems.size());
		Assert.assertEquals(new RuleProductionEntry("a", "ABCDEFGH", strategy), stems.get(0));
		Assert.assertEquals(new RuleProductionEntry("aa", "EFGH", strategy), stems.get(1));
		Assert.assertEquals(new RuleProductionEntry("ab", "AEFGH", strategy), stems.get(2));
		Assert.assertEquals(new RuleProductionEntry("ac", "EFGH", strategy), stems.get(3));
		Assert.assertEquals(new RuleProductionEntry("ad", "AEFGH", strategy), stems.get(4));
		Assert.assertEquals(new RuleProductionEntry("aba", "EFGH", strategy), stems.get(5));
		Assert.assertEquals(new RuleProductionEntry("ada", "EFGH", strategy), stems.get(6));
		Assert.assertEquals(new RuleProductionEntry("ea", "ABCD", strategy), stems.get(7));
		Assert.assertEquals(new RuleProductionEntry("fa", "ABCD", strategy), stems.get(8));
		Assert.assertEquals(new RuleProductionEntry("ga", "EABCD", strategy), stems.get(9));
		Assert.assertEquals(new RuleProductionEntry("ha", "AEBCD", strategy), stems.get(10));
		Assert.assertEquals(new RuleProductionEntry("eaa", "", strategy), stems.get(11));
		Assert.assertEquals(new RuleProductionEntry("faa", "A", strategy), stems.get(12));
		Assert.assertEquals(new RuleProductionEntry("gaa", "E", strategy), stems.get(13));
		Assert.assertEquals(new RuleProductionEntry("haa", "AE", strategy), stems.get(14));
		Assert.assertEquals(new RuleProductionEntry("eab", "A", strategy), stems.get(15));
		Assert.assertEquals(new RuleProductionEntry("fab", "A", strategy), stems.get(16));
		Assert.assertEquals(new RuleProductionEntry("gab", "EA", strategy), stems.get(17));
		Assert.assertEquals(new RuleProductionEntry("hab", "AE", strategy), stems.get(18));
		Assert.assertEquals(new RuleProductionEntry("eac", "", strategy), stems.get(19));
		Assert.assertEquals(new RuleProductionEntry("fac", "A", strategy), stems.get(20));
		Assert.assertEquals(new RuleProductionEntry("gac", "E", strategy), stems.get(21));
		Assert.assertEquals(new RuleProductionEntry("hac", "AE", strategy), stems.get(22));
		Assert.assertEquals(new RuleProductionEntry("ead", "A", strategy), stems.get(23));
		Assert.assertEquals(new RuleProductionEntry("fad", "A", strategy), stems.get(24));
		Assert.assertEquals(new RuleProductionEntry("gad", "EA", strategy), stems.get(25));
		Assert.assertEquals(new RuleProductionEntry("had", "AE", strategy), stems.get(26));
		Assert.assertEquals(new RuleProductionEntry("eaba", "", strategy), stems.get(27));
		Assert.assertEquals(new RuleProductionEntry("faba", "A", strategy), stems.get(28));
		Assert.assertEquals(new RuleProductionEntry("gaba", "E", strategy), stems.get(29));
		Assert.assertEquals(new RuleProductionEntry("haba", "AE", strategy), stems.get(30));
		Assert.assertEquals(new RuleProductionEntry("eada", "", strategy), stems.get(31));
		Assert.assertEquals(new RuleProductionEntry("fada", "A", strategy), stems.get(32));
		Assert.assertEquals(new RuleProductionEntry("gada", "E", strategy), stems.get(33));
		Assert.assertEquals(new RuleProductionEntry("hada", "AE", strategy), stems.get(34));
	}

}
