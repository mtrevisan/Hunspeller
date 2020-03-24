package unit731.hunlinter.parsers.vos;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.beust.jcommander.Strings;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.services.ArraySet;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.RegexHelper;

import static unit731.hunlinter.services.system.LoopHelper.applyIf;
import static unit731.hunlinter.services.system.LoopHelper.forEach;
import static unit731.hunlinter.services.system.LoopHelper.match;
import static unit731.hunlinter.services.system.LoopHelper.removeIf;


public class AffixEntry{

	private static final MessageFormat AFFIX_EXPECTED = new MessageFormat("Expected an affix entry, found something else{0}");
	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Cannot parse affix line ''{0}''");
	private static final MessageFormat WRONG_TYPE = new MessageFormat("Wrong rule type, expected ''{0}'', got ''{1}''");
	private static final MessageFormat WRONG_FLAG = new MessageFormat("Wrong rule flag, expected ''{0}'', got ''{1}''");
	private static final MessageFormat WRONG_CONDITION_END = new MessageFormat("Condition part doesn''t ends with removal part: ''{0}''");
	private static final MessageFormat WRONG_CONDITION_START = new MessageFormat("Condition part doesn''t starts with removal part: ''{0}''");
	private static final MessageFormat POS_PRESENT = new MessageFormat("Part-of-Speech detected: ''{0}''");
	//warning
	private static final MessageFormat CHARACTERS_IN_COMMON = new MessageFormat("Characters in common between removed and added part: ''{0}''");
	private static final MessageFormat CANNOT_FULL_STRIP = new MessageFormat("Cannot strip full word ''{0}'' without the FULLSTRIP option");

	private static final int PARAM_CONDITION = 1;
	private static final int PARAM_CONTINUATION_CLASSES = 2;
	private static final Pattern PATTERN_LINE = RegexHelper.pattern("^(?<condition>[^\\s]+?)(?:(?<!\\\\)\\/(?<continuationClasses>[^\\s]+))?$");

	private static final String TAB = "\t";
	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";

	private static final String DOT = ".";
	private static final String ZERO = "0";


	private RuleEntry parent;

	/** string to strip */
	private final String removing;
	private final int removingLength;
	/** string to append */
	private final String appending;
	private final int appendingLength;
	final String[] continuationFlags;
	/** condition that must be met before the affix can be applied */
	private final String condition;
	final String[] morphologicalFields;


	public AffixEntry(final String line, final AffixType parentType, final String parentFlag, final FlagParsingStrategy strategy,
			final List<String> aliasesFlag, final List<String> aliasesMorphologicalField){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		final String[] lineParts = StringUtils.split(line, null, 6);
		if(lineParts.length < 4 || lineParts.length > 6)
			throw new LinterException(AFFIX_EXPECTED.format(new Object[]{(lineParts.length > 0? ": '" + line + "'": StringUtils.EMPTY)}));

		final AffixType type = AffixType.createFromCode(lineParts[0]);
		final String flag = lineParts[1];
		final String removal = StringUtils.replace(lineParts[2], SLASH_ESCAPED, SLASH);
		final Matcher m = RegexHelper.matcher(lineParts[3], PATTERN_LINE);
		if(!m.find())
			throw new LinterException(WRONG_FORMAT.format(new Object[]{line}));
		final String addition = StringUtils.replace(m.group(PARAM_CONDITION), SLASH_ESCAPED, SLASH);
		final String continuationClasses = m.group(PARAM_CONTINUATION_CLASSES);
		condition = (lineParts.length > 4? StringUtils.replace(lineParts[4], SLASH_ESCAPED, SLASH): DOT);
		morphologicalFields = (lineParts.length > 5? StringUtils.split(expandAliases(lineParts[5], aliasesMorphologicalField)): null);

		final String[] classes = strategy.parseFlags((continuationClasses != null? expandAliases(continuationClasses, aliasesFlag): null));
		continuationFlags = (classes != null && classes.length > 0? classes: null);
		removing = (!ZERO.equals(removal)? removal: StringUtils.EMPTY);
		removingLength = removing.length();
		appending = (!ZERO.equals(addition)? addition: StringUtils.EMPTY);
		appendingLength = appending.length();

		checkValidity(parentType, type, parentFlag, flag, removal, line);
	}

	public void setParent(final RuleEntry parent){
		Objects.requireNonNull(parent);

		this.parent = parent;
	}

