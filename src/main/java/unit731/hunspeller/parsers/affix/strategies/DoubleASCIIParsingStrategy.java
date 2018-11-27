package unit731.hunspeller.parsers.affix.strategies;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternHelper;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded as two ASCII characters whose codes
 * must be combined into a single character.
 */
public class DoubleASCIIParsingStrategy implements FlagParsingStrategy{

	private static final Pattern PATTERN = PatternHelper.pattern("(?<=\\G.{2})");

	private static final Pattern COMPOUND_RULE_SPLITTER = PatternHelper.pattern("\\((..)\\)|([?*])");


	@Override
	@SuppressFBWarnings(value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS", justification = "Deliberate")
	public String[] parseFlags(String textFlags){
		if(StringUtils.isBlank(textFlags))
			return null;

		if(textFlags.length() % 2 != 0)
			throw new IllegalArgumentException("Flag must be of length multiple of two: " + textFlags);

		int size = (textFlags.length() >>> 1);
		String[] flags = PatternHelper.split(textFlags, PATTERN);
		Set<String> unduplicatedFlags = new HashSet<>(Arrays.asList(flags));
		if(unduplicatedFlags.size() < size)
			throw new IllegalArgumentException("Flags must not be duplicated: " + textFlags);

		return flags;
	}

	@Override
	public String joinFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;
		for(String flag : textFlags)
			if(flag == null || flag.length() != 2)
				throw new IllegalArgumentException("Each flag must be of length two: " + flag + " from " + Arrays.toString(textFlags));

		return String.join(StringUtils.EMPTY, textFlags);
	}

	@Override
	public String[] extractCompoundRule(String compoundRule){
		String[] parts = PatternHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		for(String part : parts)
			if(part.length() != 2 && (part.length() != 1 || part.charAt(0) != '*' && part.charAt(0) != '?'))
				throw new IllegalArgumentException("Compound rule must be composed by double-characters flags, or the optional operators '*' or '? : " + compoundRule);

		return parts;
	}

}
