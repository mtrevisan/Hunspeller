package unit731.hunlinter.parsers.dictionary.generators;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.vos.Affixes;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.ArraySet;
import unit731.hunlinter.workers.dictionary.DictionaryInclusionTestWorker;
import unit731.hunlinter.services.SetHelper;
import unit731.hunlinter.services.text.StringHelper;

import static unit731.hunlinter.services.system.LoopHelper.collectIf;
import static unit731.hunlinter.services.system.LoopHelper.forEach;
import static unit731.hunlinter.services.system.LoopHelper.match;
import static unit731.hunlinter.services.system.LoopHelper.removeIf;


abstract class WordGeneratorCompound extends WordGeneratorBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordGeneratorCompound.class);

	private static final Map<StringHelper.Casing, Set<StringHelper.Casing>> COMPOUND_WORD_BOUNDARY_COLLISIONS
		= new EnumMap<>(StringHelper.Casing.class);
	static{
		final Set<StringHelper.Casing> lowerOrTitleCase = SetHelper.setOf(StringHelper.Casing.TITLE_CASE, StringHelper.Casing.ALL_CAPS,
			StringHelper.Casing.CAMEL_CASE, StringHelper.Casing.PASCAL_CASE);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.LOWER_CASE, lowerOrTitleCase);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.TITLE_CASE, lowerOrTitleCase);
		final Set<StringHelper.Casing> allCaps = SetHelper.setOf(StringHelper.Casing.LOWER_CASE, StringHelper.Casing.TITLE_CASE,
			StringHelper.Casing.CAMEL_CASE, StringHelper.Casing.PASCAL_CASE);
		COMPOUND_WORD_BOUNDARY_COLLISIONS.put(StringHelper.Casing.ALL_CAPS, allCaps);
	}


	protected final DictionaryParser dicParser;
	protected final WordGenerator wordGenerator;

	private DictionaryInclusionTestWorker dicInclusionTestWorker;
	private final Set<String> compoundAsReplacement = new HashSet<>();


	WordGeneratorCompound(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator){
		super(affixData);

		this.dicParser = dicParser;
		this.wordGenerator = wordGenerator;
	}

	protected List<List<Inflection[]>> generateCompounds(final List<List<String>> permutations,
			final Map<String, DictionaryEntry[]> inputs){
		final List<List<Inflection[]>> entries = new ArrayList<>();
		final Map<String, Inflection[]> dicEntries = new HashMap<>();
		outer:
		for(final List<String> permutation : permutations){
			//expand permutation
			final List<Inflection[]> expandedPermutationEntries = new ArrayList<>();
			for(final String flag : permutation){
				if(!dicEntries.containsKey(flag)){
					Inflection[] dicEntriesPerFlag = new Inflection[0];
					for(final DictionaryEntry entry : inputs.get(flag)){
						final Inflection[] inflections = applyAffixRules(entry, true, null);
						final Inflection[] collect = collectIf(inflections,
							inflection -> inflection.hasContinuationFlag(flag), () -> new Inflection[0]);
						dicEntriesPerFlag = ArrayUtils.addAll(dicEntriesPerFlag, collect);
					}
					dicEntries.put(flag, dicEntriesPerFlag);
				}

				final Inflection[] dicEntriesPerFlag = dicEntries.get(flag);
				if(dicEntriesPerFlag.length > 0)
					expandedPermutationEntries.add(dicEntriesPerFlag);
				else{
					//it is not possible to compound some words, return empty list
					entries.clear();
					break outer;
				}
			}
			if(!expandedPermutationEntries.isEmpty())
				entries.add(expandedPermutationEntries);
		}
		return entries;
	}

	protected Inflection[] applyCompound(final List<List<Inflection[]>> entries, final int limit){
		final String compoundFlag = affixData.getCompoundFlag();
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		final String forceCompoundUppercaseFlag = affixData.getForceCompoundUppercaseFlag();
		final boolean checkCompoundReplacement = affixData.isCheckCompoundReplacement();

		compoundAsReplacement.clear();

		final StringBuffer sb = new StringBuffer();
		final ArraySet<Inflection> inflections = new ArraySet<>();
		//generate compounds:
		for(final List<Inflection[]> entry : entries){
			//compose compound:
			boolean completed = false;
			final int[] indexes = new int[entry.size()];
			while(!completed){
				final DictionaryEntry[] compoundEntries = composeCompound(indexes, entry, sb);

				if(sb.length() > 0 && (!checkCompoundReplacement || !existsCompoundAsReplacement(sb.toString()))){
					final String[][] continuationFlags = extractCompoundFlagsByComponent(compoundEntries, compoundFlag);
					if(forbiddenWordFlag == null
							|| !ArrayUtils.contains(continuationFlags[Affixes.INDEX_PREFIXES], forbiddenWordFlag)
							&& !ArrayUtils.contains(continuationFlags[Affixes.INDEX_SUFFIXES], forbiddenWordFlag)
							&& !ArrayUtils.contains(continuationFlags[Affixes.INDEX_TERMINALS], forbiddenWordFlag)){
						final String compoundWord = sb.toString();
						final Inflection[] newInflections = generateInflections(compoundWord, compoundEntries, continuationFlags);
						final Inflection[] subInflections = ArrayUtils.subarray(newInflections,
							0, Math.min(limit - inflections.size(), newInflections.length));
						inflections.addAll(subInflections);
					}
				}


				completed = (inflections.size() == limit || getNextTuple(indexes, entry));
			}
		}

		compoundAsReplacement.clear();

		applyOutputConversions(inflections, forceCompoundUppercaseFlag);

		if(LOGGER.isTraceEnabled())
			forEach(inflections, inflection -> LOGGER.trace("Inflected word: {}", inflection));

		return limitResponse(inflections, limit);
	}

	private void applyOutputConversions(final Set<Inflection> inflections, final String forceCompoundUppercaseFlag){
		//convert using output table
		for(final Inflection inflection : inflections){
			inflection.applyOutputConversionTable(affixData::applyOutputConversionTable);
			inflection.capitalizeIfContainsFlag(forceCompoundUppercaseFlag);
			inflection.removeContinuationFlag(forceCompoundUppercaseFlag);
		}
	}

	private Inflection[] limitResponse(final Set<Inflection> inflections, final int limit){
		return (inflections.size() > limit?
			new ArrayList<>(inflections).subList(0, limit).toArray(Inflection[]::new):
			inflections.toArray(Inflection[]::new));
	}

	private Inflection[] generateInflections(final String compoundWord, final DictionaryEntry[] compoundEntries,
														  final String[][] continuationFlags){
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		final boolean hasForbidCompoundFlag = (affixData.getForbidCompoundFlag() != null);
		final boolean hasPermitCompoundFlag = (affixData.getPermitCompoundFlag() != null);
		final boolean allowTwofoldAffixesInCompound = affixData.allowTwofoldAffixesInCompound();

		Inflection[] inflections;
		final StringBuffer flags = new StringBuffer();
		forEach(continuationFlags, continuationFlag -> forEach(continuationFlag, flags::append));
		final Inflection p = Inflection.createFromCompound(compoundWord, flags.toString(), compoundEntries, strategy);
		if(hasForbidCompoundFlag || hasPermitCompoundFlag)
			inflections = new Inflection[]{p};
		else{
			//add boundary affixes
			inflections = applyAffixRules(p, false, null);

			if(!allowTwofoldAffixesInCompound)
				//remove twofold because they're not allowed in compounds
				inflections = removeTwofolds(inflections);
		}
		return inflections;
	}

	private DictionaryEntry[] composeCompound(final int[] indexes, final List<Inflection[]> entry, final StringBuffer sb){
		final String forbiddenWordFlag = affixData.getForbiddenWordFlag();
		final boolean forbidDifferentCasesInCompound = affixData.isForbidDifferentCasesInCompound();
		final boolean forbidTriples = affixData.isForbidTriplesInCompound();
		final boolean simplifyTriples = affixData.isSimplifyTriplesInCompound();

		DictionaryEntry[] compoundEntries = new DictionaryEntry[0];

		sb.setLength(0);
		StringHelper.Casing lastWordCasing = null;
		for(int i = 0; i < indexes.length; i ++){
			final Inflection next = entry.get(i)[indexes[i]];

			//skip forbidden words
			if(next.hasContinuationFlag(forbiddenWordFlag)){
				sb.setLength(0);
				break;
			}

			compoundEntries = ArrayUtils.add(compoundEntries, next);

			String nextCompound = next.getWord();
			final boolean containsTriple = containsTriple(sb, nextCompound);
			//enforce simplification of triples if SIMPLIFIEDTRIPLE is set
			if(containsTriple && simplifyTriples)
				nextCompound = nextCompound.substring(1);
			//enforce not containment of a triple if CHECKCOMPOUNDTRIPLE is set
			else if(containsTriple && forbidTriples){
				sb.setLength(0);
				break;
			}
			//enforce forbidden case if CHECKCOMPOUNDCASE is set
			if(sb.length() > 0 && forbidDifferentCasesInCompound){
				if(lastWordCasing == null)
					lastWordCasing = StringHelper.classifyCasing(sb.toString());
				final StringHelper.Casing nextWordCasing = StringHelper.classifyCasing(nextCompound);

				final char lastChar = sb.charAt(sb.length() - 1);
				//FIXME if nextCompound is changed, then check for duplicates
				nextCompound = enforceNextCompoundCase(lastChar, nextCompound, lastWordCasing, nextWordCasing);

				lastWordCasing = nextWordCasing;
			}

			sb.append(nextCompound);
		}
		return compoundEntries;
	}

	private boolean containsTriple(final StringBuffer sb, final String compound){
		boolean repeated = false;
		final int size = sb.length() - 1;
		if(size > 1){
			final String interCompounds = sb.substring(Math.max(size - 1, 0), size + 1) + compound.substring(0, Math.min(compound.length(), 2));
			final int len = interCompounds.length();
			if(len == 3 || len == 4){
				repeated = (interCompounds.charAt(0) == interCompounds.charAt(1) && interCompounds.charAt(0) == interCompounds.charAt(2));
				if(len == 4)
					repeated |= (interCompounds.charAt(1) == interCompounds.charAt(2) && interCompounds.charAt(1) == interCompounds.charAt(3));
			}
		}
		return repeated;
	}

	private String enforceNextCompoundCase(final char lastChar, String nextCompound, final StringHelper.Casing lastWordCasing,
			final StringHelper.Casing nextWordCasing){
		final char nextChar = nextCompound.charAt(0);
		if(Character.isLetter(lastChar) && Character.isLetter(nextChar)){
			final Set<StringHelper.Casing> collisions = COMPOUND_WORD_BOUNDARY_COLLISIONS.get(lastWordCasing);
			//convert nextChar to lowercase/uppercase and go on
			if(collisions != null && collisions.contains(nextWordCasing))
				nextCompound = (Character.isUpperCase(lastChar)? StringUtils.capitalize(nextCompound):
					StringUtils.uncapitalize(nextCompound));
		}
		return nextCompound;
	}

	/** @return	A list of prefixes from first entry, suffixes from last entry, and terminals from both */
	private String[][] extractCompoundFlagsByComponent(final DictionaryEntry[] compoundEntries, final String compoundFlag){
		final String[][] prefixes = compoundEntries[0].extractAllAffixes(affixData, false);
		final String[][] suffixes = compoundEntries[compoundEntries.length - 1].extractAllAffixes(affixData, false);
		final ArraySet<String> terminals = new ArraySet<>();
		terminals.addAll(prefixes[Affixes.INDEX_TERMINALS]);
		terminals.addAll(suffixes[Affixes.INDEX_TERMINALS]);
		terminals.remove(compoundFlag);

		return new String[][]{prefixes[Affixes.INDEX_PREFIXES], suffixes[Affixes.INDEX_SUFFIXES], terminals.toArray(String[]::new)};
	}

	private Inflection[] removeTwofolds(Inflection[] prods){
		final String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null)
			prods = removeIf(prods, prod -> prod.isTwofolded(circumfixFlag));
		return prods;
	}

	//is word a non–compound with a REP substitution (see checkcompoundrep)?
	private boolean existsCompoundAsReplacement(final String word){
		boolean exists = (match(compoundAsReplacement, word::contains) != null);
		if(!exists && word.length() >= 2){
			final List<String> conversions = affixData.applyReplacementTable(word);
			for(final String candidate : conversions)
				if(dicInclusionTestWorker.isInDictionary(candidate)){
					compoundAsReplacement.add(word);

					exists = true;
					break;
				}
		}
		return exists;
	}

	private boolean getNextTuple(final int[] indexes, final List<Inflection[]> entry){
		//obtain next tuple
		int i = indexes.length - 1;
		while(i >= 0){
			indexes[i] ++;
			if(indexes[i] < entry.get(i).length)
				break;

			indexes[i --] = 0;
		}
		return (i == -1);
	}

	/** Merge the distribution with the others */
	protected Map<String, DictionaryEntry[]> mergeDistributions(final Map<String, DictionaryEntry[]> compoundRules,
			final Map<String, DictionaryEntry[]> distribution, final int compoundMinimumLength, final String forbiddenWordFlag){
		final List<Map.Entry<String, DictionaryEntry[]>> list = new ArrayList<>(compoundRules.entrySet());
		list.addAll(distribution.entrySet());

		final Map<String, DictionaryEntry[]> map = new HashMap<>();
		for(final Map.Entry<String, DictionaryEntry[]> m : list){
			final DictionaryEntry[] entries = m.getValue();
			DictionaryEntry[] value = new DictionaryEntry[0];
			final int size = (entries != null? entries.length: 0);
			for(int i = 0; i < size; i ++){
				final DictionaryEntry entry = entries[i];
				if(entry.getWord().length() >= compoundMinimumLength && !entry.hasContinuationFlag(forbiddenWordFlag))
					value = ArrayUtils.add(value, entry);
			}
			final String key = m.getKey();
			final DictionaryEntry[] v = map.get(key);
			map.put(key, (v != null? ArrayUtils.addAll(v, value): value));
		}
		return map;
	}

	protected void loadDictionaryForInclusionTest(){
		if(dicInclusionTestWorker == null && affixData.isCheckCompoundReplacement()){
			dicInclusionTestWorker = new DictionaryInclusionTestWorker(affixData.getLanguage(), dicParser, wordGenerator);

			dicInclusionTestWorker.executeSynchronously();
		}
	}

}
