package unit731.hunspeller.parsers.strategies;

import org.apache.commons.lang3.StringUtils;


/**
 * Implementation of {@link FlagParsingStrategy} that assumes each flag is encoded as two ASCII characters whose codes
 * must be combined into a single character.
 */
public class DoubleCharParsingStrategy implements FlagParsingStrategy{

	private static final String SPLITTER = "(?<=\\G.{2})";
	private static final String SLASH = "/";


	@Override
	public String[] parseRuleFlags(String textFlags){
		if(textFlags != null && textFlags.length() % 2 != 0)
			throw new IllegalArgumentException("Flag must be of length two or a multiple");

		return (textFlags != null && !textFlags.isEmpty()? removeDuplicates(textFlags.split(SPLITTER)): new String[0]);
	}

	@Override
	public String joinRuleFlags(String[] textFlags){
		if(textFlags == null || textFlags.length == 0)
			return StringUtils.EMPTY;

		return SLASH + String.join(StringUtils.EMPTY, textFlags);
	}

}
