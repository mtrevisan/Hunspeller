package unit731.hunspeller.parsers.dictionary;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.services.FileHelper;


public class RulesReducerTest{

	@Test
	public void replacementTable() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"LANG vec",
			"FLAG long",
			"SFX ʼ0 Y 11",
			"SFX ʼ0 r ʼ r",
			"SFX ʼ0 u ʼ u",
			"SFX ʼ0 ra ʼ ra",
			"SFX ʼ0 xa ʼ xa",
			"SFX ʼ0 me ʼ me",
			"SFX ʼ0 te ʼ te",
			"SFX ʼ0 do ʼ do",
			"SFX ʼ0 a ʼ [^rx]a",
			"SFX ʼ0 e ʼ [^mtv]e",
			"SFX ʼ0 o ʼ [^d]o",
			"SFX ʼ0 ove óʼ ove");
		RulesReducer reducer = createReducer(affFile);
		List<RulesReducer.LineEntry> plainRules = Arrays.asList(
			new RulesReducer.LineEntry("e", "ʼ", "e", "ge"),
			new RulesReducer.LineEntry("a", "ʼ", "a", "la"),
			new RulesReducer.LineEntry("a", "ʼ", "a", "na"),
			new RulesReducer.LineEntry("u", "ʼ", "u", "nu"),
			new RulesReducer.LineEntry("u", "ʼ", "u", "vu"),
			new RulesReducer.LineEntry("e", "ʼ", "e", "ge"),
			new RulesReducer.LineEntry("o", "ʼ", "o", "sto"),
			new RulesReducer.LineEntry("ove", "óʼ", "ove", "adove"),
			new RulesReducer.LineEntry("ove", "óʼ", "ove", "indove"),
			new RulesReducer.LineEntry("me", "ʼ", "me", "kome"),
			new RulesReducer.LineEntry("do", "ʼ", "do", "kuando"),
			new RulesReducer.LineEntry("o", "ʼ", "o", "tuto"),
			new RulesReducer.LineEntry("e", "ʼ", "e", "de"),
			new RulesReducer.LineEntry("o", "ʼ", "o", "so"),
			new RulesReducer.LineEntry("ra", "ʼ", "ra", "sora"),
			new RulesReducer.LineEntry("o", "ʼ", "o", "tèrŧo"),
			new RulesReducer.LineEntry("o", "ʼ", "o", "tèrso"),
			new RulesReducer.LineEntry("o", "ʼ", "o", "kuarto"),
			new RulesReducer.LineEntry("o", "ʼ", "o", "koarto"),
			new RulesReducer.LineEntry("o", "ʼ", "o", "kuinto"),
			new RulesReducer.LineEntry("o", "ʼ", "o", "sèsto"),
			new RulesReducer.LineEntry("r", "ʼ", "r", "par"),
			new RulesReducer.LineEntry("xa", "ʼ", "xa", "kaxa"),
			new RulesReducer.LineEntry("a", "ʼ", "a", "sensa"),
			new RulesReducer.LineEntry("a", "ʼ", "a", "senŧa"),
			new RulesReducer.LineEntry("do", "ʼ", "do", "komòdo"),
			new RulesReducer.LineEntry("te", "ʼ", "te", "frate"),
			new RulesReducer.LineEntry("do", "ʼ", "do", "nudo")
		);
		List<RulesReducer.LineEntry> compactedRules = reducer.reduceProductions(plainRules);

		List<RulesReducer.LineEntry> expectedCompactedRules = Arrays.asList(
			new RulesReducer.LineEntry("ove", "óʼ", "ove", Arrays.asList("indove", "adove")),
			new RulesReducer.LineEntry("r", "ʼ", "r", "par"),
			new RulesReducer.LineEntry("u", "ʼ", "u", Arrays.asList("nu", "vu")),
			new RulesReducer.LineEntry("ra", "ʼ", "ra", "sora"),
			new RulesReducer.LineEntry("do", "ʼ", "do", Arrays.asList("nudo", "komòdo", "kuando")),
			new RulesReducer.LineEntry("te", "ʼ", "te", "frate"),
			new RulesReducer.LineEntry("xa", "ʼ", "xa", "kaxa"),
			new RulesReducer.LineEntry("me", "ʼ", "me", "kome"),
			new RulesReducer.LineEntry("o", "ʼ", "[^d]o", Arrays.asList("koarto", "kuinto", "kuarto", "sèsto", "tèrso", "tèrŧo", "tuto", "so", "sto")),
			new RulesReducer.LineEntry("e", "ʼ", "[dg]e", Arrays.asList("de", "ge")),
			new RulesReducer.LineEntry("a", "ʼ", "[^rx]a", Arrays.asList("sensa", "senŧa", "na", "la"))
		);
		Assertions.assertEquals(expectedCompactedRules, compactedRules);

		String flag = "ʼ0";
		List<String> rules = reducer.convertFormat(flag, false, compactedRules);
		List<String> expectedRules = Arrays.asList(
			"SFX ʼ0 Y 11",
			"SFX ʼ0 r ʼ r",
			"SFX ʼ0 u ʼ u",
			"SFX ʼ0 ra ʼ ra",
			"SFX ʼ0 xa ʼ xa",
			"SFX ʼ0 me ʼ me",
			"SFX ʼ0 te ʼ te",
			"SFX ʼ0 do ʼ do",
			"SFX ʼ0 a ʼ [^rx]a",
			"SFX ʼ0 e ʼ [dg]e",
			"SFX ʼ0 o ʼ [^d]o",
			"SFX ʼ0 ove óʼ ove"
		);
		Assertions.assertEquals(expectedRules, rules);
	}

	private RulesReducer createReducer(File affFile) throws IOException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile);
		AffixData affixData = affParser.getAffixData();
		File dicFile = FileHelper.getTemporaryUTF8File("xxx", ".dic",
			"0");
		DictionaryParser dicParser = new DictionaryParser(dicFile, affixData.getLanguage(), affixData.getCharset());
		WordGenerator wordGenerator = new WordGenerator(affixData, dicParser);
		return new RulesReducer(affixData, wordGenerator);
	}

}
