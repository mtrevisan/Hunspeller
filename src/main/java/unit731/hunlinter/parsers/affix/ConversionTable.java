package unit731.hunlinter.parsers.affix;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.workers.exceptions.HunLintException;
import unit731.hunlinter.services.ParserHelper;


public class ConversionTable{

	private static final MessageFormat BAD_FIRST_PARAMETER = new MessageFormat("Error reading line ''{0}'': the first parameter is not a number");
	private static final MessageFormat BAD_NUMBER_OF_ENTRIES = new MessageFormat("Error reading line ''{0}'': bad number of entries, ''{1}'' must be a positive integer");
	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Error reading line ''{0}'': bad number of entries, it must be '<option> <pattern-from> <pattern-to>'");
	private static final MessageFormat BAD_OPTION = new MessageFormat("Error reading line ''{0}'': bad option, it must be {1}");
	private static final MessageFormat TOO_MANY_APPLICABLE_RULES = new MessageFormat("Cannot convert word ''{0}'', too many applicable rules");


	@FunctionalInterface
	public interface ConversionFunction{
		void convert(String word, Pair<String, String> entry, List<String> conversions);
	}

	private static final String KEY_INSIDE = reduceKey(" ");
	private static final String KEY_STARTS_WITH = reduceKey("^");
	private static final String KEY_ENDS_WITH = reduceKey("$");
	private static final String KEY_WHOLE = reduceKey("^$");
	private static final String ZERO = "0";

	private static final Map<String, ConversionFunction> CONVERSION_TABLE_ADD_METHODS = new HashMap<>();
	static{
		CONVERSION_TABLE_ADD_METHODS.put(KEY_INSIDE, ConversionTable::convertInside);
		CONVERSION_TABLE_ADD_METHODS.put(KEY_STARTS_WITH, ConversionTable::convertStartsWith);
		CONVERSION_TABLE_ADD_METHODS.put(KEY_ENDS_WITH, ConversionTable::convertEndsWith);
		CONVERSION_TABLE_ADD_METHODS.put(KEY_WHOLE, ConversionTable::convertWhole);
	}


	private final AffixOption affixOption;
	private Map<String, List<Pair<String, String>>> table;


	public ConversionTable(final AffixOption affixOption){
		this.affixOption = affixOption;
	}

	public void parseConversionTable(final ParsingContext context){
		try{
			final BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new HunLintException(BAD_FIRST_PARAMETER.format(new Object[]{context}));
			final int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new HunLintException(BAD_NUMBER_OF_ENTRIES.format(new Object[]{context, context.getFirstParameter()}));

			table = new HashMap<>(4);
			for(int i = 0; i < numEntries; i ++){
				final String line = ParserHelper.extractLine(br);

				final String[] parts = StringUtils.split(line);

				checkValidity(parts, context);

				final String key = reduceKey(parts[1]);
				table.computeIfAbsent(key, k -> new ArrayList<>())
					.add(Pair.of(parts[1], StringUtils.replaceChars(parts[2], '_', ' ')));
			}
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private void checkValidity(final String[] parts, final ParsingContext context){
		if(parts.length != 3)
			throw new HunLintException(WRONG_FORMAT.format(new Object[]{context}));
		if(!affixOption.getCode().equals(parts[0]))
			throw new HunLintException(BAD_OPTION.format(new Object[]{context, affixOption.getCode()}));
	}

	/**
	 * NOTE: returns the original word if no conversion has been applied!
	 *
	 * @param word	Word to be converted
	 * @return	The conversion
	 */
	public String applySingleConversionTable(final String word){
		final List<String> conversions = applyConversionTable(word);
		if(conversions.size() > 1)
			throw new HunLintException(TOO_MANY_APPLICABLE_RULES.format(new Object[]{word}));

		return (!conversions.isEmpty()? conversions.get(0): word);
	}

	/**
	 * NOTE: does not include the original word!
	 *
	 * @param word	Word to be converted
	 * @return	The list of conversions
	 */
	public List<String> applyConversionTable(final String word){
		final List<String> conversions = new ArrayList<>();
		if(table != null){
			conversions.addAll(applyConversionTable(word, KEY_WHOLE));
			conversions.addAll(applyConversionTable(word, KEY_STARTS_WITH));
			conversions.addAll(applyConversionTable(word, KEY_ENDS_WITH));
			conversions.addAll(applyConversionTable(word, KEY_INSIDE));
		}
		return conversions;
	}

	private List<String> applyConversionTable(final String word, final String key){
		final List<String> conversions = new ArrayList<>();
		final List<Pair<String, String>> list = table.get(key);
		if(list != null){
			final ConversionFunction fun = CONVERSION_TABLE_ADD_METHODS.get(key);
			list.forEach(entry -> fun.convert(word, entry, conversions));
		}
		return conversions;
	}

	private static String reduceKey(final String key){
		return (isStarting(key)? "^": " ") + (isEnding(key)? "$": " ");
	}

	private static boolean isStarting(final String key){
		return (key.charAt(0) == '^');
	}

	private static boolean isEnding(final String key){
		return (key.charAt(key.length() - 1) == '$');
	}

	private static void convertInside(final String word, final Pair<String, String> entry, final List<String> conversions){
		final String key = entry.getKey();

		if(word.contains(key)){
			final String value = (ZERO.equals(entry.getValue())? StringUtils.EMPTY: entry.getValue());
			int keyLength = key.length();
			int valueLength = value.length();

			//search every occurrence of the pattern in the word
			int idx = -valueLength;
			final StringBuffer sb = new StringBuffer();
			while((idx = word.indexOf(key, idx + valueLength)) >= 0){
				sb.append(word);
				sb.replace(idx, idx + keyLength, value);
				conversions.add(sb.toString());

				sb.setLength(0);
			}
		}
	}

	private static void convertStartsWith(final String word, final Pair<String, String> entry, final List<String> conversions){
		final String key = entry.getKey();
		final String strippedKey = key.substring(1);
		if(word.startsWith(strippedKey)){
			final String value = (ZERO.equals(entry.getValue())? StringUtils.EMPTY: entry.getValue());
			conversions.add(value + word.substring(key.length() - 1));
		}
	}

	private static void convertEndsWith(final String word, final Pair<String, String> entry, final List<String> conversions){
		final String key = entry.getKey();
		final int keyLength = key.length() - 1;
		final String strippedKey = key.substring(0, keyLength);
		if(word.endsWith(strippedKey)){
			final String value = (ZERO.equals(entry.getValue())? StringUtils.EMPTY: entry.getValue());
			conversions.add(word.substring(0, word.length() - keyLength) + value);
		}
	}

	private static void convertWhole(final String word, final Pair<String, String> entry, final List<String> conversions){
		final String key = entry.getKey();
		final String strippedKey = key.substring(1, key.length() - 1);
		if(word.equals(strippedKey))
			conversions.add(ZERO.equals(entry.getValue())? StringUtils.EMPTY: entry.getValue());
	}

	@Override
	public String toString(){
		return "[affixOption=" + affixOption + ',' + "table=" + table + ']';
	}

}