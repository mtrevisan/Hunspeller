package unit731.hunspeller.parsers.affix.strategies;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.SetHelper;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded as two ASCII characters whose codes
 * must be combined into a single character.
 */
class DoubleASCIIParsingStrategy implements FlagParsingStrategy{

	private static final Pattern PATTERN = PatternHelper.pattern("(?<=\\G.{2})");

	private static final Pattern COMPOUND_RULE_SPLITTER = PatternHelper.pattern("\\((..)\\)|([?*])");

	private static class SingletonHelper{
		private static final DoubleASCIIParsingStrategy INSTANCE = new DoubleASCIIParsingStrategy();
	}


	public static synchronized DoubleASCIIParsingStrategy getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private DoubleASCIIParsingStrategy(){}

	@Override
	public String[] parseFlags(final String flags){
		if(StringUtils.isBlank(flags))
			return null;

		checkValidity(flags);

		final String[] singleFlags = extractFlags(flags);

		checkForDuplication(singleFlags, flags);

		return singleFlags;
	}

	private void checkValidity(final String flags) throws IllegalArgumentException{
		if(flags.length() % 2 != 0)
			throw new IllegalArgumentException("Flag must be of length multiple of two: " + flags);
	}

	private String[] extractFlags(final String flags){
		return PatternHelper.split(flags, PATTERN);
	}

	private void checkForDuplication(final String[] flags, final String originalFlags) throws IllegalArgumentException{
		final Set<String> notDuplicatedFlags = SetHelper.setOf(flags);
		if((notDuplicatedFlags.size() << 1) < originalFlags.length())
			throw new IllegalArgumentException("Flags must not be duplicated: " + originalFlags);
	}

	@Override
	public String joinFlags(final String[] flags){
		if(flags == null || flags.length == 0)
			return StringUtils.EMPTY;

		final String originalFlags = Arrays.toString(flags);
		checkValidity(flags, originalFlags);

		return String.join(StringUtils.EMPTY, flags);
	}

	private void checkValidity(final String[] flags, final String originalFlags) throws IllegalArgumentException{
		for(final String flag : flags)
			if(flag == null || flag.length() != 2)
				throw new IllegalArgumentException("Flag must be of length two: " + flag + " from " + originalFlags);
	}

	@Override
	public String[] extractCompoundRule(final String compoundRule){
		final String[] parts = PatternHelper.extract(compoundRule, COMPOUND_RULE_SPLITTER);

		checkCompoundValidity(parts, compoundRule);

		return parts;
	}

	private void checkCompoundValidity(final String[] parts, final String compoundRule) throws IllegalArgumentException{
		for(final String part : parts)
			checkCompoundValidity(part, compoundRule);
	}

	private void checkCompoundValidity(final String part, final String compoundRule) throws IllegalArgumentException{
		final int size = part.length();
		final boolean isFlag = (size != 1 || part.charAt(0) != '*' && part.charAt(0) != '?');
		if(size != 2 && isFlag)
			throw new IllegalArgumentException("Compound rule must be composed by double-characters flags, or the optional operators '*' or '? : "
				+ compoundRule);
	}

}
