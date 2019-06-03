package unit731.hunspeller.parsers.affix.strategies;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.SetHelper;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded in its numerical form. In the case
 * of multiple flags, each number is separated by a comma.
 */
class NumericalParsingStrategy implements FlagParsingStrategy{

	private static final int MAX_NUMERICAL_FLAG = 65_000;

	private static final String COMMA = ",";

	private static final Pattern COMPOUND_RULE_SPLITTER = PatternHelper.pattern("\\((\\d+)\\)|([?*])");

	private static class SingletonHelper{
		private static final NumericalParsingStrategy INSTANCE = new NumericalParsingStrategy();
	}


	public static synchronized NumericalParsingStrategy getInstance(){
		return SingletonHelper.INSTANCE;
	}

	private NumericalParsingStrategy(){}

	@Override
	public String[] parseFlags(final String flags){
		if(StringUtils.isBlank(flags))
			return null;

		final String[] singleFlags = extractFlags(flags);

		checkForDuplication(singleFlags, flags);

		checkValidity(singleFlags, flags);

		return singleFlags;
	}

	private String[] extractFlags(final String flags){
		return StringUtils.split(flags, COMMA);
	}

	private void checkForDuplication(final String[] flags, final String originalFlags) throws IllegalArgumentException{
		final Set<String> unduplicatedFlags = SetHelper.setOf(flags);
		if(unduplicatedFlags.size() < flags.length)
			throw new IllegalArgumentException("Flags must not be duplicated: " + originalFlags);
	}

	@Override
	public String joinFlags(final String[] flags){
		if(flags == null || flags.length == 0)
			return StringUtils.EMPTY;

		final String originalFlags = Arrays.toString(flags);
		checkValidity(flags, originalFlags);

		return String.join(COMMA, flags);
	}

	private void checkValidity(final String[] flags, final String originalFlags) throws IllegalArgumentException{
		for(final String flag : flags)
			checkValidity(flag, originalFlags);
	}

	private void checkValidity(final String flag, final String originalFlags) throws IllegalArgumentException{
		try{
			final int numericalFlag = Integer.parseInt(flag);
			if(numericalFlag <= 0 || numericalFlag > MAX_NUMERICAL_FLAG)
				throw new IllegalArgumentException("Flag must be in the range [1, " + MAX_NUMERICAL_FLAG + "]: " + flag + " from " + originalFlags);
		}
		catch(final NumberFormatException e){
			throw new IllegalArgumentException("Flag must be an integer number: " + flag + " from " + originalFlags);
		}
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
		final boolean isNumber = (part.length() != 1 || part.charAt(0) != '*' && part.charAt(0) != '?');
		if(isNumber && !NumberUtils.isCreatable(part))
			throw new IllegalArgumentException("Compound rule must be composed by numbers and the optional operators '*' and '?': "
				+ compoundRule);
	}

}
