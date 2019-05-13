package unit731.hunspeller.parsers.dictionary.vos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.Affixes;
import unit731.hunspeller.parsers.dictionary.dtos.MorphologicalTag;
import unit731.hunspeller.services.PatternHelper;


public class DictionaryEntry{

	private static final int PARAM_WORD = 1;
	private static final int PARAM_FLAGS = 2;
	private static final int PARAM_MORPHOLOGICAL_FIELDS = 3;
	private static final Pattern PATTERN_ENTRY = PatternHelper.pattern("^(?<word>[^\\s]+?)(?:(?<!\\\\)\\/(?<flags>[^\\s]+))?(?:[\\s]+(?<morphologicalFields>.+))?$");

	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";
	private static final String TAB = "\t";
	private static final String COMMA = ",";


	protected String word;
	protected String[] continuationFlags;
	protected final String[] morphologicalFields;
	private final boolean combineable;


	public static DictionaryEntry createFromDictionaryLine(String line, FlagParsingStrategy strategy){
		return createFromDictionaryLineWithAliases(line, strategy, null, null);
	}

	public static DictionaryEntry createFromDictionaryLineWithAliases(String line, FlagParsingStrategy strategy, List<String> aliasesFlag,
			List<String> aliasesMorphologicaField){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		Matcher m = PATTERN_ENTRY.matcher(line);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse dictionary line '" + line + "'");

		String word = StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
		String dicFlags = m.group(PARAM_FLAGS);
		String[] continuationFlags = strategy.parseFlags(expandAliases(dicFlags, aliasesFlag));
		String dicMorphologicalFields = m.group(PARAM_MORPHOLOGICAL_FIELDS);
		String[] mfs = StringUtils.split(expandAliases(dicMorphologicalFields, aliasesMorphologicaField));
		String[] morphologicalFields = (containsStem(mfs)? mfs: ArrayUtils.addAll(new String[]{MorphologicalTag.TAG_STEM + word}, mfs));
		boolean combineable = true;
		return new DictionaryEntry(word, continuationFlags, morphologicalFields, combineable);
	}

	public static DictionaryEntry clone(DictionaryEntry dicEntry){
		return new DictionaryEntry(dicEntry);
	}

	protected DictionaryEntry(DictionaryEntry dicEntry){
		Objects.requireNonNull(dicEntry);

		word = dicEntry.word;
		continuationFlags = ArrayUtils.clone(dicEntry.continuationFlags);
		morphologicalFields = ArrayUtils.clone(dicEntry.morphologicalFields);
		combineable = dicEntry.combineable;
	}

	protected DictionaryEntry(String word, String[] continuationFlags, String[] morphologicalFields, boolean combineable){
		Objects.requireNonNull(word);

		this.word = word;
		this.continuationFlags = continuationFlags;
		this.morphologicalFields = morphologicalFields;
		this.combineable = combineable;
	}

