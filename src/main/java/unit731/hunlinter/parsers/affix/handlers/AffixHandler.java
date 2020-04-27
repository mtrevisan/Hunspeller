package unit731.hunlinter.parsers.affix.handlers;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.affix.ParsingContext;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.workers.exceptions.LinterException;


public class AffixHandler implements Handler{

	private static final MessageFormat BAD_THIRD_PARAMETER = new MessageFormat("Error reading line ''{0}'': the third parameter is not a number");
	private static final MessageFormat BAD_NUMBER_OF_ENTRIES = new MessageFormat("Error reading line ''{0}'': bad number of entries, ''{1}'' must be a positive integer");
	private static final MessageFormat DUPLICATED_LINE = new MessageFormat("Duplicated line: {0}");
	private static final MessageFormat MISMATCHED_RULE_TYPE = new MessageFormat("Mismatched rule type (expected ''{0}'')");
	private static final MessageFormat MISMATCHED_RULE_FLAG = new MessageFormat("Mismatched rule flag (expected ''{0}'')");


	@Override
	public int parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixOption, List<String>> getData){
		try{
			final AffixType parentType = AffixType.createFromCode(context.getRuleType());
			final String ruleFlag = context.getFirstParameter();
			final char combinable = context.getSecondParameter().charAt(0);
			if(!NumberUtils.isCreatable(context.getThirdParameter()))
				throw new LinterException(BAD_THIRD_PARAMETER.format(new Object[]{context}));

			final RuleEntry parent = new RuleEntry(parentType, ruleFlag, combinable);
			final AffixEntry[] entries = readEntries(context, strategy, parent, getData);
			parent.setEntries(entries);

			addData.accept(ruleFlag, parent);

			return Integer.parseInt(context.getThirdParameter());
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private AffixEntry[] readEntries(final ParsingContext context, final FlagParsingStrategy strategy, final RuleEntry parent,
			final Function<AffixOption, List<String>> getData) throws IOException{
		final int numEntries = Integer.parseInt(context.getThirdParameter());
		if(numEntries <= 0)
			throw new LinterException(BAD_NUMBER_OF_ENTRIES.format(new Object[]{context, context.getThirdParameter()}));

		final Scanner scanner = context.getScanner();
		final AffixType parentType = AffixType.createFromCode(context.getRuleType());
		final String parentFlag = context.getFirstParameter();

		//List<AffixEntry> prefixEntries = new ArrayList<>();
		//List<AffixEntry> suffixEntries = new ArrayList<>();
		final List<String> aliasesFlag = getData.apply(AffixOption.ALIASES_FLAG);
		final List<String> aliasesMorphologicalField = getData.apply(AffixOption.ALIASES_MORPHOLOGICAL_FIELD);
		String line;
		int offset = 0;
		final AffixEntry[] entries = new AffixEntry[numEntries];
		for(int i = 0; i < numEntries; i ++){
			ParserHelper.assertNotEOF(scanner);

			line = scanner.nextLine();
			final AffixEntry entry = new AffixEntry(line, parentType, parentFlag, strategy, aliasesFlag, aliasesMorphologicalField);
			entry.setParent(parent);
//com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(entry)


			checkValidity(parentType, parentFlag, line, entry);


			if(ArrayUtils.contains(entries, entry))
				throw new LinterException(DUPLICATED_LINE.format(new Object[]{entry.toString()}));

			entries[offset ++] = entry;

//String regexToMatch = (entry.getMatch() != null? entry.getMatch().pattern().pattern().replaceFirst("^\\^", StringUtils.EMPTY).replaceFirst("\\$$", StringUtils.EMPTY): ".");
//String[] arr = RegExpTrieSequencer.extractCharacters(regexToMatch);
//List<AffixEntry> lst = new ArrayList<>();
//lst.add(entry);
//if(entry.isSuffix()){
//	ArrayUtils.reverse(arr);
//	suffixEntries.add(arr, lst);
//}
//else
//	prefixEntries.put(arr, lst);
		}
		return entries;
	}

	private void checkValidity(final AffixType ruleType, final String ruleFlag, final String line, final AffixEntry entry){
		final String ruleTypeCode = ruleType.getOption().getCode();
		if(!line.startsWith(ruleTypeCode))
			throw new LinterException(MISMATCHED_RULE_TYPE.format(new Object[]{ruleType}));
		if(!line.startsWith(ruleTypeCode + StringUtils.SPACE + ruleFlag))
			throw new LinterException(MISMATCHED_RULE_FLAG.format(new Object[]{ruleFlag}));

		entry.validate();
	}

}