	private void checkValidity(final AffixType parentType, final AffixType type, final String parentFlag, final String flag,
			final String removal, final String line){
		if(parentType != type)
			throw new LinterException(WRONG_TYPE.format(new Object[]{parentType, type}));
		if(!parentFlag.equals(flag))
			throw new LinterException(WRONG_FLAG.format(new Object[]{parentFlag, flag}));
		if(removingLength > 0){
			if(parentType == AffixType.SUFFIX){
				if(!condition.endsWith(removal))
					throw new LinterException(WRONG_CONDITION_END.format(new Object[]{line}));
				if(appending.length() > 1 && removal.charAt(0) == appending.charAt(0))
					throw new LinterException(CHARACTERS_IN_COMMON.format(new Object[]{line}));
			}
			else{
				if(!condition.startsWith(removal))
					throw new LinterException(WRONG_CONDITION_START.format(new Object[]{line}));
				if(appending.length() > 1 && removal.charAt(removal.length() - 1) == appending.charAt(appending.length() - 1))
					throw new LinterException(CHARACTERS_IN_COMMON.format(new Object[]{line}));
			}
		}
	}

	public String getAppending(){
		return appending;
	}

	private String expandAliases(final String part, final List<String> aliases){
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)? aliases.get(Integer.parseInt(part) - 1): part);
	}

	public boolean hasContinuationFlags(){
		return (continuationFlags != null && continuationFlags.length > 0);
	}

	public boolean hasContinuationFlag(final String flag){
		return (hasContinuationFlags() && flag != null && Arrays.binarySearch(continuationFlags, flag) >= 0);
	}

	public String[] combineContinuationFlags(final String[] otherContinuationFlags){
		final ArraySet<String> flags = new ArraySet<>();
		if(otherContinuationFlags != null && otherContinuationFlags.length > 0)
			flags.addAll(otherContinuationFlags);
		if(continuationFlags != null)
			flags.addAll(Arrays.asList(continuationFlags));
		final int size = flags.size();
		return (size > 0? flags.toArray(String[]::new): null);
	}

	//FIXME is this documentation updated/true?
	/**
	 *
	 * Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the
	 * 	suffix fields)
	 * Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the
	 * 	suffix fields)
	 * Terminal Suffix: inflectional suffix fields removed by additional (not terminal) suffixes, useful for zero morphemes and
	 * 	affixes removed by splitting rules
	 *
	 * @param dicEntry	The dictionary entry to combine from
	 * @return	The list of new morphological fields
	 */
	public String[] combineMorphologicalFields(final DictionaryEntry dicEntry){
		final String[] mf = (dicEntry.morphologicalFields != null? dicEntry.morphologicalFields: new String[0]);
		final String[] amf = (morphologicalFields != null? morphologicalFields: new String[0]);

		//NOTE: part–of–speech is NOT overwritten, both in simple application of an affix rule and of a compound rule
		final boolean containsInflectionalAffix = containsAffixes(amf, MorphologicalTag.INFLECTIONAL_SUFFIX,
			MorphologicalTag.INFLECTIONAL_PREFIX);
		final boolean containsTerminalAffixes = containsAffixes(amf, MorphologicalTag.TERMINAL_SUFFIX,
			MorphologicalTag.TERMINAL_PREFIX);
		//remove inflectional and terminal suffixes
		removeIf(mf, field ->
			containsInflectionalAffix && (MorphologicalTag.INFLECTIONAL_SUFFIX.isSupertypeOf(field) || MorphologicalTag.INFLECTIONAL_PREFIX.isSupertypeOf(field))
			|| !containsTerminalAffixes && MorphologicalTag.TERMINAL_SUFFIX.isSupertypeOf(field));

		//add morphological fields from the applied affix
		return (parent.getType() == AffixType.SUFFIX? ArrayUtils.addAll(mf, amf): ArrayUtils.addAll(amf, mf));
	}

	private boolean containsAffixes(final String[] amf, final MorphologicalTag... tags){
		return (match(tags, tag -> match(amf, tag::isSupertypeOf) != null) != null);
	}

	public static String[] extractMorphologicalFields(final DictionaryEntry[] compoundEntries){
		final List<String[]> mf = new ArrayList<>(compoundEntries != null? compoundEntries.length: 0);
		forEach(compoundEntries, compoundEntry -> {
			final String compound = compoundEntry.getWord();
			mf.add(ArrayUtils.addAll(new String[]{MorphologicalTag.PART.attachValue(compound)}, compoundEntry.morphologicalFields));
		});

		final List<String> list = new ArrayList<>();
		for(final String[] strings : mf)
			forEach(strings, list::add);
		return list.toArray(new String[0]);
	}

	public void validate(){
		final List<String> filteredFields = getMorphologicalFields(MorphologicalTag.PART_OF_SPEECH);
		if(!filteredFields.isEmpty())
			throw new LinterException(POS_PRESENT.format(new Object[]{String.join(", ", filteredFields)}));
	}

	public AffixType getType(){
		return parent.getType();
	}

	public String getFlag(){
		return parent.getFlag();
	}

	private List<String> getMorphologicalFields(final MorphologicalTag morphologicalTag){
		final String tag = morphologicalTag.getCode();
		final int purgeTag = tag.length();
		final List<String> collector = new ArrayList<>(morphologicalFields != null? morphologicalFields.length: 0);
		applyIf(morphologicalFields,
			df -> df.startsWith(tag),
			df -> collector.add(df.substring(purgeTag)));
		return collector;
	}

	public boolean canApplyTo(final String word){
		final int conditionLength = condition.length();
		if(conditionLength == 1 && condition.charAt(0) == '.')
			return true;

		final int wordLength = word.length();
		if(parent.getType() == AffixType.PREFIX){
			if(wordLength >= conditionLength && word.startsWith(condition))
				return true;

			int i, j;
			for(i = 0, j = 0; i < wordLength && j < conditionLength; i ++, j ++){
				if(condition.charAt(j) == '['){
					final boolean neg = (condition.charAt(j + 1) == '^');
					boolean in = false;
					do{
						j ++;
						//noinspection IfStatementMissingBreakInLoop
						if(word.charAt(i) == condition.charAt(j))
							in = true;
					}while(j < conditionLength - 1 && condition.charAt(j) != ']');
					if(neg == in || j == conditionLength - 1 && condition.charAt(j) != ']')
						return false;
				}
				else if(condition.charAt(j) != word.charAt(i))
					return false;
			}
			return (j >= conditionLength);
		}
		else{
			if(wordLength >= conditionLength && word.endsWith(condition))
				return true;

			int i, j;
			for(i = wordLength - 1, j = conditionLength - 1; i >= 0 && j >= 0; i --, j --){
				if(condition.charAt(j) == ']'){
					boolean in = false;
					do{
						j --;
						//noinspection IfStatementMissingBreakInLoop
						if(word.charAt(i) == condition.charAt(j))
							in = true;
					}while(j > 0 && condition.charAt(j) != '[');
					if(j == 0 && condition.charAt(j) != '[')
						return false;
					final boolean neg = (condition.charAt(j + 1) == '^');
					if(neg == in)
						return false;
				}
				else if(condition.charAt(j) != word.charAt(i))
					return false;
			}
			return (j < 0);
		}
	}

	public boolean canInverseApplyTo(final String word){
		return (parent.getType() == AffixType.SUFFIX? word.endsWith(appending): word.startsWith(appending));
	}

	public String applyRule(final String word, final boolean isFullstrip){
		if(!isFullstrip && word.length() == removingLength)
			throw new LinterException(CANNOT_FULL_STRIP.format(new Object[]{word}));

		return (parent.getType() == AffixType.SUFFIX?
			word.substring(0, word.length() - removingLength) + appending:
			appending + word.substring(removingLength));
	}

	//NOTE: {#canInverseApplyTo} should be called to verify applicability
	public String undoRule(final String word){
		return (parent.getType() == AffixType.SUFFIX?
			word.substring(0, word.length() - appendingLength) + removing:
			removing + word.substring(appendingLength));
	}

	public String toStringWithMorphologicalFields(final FlagParsingStrategy strategy){
		Objects.requireNonNull(strategy);

		final StringBuffer sb = new StringBuffer();
		if(continuationFlags != null && continuationFlags.length > 0){
			sb.append(SLASH);
			sb.append(strategy.joinFlags(continuationFlags));
		}
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sb.append(TAB).append(StringUtils.join(morphologicalFields, StringUtils.SPACE));
		return sb.toString();
	}

	@Override
	public String toString(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(parent.getType().getOption().getCode())
			.add(parent.getFlag())
			.add(removing)
			.add(appending);
		if(continuationFlags != null && continuationFlags.length > 0)
			sj.add(SLASH + Strings.join(StringUtils.EMPTY, continuationFlags));
		sj.add(condition);
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sj.add(Strings.join(StringUtils.EMPTY, morphologicalFields));
		return sj.toString();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final AffixEntry rhs = (AffixEntry)obj;
		final EqualsBuilder builder = new EqualsBuilder()
			.append(parent != null, rhs.parent != null)
			.append(continuationFlags, rhs.continuationFlags)
			.append(condition, rhs.condition)
			.append(removing, rhs.removing)
			.append(appending, rhs.appending)
			.append(morphologicalFields, rhs.morphologicalFields);
		if(parent != null)
			builder.append(parent.getType(), rhs.parent.getType())
				.append(parent.getFlag(), rhs.parent.getFlag());
		return builder.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(parent.getType())
			.append(parent.getFlag())
			.append(continuationFlags)
			.append(condition)
			.append(removing)
			.append(appending)
			.append(morphologicalFields)
			.toHashCode();
	}

}