	private static String expandAliases(String part, List<String> aliases) throws IllegalArgumentException{
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)? aliases.get(Integer.parseInt(part) - 1): part);
	}

	private static boolean containsStem(String[] mfs){
		boolean containsStem = false;
		if(mfs != null)
			for(String mf : mfs)
				if(mf.startsWith(MorphologicalTag.TAG_STEM)){
					containsStem = true;
					break;
				}
		return containsStem;
	}

	public static String extractWord(String line){
		Objects.requireNonNull(line);

		Matcher m = PATTERN_ENTRY.matcher(line);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse dictionary line '" + line + "'");

		return StringUtils.replace(m.group(PARAM_WORD), SLASH_ESCAPED, SLASH);
	}

	public String getWord(){
		return word;
	}

	public boolean isCombineable(){
		return combineable;
	}

	public void applyInputConversionTable(AffixData affixData){
		word = affixData.applyInputConversionTable(word);
	}

	public boolean removeContinuationFlag(String continuationFlagToRemove){
		boolean removed = false;
		if(continuationFlagToRemove != null && continuationFlags != null){
			int previousSize = continuationFlags.length;
			continuationFlags = ArrayUtils.removeElement(ArrayUtils.clone(continuationFlags), continuationFlagToRemove);

			removed = (continuationFlags.length != previousSize);

			if(continuationFlags.length == 0)
				continuationFlags = null;
		}
		return removed;
	}

	/**
	 * @param affixData	The Affix Data used to determine if a flag is a terminal
	 * @return	Whether there are continuation flags that are not terminal affixes
	 */
	public boolean hasNonTerminalContinuationFlags(AffixData affixData){
		if(continuationFlags != null)
			for(String flag : continuationFlags)
				if(!affixData.isTerminalAffix(flag))
					return true;
		return false;
	}

	public int getContinuationFlagCount(){
		return (continuationFlags != null? continuationFlags.length: 0);
	}

	public boolean hasContinuationFlag(String ... continuationFlags){
		if(this.continuationFlags != null && continuationFlags != null)
			for(String flag : this.continuationFlags)
				if(ArrayUtils.contains(continuationFlags, flag))
					return true;
		return false;
	}

	public List<AffixEntry> getAppliedRules(){
		return Collections.<AffixEntry>emptyList();
	}

	/**
	 * Get last applied rule of type {@code type}
	 * 
	 * @param type	The type used to filter the last applied rule
	 * @return	The last applied rule of the specified type
	 */
	public AffixEntry getLastAppliedRule(AffixEntry.Type type){
		return null;
	}

	public Map<String, Set<DictionaryEntry>> distributeByCompoundRule(AffixData affixData){
		return Arrays.stream(continuationFlags != null? continuationFlags: new String[0])
			.filter(affixData::isManagedByCompoundRule)
			.collect(Collectors.groupingBy(flag -> flag, Collectors.mapping(x -> this, Collectors.toSet())));
	}

	public Map<String, Set<DictionaryEntry>> distributeByCompoundBeginMiddleEnd(String compoundBeginFlag, String compoundMiddleFlag,
			String compoundEndFlag){
		Map<String, Set<DictionaryEntry>> distribution = new HashMap<>(3);
		distribution.put(compoundBeginFlag, new HashSet<>());
		distribution.put(compoundMiddleFlag, new HashSet<>());
		distribution.put(compoundEndFlag, new HashSet<>());
		if(continuationFlags != null)
			for(String flag : continuationFlags){
				Set<DictionaryEntry> value = distribution.get(flag);
				if(value != null)
					value.add(this);
			}
		return distribution;
	}

	public boolean hasPartOfSpeech(String partOfSpeech){
		return hasMorphologicalField(MorphologicalTag.TAG_PART_OF_SPEECH + partOfSpeech);
	}

	private boolean hasMorphologicalField(String morphologicalField){
		return (morphologicalFields != null && ArrayUtils.contains(morphologicalFields, morphologicalField));
	}

	public void forEachMorphologicalField(Consumer<String> fun){
		if(morphologicalFields != null)
			for(String morphologicalField : morphologicalFields)
				fun.accept(morphologicalField);
	}

	public void removeAffixes(AffixData affixData){
		Affixes affixes = separateAffixes(affixData);
		continuationFlags = affixes.getTerminalAffixes();
	}

	public List<String[]> extractAllAffixes(AffixData affixData, boolean reverse){
		Affixes affixes = separateAffixes(affixData);
		return affixes.extractAllAffixes(reverse);
	}

	/**
	 * Separate the prefixes from the suffixes and from the terminals
	 * 
	 * @param affixData	The {@link AffixData}
	 * @return	An object with separated flags, one for each group (prefixes, suffixes, terminals)
	 */
	private Affixes separateAffixes(AffixData affixData) throws IllegalArgumentException{
		List<String> terminalAffixes = new ArrayList<>();
		List<String> prefixes = new ArrayList<>();
		List<String> suffixes = new ArrayList<>();
		if(continuationFlags != null)
			for(String affix : continuationFlags){
				if(affixData.isTerminalAffix(affix)){
					terminalAffixes.add(affix);
					continue;
				}

				Object rule = affixData.getData(affix);
				if(rule == null){
					if(affixData.isManagedByCompoundRule(affix))
						continue;

					List<AffixEntry> appliedRules = getAppliedRules();
					String parentFlag = (appliedRules != null && !appliedRules.isEmpty()? appliedRules.get(0).getFlag(): null);
					throw new IllegalArgumentException("Non–existent rule " + affix + " found" + (parentFlag != null? " via " + parentFlag:
						StringUtils.EMPTY));
				}

				if(rule instanceof RuleEntry){
					if(((RuleEntry)rule).isSuffix())
						suffixes.add(affix);
					else
						prefixes.add(affix);
				}
				else
					terminalAffixes.add(affix);
			}

		return new Affixes(prefixes, suffixes, terminalAffixes);
	}

	public boolean isCompound(){
		return false;
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer(word);
		if(continuationFlags != null && continuationFlags.length > 0){
			sb.append(SLASH);
			sb.append(StringUtils.join(continuationFlags, COMMA));
		}
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sb.append(TAB).append(StringUtils.join(morphologicalFields, StringUtils.SPACE));
		return sb.toString();
	}

	public String toString(FlagParsingStrategy strategy){
		Objects.requireNonNull(strategy);

		StringBuffer sb = new StringBuffer(word);
		if(continuationFlags != null && continuationFlags.length > 0){
			sb.append(SLASH);
			sb.append(strategy.joinFlags(continuationFlags));
		}
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sb.append(TAB).append(StringUtils.join(morphologicalFields, StringUtils.SPACE));
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		DictionaryEntry rhs = (DictionaryEntry)obj;
		return new EqualsBuilder()
			.append(word, rhs.word)
			.append(continuationFlags, rhs.continuationFlags)
			.append(morphologicalFields, rhs.morphologicalFields)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(word)
			.append(continuationFlags)
			.append(morphologicalFields)
			.toHashCode();
	}

}
