package unit731.hunspeller.parsers.hyphenation;

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorFactory;
import unit731.hunspeller.parsers.hyphenation.vos.HyphenationOptionsParser;
import unit731.hunspeller.services.PatternHelper;


public class HyphenationParserTest{

	private static final Pattern PATTERN_CLEANER = PatternHelper.pattern("\\d|/.+$");


	@Test
	public void noHyphenationDueToLeftMin(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "a1bc");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 2");
		optParser.parseLine("RIGHTHYPHENMIN 0");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, null, optParser);

		check(parser, "abc", "abc");
	}

	@Test
	public void noHyphenationDueToRightMin(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "ab1c");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 0");
		optParser.parseLine("RIGHTHYPHENMIN 2");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, null, optParser);

		check(parser, "abc", "abc");
	}

	@Test
	public void hyphenationOkLeftMin(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "a1bc");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 0");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, null, optParser);

		check(parser, "abc", "a", "bc");
	}

	@Test
	public void hyphenationOkRightMin(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "ab1c");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 0");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, null, optParser);

		check(parser, "abc", "ab", "c");
	}

	@Test
	public void augmentedWithRemovalBeforeHyphen(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "aa1tje/=,2,1");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "du", allPatterns, null, optParser);

		check(parser, "omaatje", "oma", "tje");
	}

	@Test
	public void augmentedWithIndexes(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "1–/–=,1,1");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, null, optParser);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedWithoutIndexes(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "1–/–=");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, null, optParser);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedAfterBreak(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "–1/–=–");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, null, optParser);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedAfterBreakWithRuleOverlap(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "–3/–=–");
		addRule(hyphenations, "1c");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, null, optParser);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedAfterBreak2(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "1k");
		addRule(hyphenations, "–1/–=–");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, null, optParser);

		check(parser, "kuko–fu", "ku", "ko–", "–fu");
	}

	@Test
	public void augmentedNonWordInitial(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "eigh1teen/ht=t,4,2");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "en", allPatterns, null, optParser);

		check(parser, "eighteen", "eight", "teen");
	}

	@Test
	public void augmentedWordInitial(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, ".schif1fahrt/ff=f,5,2");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "de", allPatterns, null, optParser);

		check(parser, "schiffahrt", "schiff", "fahrt");
	}

	@Test
	public void augmentedBase(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "c1k/k=k");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "de", allPatterns, null, optParser);

		check(parser, "Zucker", "Zuk", "ker");
	}

	@Test
	public void customHyphenation(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		Map<HyphenationParser.Level, Map<String, String>> custom = new HashMap<>();
		Map<String, String> custom1stLevel = new HashMap<>();
		custom1stLevel.put("abcd", "ab=cd");
		custom.put(HyphenationParser.Level.NON_COMPOUND, custom1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, custom, optParser);

		check(parser, "abcd", "ab", "cd");
	}

	@Test
	public void competingRules(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "ab1c");
		addRule(hyphenations, "2c");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "vec", allPatterns, null, optParser);

		check(parser, "abc", "abc");
	}


	/** German pre-reform hyphenation: Schiffahrt -> Schiff-fahrt */
	@Test
	public void germanPreReform(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "f1f");
		addRule(hyphenations, "if3fa/ff=f,2,2");
		addRule(hyphenations, "tenerif5fa");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "de", allPatterns, null, optParser);

		check(parser, "schiffen", "schif", "fen");
		check(parser, "schiffahrt", "schiff", "fahrt");
		check(parser, "teneriffa", "tenerif", "fa");
	}

	/** Hungarian simplified double 2-character consonants: ssz -> sz-sz, nny -> ny-ny */
	@Test
	public void hungarianSimplifiedDoubleConsonants(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "s1sz/sz=sz,1,3");
		addRule(hyphenations, "n1ny/ny=ny,1,3");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "hu", allPatterns, null, optParser);

		check(parser, "asszonnyal", "asz", "szony", "nyal");
	}

	/** Dutch: omaatje -> oma-tje */
	@Test
	public void dutch1(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "aa1tje./=,2,1");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "nl", allPatterns, null, optParser);

		check(parser, "omaatje", "oma", "tje");
	}

	/** Dutch: omaatje -> oma-tje */
	@Test
	public void dutch2(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "aa1tje./a=tje,1,5");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "nl", allPatterns, null, optParser);

		check(parser, "omaatje", "oma", "tje");
	}

	@Test
	public void french(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "xé1ém/á=a,2,2");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "fr", allPatterns, null, optParser);

		check(parser, "exéémple", "exá", "ample");
		check(parser, "exéémplxééme", "exá", "amplxá", "ame");
	}

	@Test
	public void baseAlt(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "l·1l/l=l,1,3");
		addRule(hyphenations, "a1atje./a=t,1,3");
		addRule(hyphenations, "e1etje./é=tje,1,5");
		addRule(hyphenations, ".schif1fahrt/ff=f,5,2");
		addRule(hyphenations, "c1k/k=k,1,2");
		addRule(hyphenations, "d1dzsel./dzs=dzs,1,4");
		addRule(hyphenations, ".as3szon/sz=sz,2,3");
		addRule(hyphenations, "n1nyal./ny=ny,1,3");
		addRule(hyphenations, ".til1lata./ll=l,3,2");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "xx", allPatterns, null, optParser);

		check(parser, "paral·lel", "paral", "lel");
		check(parser, "omaatje", "oma", "tje");
		check(parser, "cafeetje", "café", "tje");
		check(parser, "schiffahrt", "schiff", "fahrt");
		check(parser, "drucker", "druk", "ker");
		check(parser, "briddzsel", "bridzs", "dzsel");
		check(parser, "asszonnyal", "asz", "szony", "nyal");
		check(parser, "tillata", "till", "lata");
	}

	@Test
	public void englishCompound1(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, "motor1cycle");
		patterns1stLevel.build(hyphenations1stLevel);
		AhoCorasickDoubleArrayTrie<String> patterns2ndLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, ".mo1tor.");
		addRule(hyphenations2ndLevel, ".cy1cle.");
		//check independency of the 1st and 2nd hyphenation levels
		addRule(hyphenations2ndLevel, ".motor2cycle.");
		patterns2ndLevel.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 2");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 3");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "en", allPatterns, null, optParser);

		check(parser, "motorcycle", "mo", "tor", "cy", "cle");
	}

	@Test
	public void englishCompound2(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, "motor1cycle");
		patterns1stLevel.build(hyphenations1stLevel);
		AhoCorasickDoubleArrayTrie<String> patterns2ndLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, ".mo1tor.");
		addRule(hyphenations2ndLevel, ".cy1cle.");
		//check independency of the 1st and 2nd hyphenation levels
		addRule(hyphenations2ndLevel, ".motor2cycle.");
		patterns2ndLevel.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 3");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 4");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "en", allPatterns, null, optParser);

		check(parser, "motorcycle", "motor", "cycle");
	}

	@Test
	public void compound2(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, "szony1fő");
		addRule(hyphenations1stLevel, "ök1assz");
		patterns1stLevel.build(hyphenations1stLevel);
		AhoCorasickDoubleArrayTrie<String> patterns2ndLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, ".as1szony./sz=,2,1");
		addRule(hyphenations2ndLevel, ".fő1nök.");
		patterns2ndLevel.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "hu", allPatterns, null, optParser);

		check(parser, "főnökasszony", "fő", "nök", "asz", "szony");
		check(parser, "asszonyfőnök", "asz", "szony", "fő", "nök");
	}

	/**
	 * Norwegian: non-standard hyphenation at compound boundary (kilowattime -> kilowatt-time)
	 * and recursive compound hyphenation (kilowatt->kilo-watt)
	 */
	@Test
	public void compound3(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, "wat1time/tt=t,3,2");
		addRule(hyphenations1stLevel, ".kilo1watt");
		patterns1stLevel.build(hyphenations1stLevel);
		AhoCorasickDoubleArrayTrie<String> patterns2ndLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, ".ki1lo.");
		addRule(hyphenations2ndLevel, ".ti1me.");
		patterns2ndLevel.build(hyphenations1stLevel);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "no", allPatterns, null, optParser);

		check(parser, "kilowattime", "ki", "lo", "watt", "ti", "me");
	}

	@Test
	public void compound5(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, ".post1");
		patterns1stLevel.build(hyphenations1stLevel);
		AhoCorasickDoubleArrayTrie<String> patterns2ndLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, "e1");
		addRule(hyphenations2ndLevel, "a1");
		patterns2ndLevel.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "xx", allPatterns, null, optParser);

		check(parser, "postea", "post", "e", "a");
	}

	@Test
	public void compound6(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, "1que.");
		patterns1stLevel.build(hyphenations1stLevel);
		AhoCorasickDoubleArrayTrie<String> patterns2ndLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, "e1");
		patterns2ndLevel.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "xx", allPatterns, null, optParser);

		check(parser, "meaque", "me", "a", "que");
	}

	@Test
	public void noHyphen1(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "1_1");
		addRule(hyphenations, "1" + HyphenationParser.MINUS_SIGN + "1");
		addRule(hyphenations, "1" + HyphenationParser.APOSTROPHE + "1");
		addRule(hyphenations, "1" + HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK + "1");
		patterns1stLevel.build(hyphenations);
		AhoCorasickDoubleArrayTrie<String> patterns2ndLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 1");
		optParser.parseLine("NOHYPHEN ^_,_$,-,'," + HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK);
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "xx", allPatterns, null, optParser);

		check(parser, "_foobara'foobarb-foo_barc\u2019foobard_", "_foobara'foobarb-foo", "_", "barc\u2019foobard_");
	}

	@Test
	public void noHyphen2(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "1_1");
		addRule(hyphenations, "1" + HyphenationParser.MINUS_SIGN + "1");
		addRule(hyphenations, "1" + HyphenationParser.APOSTROPHE + "1");
		addRule(hyphenations, "1" + HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK + "1");
		patterns1stLevel.build(hyphenations);
		AhoCorasickDoubleArrayTrie<String> patterns2ndLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 1");
		optParser.parseLine("NOHYPHEN -,',=," + HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK);
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "xx", allPatterns, null, optParser);

		check(parser, "=foobara'foobarb-foo_barc\u2019foobard=", "=foobara'foobarb-foo", "_", "barc\u2019foobard=");
	}

	/** Unicode ligature hyphenation (ffi -> f=fi) */
	@Test
	public void ligature(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "ﬃ1/f=ﬁ,1,1");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "xx", allPatterns, null, optParser);

		check(parser, "maﬃa", "maf", "ﬁa");
		check(parser, "maﬃaﬃa", "maf", "ﬁaf", "ﬁa");
	}

		@Test
	public void settings(){
		AhoCorasickDoubleArrayTrie<String> patterns1stLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "ő1");
		patterns1stLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "xx", allPatterns, null, optParser);

		check(parser, "őőőőőőő", "őő", "ő", "ő", "ő", "őő");
	}

	@Test
	public void unicode(){
		AhoCorasickDoubleArrayTrie<String> patterns2ndLevel = new AhoCorasickDoubleArrayTrie<>();
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "l·1l/l=l,1,3");
		addRule(hyphenations, "e1ë/e=e,1,2");
		addRule(hyphenations, "a1atje./a=t,1,3");
		addRule(hyphenations, "e1etje./é=tje,1,5");
		addRule(hyphenations, "eigh1teen/t=t,5,1");
		addRule(hyphenations, ".schif1fahrt/ff=f,5,2");
		addRule(hyphenations, "c1k/k=k,1,2");
		addRule(hyphenations, "1ΐ/=ί,1,1");
		addRule(hyphenations, "d1dzsel./dzs=dzs,1,4");
		addRule(hyphenations, ".as3szon/sz=sz,2,3");
		addRule(hyphenations, "n1nyal./ny=ny,1,3");
		addRule(hyphenations, "bus1s/ss=s,3,2");
		addRule(hyphenations, "7-/=-,1,1");
		addRule(hyphenations, ".til1låta./ll=l,3,2");
		patterns2ndLevel.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickDoubleArrayTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser(HyphenatorFactory.Type.STANDARD, "xx", allPatterns, null, optParser);

		check(parser, "paral·lel", "paral", "lel");
		check(parser, "reëel", "re", "eel");
		check(parser, "omaatje", "oma", "tje");
		check(parser, "cafeetje", "café", "tje");
		check(parser, "eighteen", "eight", "teen");
		check(parser, "drucker", "druk", "ker");
		check(parser, "schiffahrt", "schiff", "fahrt");
		check(parser, "Μαΐου", "Μα", "ίου");
		check(parser, "asszonnyal", "asz", "szony", "nyal");
		check(parser, "briddzsel", "bridzs", "dzsel");
		check(parser, "bussjåfør", "buss", "sjåfør");
		check(parser, "100-sekundowy", "100", "-sekundowy");
		check(parser, "tillåta", "till", "låta");
	}


	private void addRule(Map<String, String> hyphenations, String rule){
		hyphenations.put(getKeyFromData(rule), rule);
	}

	private String getKeyFromData(String rule){
		return PatternHelper.replaceAll(rule, PATTERN_CLEANER, StringUtils.EMPTY);
	}

	private void check(HyphenationParser parser, String word, String ... hyphs){
		HyphenatorInterface hyphenator = HyphenatorFactory.createHyphenator(parser, HyphenationParser.BREAK_CHARACTER);
		Hyphenation hyphenation = hyphenator.hyphenate(word);

		Assertions.assertEquals(Arrays.asList(hyphs), hyphenation.getSyllabes());
	}

}
