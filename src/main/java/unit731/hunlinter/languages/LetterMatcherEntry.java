package unit731.hunlinter.languages;

import java.text.MessageFormat;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.exceptions.HunLintException;


public class LetterMatcherEntry{

	private final MessageFormat messagePattern;
	private final String masterLetter;
	private final String[] wrongFlags;
	private final String correctRule;


	public LetterMatcherEntry(final MessageFormat messagePattern, final String masterLetter, final String[] wrongFlags, final String correctRule){
		this.messagePattern = messagePattern;
		this.masterLetter = masterLetter;
		this.wrongFlags = wrongFlags;
		this.correctRule = correctRule;
	}

	public void match(final Production production){
		for(final String flag : wrongFlags)
			if(production.hasContinuationFlag(flag))
				throw new HunLintException(messagePattern.format(new Object[]{masterLetter, flag, correctRule}));
	}

}