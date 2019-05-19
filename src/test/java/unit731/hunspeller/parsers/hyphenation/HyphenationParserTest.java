package unit731.hunspeller.parsers.hyphenation;

import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.collections.ahocorasicktrie.AhoCorasickTrie;
import unit731.hunspeller.collections.ahocorasicktrie.AhoCorasickTrieBuilder;
import unit731.hunspeller.parsers.hyphenation.hyphenators.Hyphenator;
import unit731.hunspeller.parsers.hyphenation.vos.HyphenationOptionsParser;
import unit731.hunspeller.services.PatternHelper;


public class HyphenationParserTest{

	private static final Pattern PATTERN_CLEANER = PatternHelper.pattern("\\d|/.+$");


	@Test
	public void noHyphenationDueToLeftMin(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "a1bc");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 2");
		optParser.parseLine("RIGHTHYPHENMIN 0");
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, optParser);

		check(parser, "abc", "abc");
	}

	@Test
	public void noHyphenationDueToRightMin(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "ab1c");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 0");
		optParser.parseLine("RIGHTHYPHENMIN 2");
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, optParser);

		check(parser, "abc", "abc");
	}

	@Test
	public void hyphenationOkLeftMin(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "a1bc");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 0");
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, optParser);

		check(parser, "abc", "a", "bc");
	}

	@Test
	public void hyphenationOkRightMin(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "ab1c");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 0");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, optParser);

		check(parser, "abc", "ab", "c");
	}

	@Test
	public void augmentedWithRemovalBeforeHyphen(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "aa1tje/=,2,1");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("du", allPatterns, null, optParser);

		check(parser, "omaatje", "oma", "tje");
	}

	@Test
	public void augmentedWithIndexes(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "1–/–=,1,1");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, optParser);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedWithoutIndexes(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "1–/–=");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, optParser);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedAfterBreak(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "–1/–=–");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, optParser);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedAfterBreakWithRuleOverlap(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "–3/–=–");
		addRule(hyphenations, "1c");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, optParser);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedAfterBreak2(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "1k");
		addRule(hyphenations, "–1/–=–");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, optParser);

		check(parser, "kuko–fu", "ku", "ko–", "–fu");
	}

	@Test
	public void augmentedNonWordInitial(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "eigh1teen/ht=t,4,2");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("en", allPatterns, null, optParser);

		check(parser, "eighteen", "eight", "teen");
	}

	@Test
	public void augmentedWordInitial(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, ".schif1fahrt/ff=f,5,2");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("de", allPatterns, null, optParser);

		check(parser, "schiffahrt", "schiff", "fahrt");
	}

	@Test
	public void augmentedBase(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "c1k/k=k");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("de", allPatterns, null, optParser);

		check(parser, "Zucker", "Zuk", "ker");
	}

	@Test
	public void customHyphenation(){
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(Collections.<String, String>emptyMap());
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		Map<HyphenationParser.Level, Map<String, String>> custom = new HashMap<>();
		Map<String, String> custom1stLevel = new HashMap<>();
		custom1stLevel.put("abcd", "ab=cd");
		custom.put(HyphenationParser.Level.NON_COMPOUND, custom1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, custom, optParser);

		check(parser, "abcd", "ab", "cd");
	}

	@Test
	public void competingRules(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "ab1c");
		addRule(hyphenations, "2c");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, optParser);

		check(parser, "abc", "abc");
	}


	/** German pre-reform hyphenation: Schiffahrt -> Schiff-fahrt */
	@Test
	public void germanPreReform(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "f1f");
		addRule(hyphenations, "if3fa/ff=f,2,2");
		addRule(hyphenations, "tenerif5fa");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("de", allPatterns, null, optParser);

		check(parser, "schiffen", "schif", "fen");
		check(parser, "schiffahrt", "schiff", "fahrt");
		check(parser, "teneriffa", "tenerif", "fa");
	}

	/** Hungarian simplified double 2-character consonants: ssz -> sz-sz, nny -> ny-ny */
	@Test
	public void hungarianSimplifiedDoubleConsonants(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "s1sz/sz=sz,1,3");
		addRule(hyphenations, "n1ny/ny=ny,1,3");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("hu", allPatterns, null, optParser);

		check(parser, "asszonnyal", "asz", "szony", "nyal");
	}

	/** Dutch: omaatje -> oma-tje */
	@Test
	public void dutch1(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "aa1tje./=,2,1");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("nl", allPatterns, null, optParser);

		check(parser, "omaatje", "oma", "tje");
	}

	/** Dutch: omaatje -> oma-tje */
	@Test
	public void dutch2(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "aa1tje./a=tje,1,5");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("nl", allPatterns, null, optParser);

		check(parser, "omaatje", "oma", "tje");
	}

	@Test
	public void french(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "xé1ém/á=a,2,2");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("fr", allPatterns, null, optParser);

		check(parser, "exéémple", "exá", "ample");
		check(parser, "exéémplxééme", "exá", "amplxá", "ame");
	}

	@Test
	public void baseAlt(){
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
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, optParser);

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
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, "motor1cycle");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations1stLevel);
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, ".mo1tor.");
		addRule(hyphenations2ndLevel, ".cy1cle.");
		//check independency of the 1st and 2nd hyphenation levels
		addRule(hyphenations2ndLevel, ".motor2cycle.");
		AhoCorasickTrie<String> patterns2ndLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 2");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 3");
		HyphenationParser parser = new HyphenationParser("en", allPatterns, null, optParser);

		check(parser, "motorcycle", "mo", "tor", "cy", "cle");
	}

	@Test
	public void englishCompound2(){
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, "motor1cycle");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations1stLevel);
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, ".mo1tor.");
		addRule(hyphenations2ndLevel, ".cy1cle.");
		//check independency of the 1st and 2nd hyphenation levels
		addRule(hyphenations2ndLevel, ".motor2cycle.");
		AhoCorasickTrie<String> patterns2ndLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 3");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 4");
		HyphenationParser parser = new HyphenationParser("en", allPatterns, null, optParser);

		check(parser, "motorcycle", "motor", "cycle");
	}

	@Test
	public void compound2(){
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, "szony1fő");
		addRule(hyphenations1stLevel, "ök1assz");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations1stLevel);
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, ".as1szony./sz=,2,1");
		addRule(hyphenations2ndLevel, ".fő1nök.");
		AhoCorasickTrie<String> patterns2ndLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("hu", allPatterns, null, optParser);

		check(parser, "főnökasszony", "fő", "nök", "asz", "szony");
		check(parser, "asszonyfőnök", "asz", "szony", "fő", "nök");
	}

	/**
	 * Norwegian: non-standard hyphenation at compound boundary (kilowattime -> kilowatt-time)
	 * and recursive compound hyphenation (kilowatt->kilo-watt)
	 */
	@Test
	public void compound3(){
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, "wat1time/tt=t,3,2");
		addRule(hyphenations1stLevel, ".kilo1watt");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations1stLevel);
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, ".ki1lo.");
		addRule(hyphenations2ndLevel, ".ti1me.");
		AhoCorasickTrie<String> patterns2ndLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("no", allPatterns, null, optParser);

		check(parser, "kilowattime", "ki", "lo", "watt", "ti", "me");
	}

	@Test
	public void compound5(){
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, ".post1");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations1stLevel);
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, "e1");
		addRule(hyphenations2ndLevel, "a1");
		AhoCorasickTrie<String> patterns2ndLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, optParser);

		check(parser, "postea", "post", "e", "a");
	}

	@Test
	public void compound6(){
		Map<String, String> hyphenations1stLevel = new HashMap<>();
		addRule(hyphenations1stLevel, "1que.");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations1stLevel);
		Map<String, String> hyphenations2ndLevel = new HashMap<>();
		addRule(hyphenations2ndLevel, "e1");
		AhoCorasickTrie<String> patterns2ndLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations2ndLevel);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 1");
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, optParser);

		check(parser, "meaque", "me", "a", "que");
	}

	@Test
	public void noHyphen1(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "1_1");
		addRule(hyphenations, "1" + HyphenationParser.MINUS_SIGN + "1");
		addRule(hyphenations, "1" + HyphenationParser.APOSTROPHE + "1");
		addRule(hyphenations, "1" + HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK + "1");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		AhoCorasickTrie<String> patterns2ndLevel = new AhoCorasickTrieBuilder<String>()
			.build(Collections.<String, String>emptyMap());
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 1");
		optParser.parseLine("NOHYPHEN ^_,_$,-,'," + HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK);
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, optParser);

		check(parser, "_foobara'foobarb-foo_barc\u2019foobard_", "_foobara'foobarb-foo", "_", "barc\u2019foobard_");
	}

	@Test
	public void noHyphen2(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "1_1");
		addRule(hyphenations, "1" + HyphenationParser.MINUS_SIGN + "1");
		addRule(hyphenations, "1" + HyphenationParser.APOSTROPHE + "1");
		addRule(hyphenations, "1" + HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK + "1");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		AhoCorasickTrie<String> patterns2ndLevel = new AhoCorasickTrieBuilder<String>()
			.build(Collections.<String, String>emptyMap());
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		optParser.parseLine("LEFTHYPHENMIN 1");
		optParser.parseLine("RIGHTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDLEFTHYPHENMIN 1");
		optParser.parseLine("COMPOUNDRIGHTHYPHENMIN 1");
		optParser.parseLine("NOHYPHEN -,',=," + HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK);
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, optParser);

		check(parser, "=foobara'foobarb-foo_barc\u2019foobard=", "=foobara'foobarb-foo", "_", "barc\u2019foobard=");
	}

	/** Unicode ligature hyphenation (ffi -> f=fi) */
	@Test
	public void ligature(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "ﬃ1/f=ﬁ,1,1");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, optParser);

		check(parser, "maﬃa", "maf", "ﬁa");
		check(parser, "maﬃaﬃa", "maf", "ﬁaf", "ﬁa");
	}

		@Test
	public void settings(){
		Map<String, String> hyphenations = new HashMap<>();
		addRule(hyphenations, "ő1");
		AhoCorasickTrie<String> patterns1stLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.NON_COMPOUND, patterns1stLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, optParser);

		check(parser, "őőőőőőő", "őő", "ő", "ő", "ő", "őő");
	}

	@Test
	public void unicode(){
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
		AhoCorasickTrie<String> patterns2ndLevel = new AhoCorasickTrieBuilder<String>()
			.build(hyphenations);
		Map<HyphenationParser.Level, AhoCorasickTrie<String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns2ndLevel);
		HyphenationOptionsParser optParser = new HyphenationOptionsParser();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, optParser);

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
		HyphenatorInterface hyphenator = new Hyphenator(parser, HyphenationParser.BREAK_CHARACTER);
		Hyphenation hyphenation = hyphenator.hyphenate(word);

		Assertions.assertEquals(Arrays.asList(hyphs), hyphenation.getSyllabes());
	}

}
