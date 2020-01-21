package unit731.hunlinter.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.affix.ConversionTable;
import unit731.hunlinter.services.FileHelper;


class DictionaryParserTest{

	private final AffixParser affParser = new AffixParser();


	@Test
	void replacementTable() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"REP 4",
			"REP ^b bb",
			"REP e$ ee",
			"REP ij IJ",
			"REP alot a_lot");
		affParser.parse(affFile, language);

		ConversionTable table = affParser.getAffixData().getData(AffixOption.REPLACEMENT_TABLE);
		Assertions.assertEquals("[affixOption=REPLACEMENT_TABLE,table={  =[(ij,IJ), (alot,a lot)],  $=[(e$,ee)], ^ =[(^b,bb)]}]", table.toString());

		List<String> replaced = affParser.getAffixData().applyReplacementTable("clea");
		Assertions.assertTrue(replaced.isEmpty());

		replaced = affParser.getAffixData().applyReplacementTable("bcijde");
		Assertions.assertEquals(Arrays.asList("bbcijde", "bcijdee", "bcIJde"), replaced);

		replaced = affParser.getAffixData().applyReplacementTable("alot");
		Assertions.assertEquals(Collections.singletonList("a lot"), replaced);
	}

	@Test
	void applyLongest() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"REP 3",
			"REP b 1",
			"REP bac 3",
			"REP ba 2");
		affParser.parse(affFile, language);

		List<String> replaced = affParser.getAffixData().applyReplacementTable("abacc");
		Assertions.assertEquals(Arrays.asList("a1acc", "a3c", "a2cc"), replaced);
	}

	@Test
	void applyLongestOnStart() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"REP 3",
			"REP ^b 1",
			"REP ^bac 3",
			"REP ^ba 2");
		affParser.parse(affFile, language);

		List<String> replaced = affParser.getAffixData().applyReplacementTable("bacc");
		Assertions.assertEquals(Arrays.asList("1acc", "3c", "2cc"), replaced);
	}

	@Test
	void applyLongestOnEnd() throws IOException{
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"REP 3",
			"REP b$ 1",
			"REP cab$ 3",
			"REP ab$ 2");
		affParser.parse(affFile, language);

		List<String> replaced = affParser.getAffixData().applyReplacementTable("ccab");
		Assertions.assertEquals(Arrays.asList("cca1", "c3", "cc2"), replaced);
	}

}