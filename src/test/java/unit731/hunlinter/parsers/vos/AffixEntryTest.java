package unit731.hunlinter.parsers.vos;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.affix.strategies.ParsingStrategyFactory;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.services.datastructures.GrowableArray;
import unit731.hunlinter.workers.exceptions.LinterException;


class AffixEntryTest{

	@Test
	void notValidSuffix1(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		String line = "SFX M0 b i a";
		Throwable exception = Assertions.assertThrows(LinterException.class,
			() -> new AffixEntry(line, AffixType.SUFFIX, "M0", strategy, null, null));
		Assertions.assertEquals("Condition part doesn't ends with removal part: '" + line + "'", exception.getMessage());
	}

	@Test
	void notValidSuffix2(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		String line = "SFX M0 a ai a";
		Throwable exception = Assertions.assertThrows(LinterException.class,
			() -> new AffixEntry(line, AffixType.SUFFIX, "M0", strategy, null, null));
		Assertions.assertEquals("Characters in common between removed and added part: '" + line + "'", exception.getMessage());
	}

	@Test
	void notValidPrefix1(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		String line = "PFX M0 b i a";
		Throwable exception = Assertions.assertThrows(LinterException.class,
			() -> new AffixEntry(line, AffixType.PREFIX, "M0", strategy, null, null));
		Assertions.assertEquals("Condition part doesn't starts with removal part: '" + line + "'", exception.getMessage());
	}

	@Test
	void notValidPrefix2(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		String line = "PFX M0 a ia a";
		Throwable exception = Assertions.assertThrows(LinterException.class,
			() -> new AffixEntry(line, AffixType.PREFIX, "M0", strategy, null, null));
		Assertions.assertEquals("Characters in common between removed and added part: '" + line + "'", exception.getMessage());
	}

	@Test
	void hasContinuationFlag(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", AffixType.SUFFIX, "M0", strategy, null, null);

		boolean matches = entry.hasContinuationFlag("A");

		Assertions.assertTrue(matches);
	}

	@Test
	void notHasContinuationFlag(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", AffixType.SUFFIX, "M0", strategy, null, null);

		boolean matches = entry.hasContinuationFlag("B");

		Assertions.assertFalse(matches);
	}

	@Test
	void combineContinuationFlags(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", AffixType.SUFFIX, "M0", strategy, null, null);

		GrowableArray<String> combinedFlags = entry.combineContinuationFlags(new String[]{"B", "A"});

		Assertions.assertArrayEquals(new String[]{"B", "A"}, combinedFlags.extractCopyOrNull());
	}

	@Test
	void isSuffix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M0", 'N');
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", parent.getType(), parent.getFlag(), strategy, null, null);
		parent.setEntries(entry);

		AffixType type = entry.getType();

		Assertions.assertEquals(AffixType.SUFFIX, type);
	}

	@Test
	void isPrefix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.PREFIX, "M0", 'N');
		AffixEntry entry = new AffixEntry("PFX M0 0 i/A [^oaie]", parent.getType(), parent.getFlag(), strategy, null, null);
		parent.setEntries(entry);

		AffixType type = entry.getType();

		Assertions.assertEquals(AffixType.PREFIX, type);
	}

	@Test
	void matchOk(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M0", 'N');
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", parent.getType(), parent.getFlag(), strategy, null, null);
		parent.setEntries(entry);

		boolean matches = entry.canApplyTo("man");

		Assertions.assertTrue(matches);
	}

	@Test
	void matchNotOk(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M0", 'N');
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", parent.getType(), parent.getFlag(), strategy, null, null);
		parent.setEntries(entry);

		boolean matches = entry.canApplyTo("mano");

		Assertions.assertFalse(matches);
	}

	@Test
	void applyRuleSuffix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M0", 'N');
		AffixEntry entry = new AffixEntry("SFX M0 0 i/A [^oaie]", parent.getType(), parent.getFlag(), strategy, null, null);
		parent.setEntries(entry);

		String inflection = entry.applyRule("man\\/man", true);

		Assertions.assertEquals("man\\/mani", inflection);
	}

	@Test
	void cannotApplyRuleSuffixFullstrip(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		AffixEntry entry = new AffixEntry("SFX M0 man i/A man", AffixType.SUFFIX, "M0", strategy, null, null);

		Throwable exception = Assertions.assertThrows(LinterException.class,
			() -> entry.applyRule("man", false));
		Assertions.assertEquals("Cannot strip full word 'man' without the FULLSTRIP option", exception.getMessage());
}

	@Test
	void applyRulePrefix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.PREFIX, "TB", 'N');
		AffixEntry entry = new AffixEntry("PFX TB ŧ s ŧ	po:noun", parent.getType(), parent.getFlag(), strategy, null, null);
		parent.setEntries(entry);

		String inflection = entry.applyRule("ŧinkue", true);

		Assertions.assertEquals("sinkue", inflection);
	}

	@Test
	void undoRuleSuffix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.SUFFIX, "M0", 'N');
		AffixEntry entry = new AffixEntry("SFX M0 0 i [^oaie]	po:noun", parent.getType(), parent.getFlag(), strategy, null, null);
		parent.setEntries(entry);

		String inflection = entry.undoRule("mani");

		Assertions.assertEquals("man", inflection);
	}

	@Test
	void undoRulePrefix(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.PREFIX, "TB", 'N');
		AffixEntry entry = new AffixEntry("PFX TB ŧ s ŧ	po:noun", parent.getType(), parent.getFlag(), strategy, null, null);
		parent.setEntries(entry);

		String inflection = entry.undoRule("sinkue");

		Assertions.assertEquals("ŧinkue", inflection);
	}

	@Test
	void testToString(){
		FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
		RuleEntry parent = new RuleEntry(AffixType.PREFIX, "TB", 'N');
		AffixEntry entry = new AffixEntry("PFX TB ŧ s ŧ	po:noun", parent.getType(), parent.getFlag(), strategy, null, null);
		parent.setEntries(entry);

		String representation = entry.toString();

		Assertions.assertEquals("PFX TB ŧ s ŧ po:noun", representation);
	}

}
