/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.parsers.affix.handlers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.ParsingContext;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.services.eventbus.EventBusService;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.workers.exceptions.LinterWarning;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;


public class CompoundRuleHandler implements Handler{

	private static final MessageFormat MISMATCHED_COMPOUND_RULE_TYPE = new MessageFormat("Error reading line `{0}`: mismatched compound rule type (expected {1})");
	private static final MessageFormat DUPLICATED_LINE = new MessageFormat("Error reading line `{0}`: duplicated line");
	private static final MessageFormat BAD_FIRST_PARAMETER = new MessageFormat("Error reading line `{0}`: the first parameter is not a number");
	private static final MessageFormat BAD_NUMBER_OF_ENTRIES = new MessageFormat("Error reading line `{0}`: bad number of entries, `{1}` must be a positive integer");
	private static final MessageFormat EMPTY_COMPOUND_RULE_TYPE = new MessageFormat("Error reading line `{0}`: compound rule type cannot be empty");
	private static final MessageFormat BAD_FORMAT = new MessageFormat("Error reading line `{0}`: compound rule is bad formatted");


	@Override
	public int parse(final ParsingContext context, final AffixData affixData){
		try{
			final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();

			final int numEntries = checkValidity(context);

			final Scanner scanner = context.getScanner();

			final Set<String> compoundRules = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				ParserHelper.assertNotEOF(scanner);

				final String line = scanner.nextLine();
				final String[] lineParts = StringUtils.split(line);

				final AffixOption option = AffixOption.createFromCode(lineParts[0]);
				if(option != AffixOption.COMPOUND_RULE)
					throw new LinterException(MISMATCHED_COMPOUND_RULE_TYPE.format(new Object[]{line, AffixOption.COMPOUND_RULE}));

				final String rule = lineParts[1];

				checkRuleValidity(rule, line, strategy);

				final boolean inserted = compoundRules.add(rule);
				if(!inserted)
					EventBusService.publish(new LinterWarning(DUPLICATED_LINE.format(new Object[]{line}), IndexDataPair.of(context.getIndex() + i, null)));
			}

			affixData.addData(AffixOption.COMPOUND_RULE.getCode(), compoundRules);

			return numEntries;
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private int checkValidity(final ParsingContext context){
		if(!NumberUtils.isCreatable(context.getFirstParameter()))
			throw new LinterException(BAD_FIRST_PARAMETER.format(new Object[]{context}));
		final int numEntries = Integer.parseInt(context.getFirstParameter());
		if(numEntries <= 0)
			throw new LinterException(BAD_NUMBER_OF_ENTRIES.format(new Object[]{context, context.getFirstParameter()}));

		return numEntries;
	}

	private void checkRuleValidity(final String rule, final String line, final FlagParsingStrategy strategy){
		if(StringUtils.isBlank(rule))
			throw new LinterException(EMPTY_COMPOUND_RULE_TYPE.format(new Object[]{line}));
		final String[] compounds = strategy.extractCompoundRule(rule);
		if(compounds.length == 0)
			throw new LinterException(BAD_FORMAT.format(new Object[]{line}));
	}

}
