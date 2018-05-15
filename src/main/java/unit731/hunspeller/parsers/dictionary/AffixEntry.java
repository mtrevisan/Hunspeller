package unit731.hunspeller.parsers.dictionary;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


@Slf4j
@EqualsAndHashCode(of = "entry")
public class AffixEntry{

	private static final Matcher REGEX_ENTRY = PatternService.matcher("\t.*$");

	private static final Pattern PATTERN_SEPARATOR = PatternService.pattern("[\\s\\t]+");
	private static final Pattern PATTERN_SLASH = PatternService.pattern("/");

	private static final String POINT = ".";
	private static final String ZERO = "0";

	private static final String REGEX_CONDITION_SUFFIX = "%s$";
	private static final String REGEX_CONDITION_PREFIX = "^%s";


	@Getter
	public static enum Type{
		SUFFIX("SFX"),
		PREFIX("PFX");


		private final String flag;

		Type(String flag){
			this.flag = flag;
		}

		public static Type toEnum(String flag){
			Type[] types = Type.values();
			for(Type type : types)
				if(type.getFlag().equals(flag))
					return type;
			return null;
		}
	};


	@Getter private final Type type;
	//ID used to represent the affix
	@Getter private final String flag;
	@Getter private final String[] ruleFlags;
	@Getter private final Matcher match;
	//string to strip
	private final Matcher remove;
	//string to append
	private final String add;
	@Getter private final String[] dataFields;

	private final String entry;


	public AffixEntry(String line, FlagParsingStrategy strategy){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		String[] lineParts = PatternService.split(line, PATTERN_SEPARATOR, 6);
		String ruleType = lineParts[0];
		this.flag = lineParts[1];
		String regexToRemove = lineParts[2];
		String[] additionParts = PatternService.split(lineParts[3], PATTERN_SLASH);
		String addition = additionParts[0];
		String regexToMatch = (lineParts.length > 4? lineParts[4]: POINT);
		dataFields = (lineParts.length > 5? PatternService.split(lineParts[5], PATTERN_SEPARATOR): new String[0]);

		type = Type.toEnum(ruleType);
		String[] classes = strategy.parseRuleFlags((additionParts.length > 1? additionParts[1]: null));
		ruleFlags = (classes.length > 0? classes: null);
		String conditionPattern = (isSuffix()? REGEX_CONDITION_SUFFIX: REGEX_CONDITION_PREFIX);
		match = (!POINT.equals(regexToMatch)? PatternService.matcher(String.format(conditionPattern, regexToMatch)): null);
		remove = (!ZERO.equals(regexToRemove)? PatternService.matcher(String.format(conditionPattern, regexToRemove)): null);
		add = (ZERO.equals(addition)? StringUtils.EMPTY: addition);

		if(isSuffix() && StringUtils.isNotBlank(regexToRemove) && addition.length() > 1 && regexToRemove.charAt(0) == addition.charAt(0))
			log.warn("This line has characters in common between removed and added part: " + line);

		entry = PatternService.clear(line, REGEX_ENTRY);
	}

	public final boolean isSuffix(){
		return (type == Type.SUFFIX);
	}

	public String applyRule(String word, boolean isFullstrip) throws IllegalArgumentException{
		if(remove != null){
			word = PatternService.clear(word, remove);
			if(word.isEmpty() && !isFullstrip)
				throw new IllegalArgumentException("Cannot strip full words without the flag FULLSTRIP");
		}

		return (isSuffix()? word + add: add + word);
	}

	@Override
	public String toString(){
		return entry;
	}

}
