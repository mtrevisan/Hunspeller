package unit731.hunspeller.parsers.strategies;

import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;


/**
 * Simple implementation of {@link FlagParsingStrategy} that treats the chars in each String as a individual flags.
 */
public class ASCIIParsingStrategy implements FlagParsingStrategy{

	@Override
	public String[] parseRuleFlags(String textFlags){
		if(textFlags == null)
			return new String[0];

		if(!StandardCharsets.US_ASCII.newEncoder().canEncode(textFlags))
			throw new IllegalArgumentException("Each flag must be in ASCII encoding");

		String[] flags = (!textFlags.isEmpty()? removeDuplicates(textFlags.split(StringUtils.EMPTY)): new String[0]);
		for(String flag : flags)
			if(StringUtils.isBlank(flag))
				throw new IllegalArgumentException("Flag must be a valid ASCII character");
		return flags;
	}

	@Override
	public String joinRuleFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;
		for(String flag : textFlags){
			if(flag == null || flag.length() != 1)
				throw new IllegalArgumentException("Each flag must be of length one");
			if(!StandardCharsets.US_ASCII.newEncoder().canEncode(flag))
				throw new IllegalArgumentException("Each flag must be in ASCII encoding");
		}

		return SLASH + String.join(StringUtils.EMPTY, textFlags);
	}

}
