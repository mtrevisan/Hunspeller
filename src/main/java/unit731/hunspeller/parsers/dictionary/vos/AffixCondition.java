package unit731.hunspeller.parsers.dictionary.vos;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;
import unit731.hunspeller.services.PatternHelper;


public class AffixCondition{

	private static final Pattern PATTERN_CONDITION_SPLITTER = PatternHelper.pattern("(?<!\\[\\^?)(?![^\\[]*\\])");


	private final String[] condition;


	public AffixCondition(String condition, AffixEntry.Type affixType){
		this.condition = PatternHelper.split(condition, PATTERN_CONDITION_SPLITTER);

		if(affixType == AffixEntry.Type.SUFFIX)
			//invert condition
			Collections.reverse(Arrays.asList(this.condition));
	}

	public boolean match(String word, AffixEntry.Type type){
		boolean match = false;

		int size = word.length();
		//if the length of the condition is greater than the length of the word then the rule cannot be applied
		if(condition.length <= size){
			match = true;

			int idxWord = (type == AffixEntry.Type.PREFIX? 0: size - 1);
			for(String conditionPart : condition){
				if(idxWord < 0 || idxWord >= size){
					match = false;
					break;
				}

				char firstChar = conditionPart.charAt(0);
				if(firstChar != '.'){
					if(firstChar == '['){
						boolean negatedGroup = (conditionPart.charAt(1) == '^');
						//extract inside of group
						conditionPart = conditionPart.substring(1 + (negatedGroup? 1: 0), conditionPart.length() - 1);
						match = (negatedGroup ^ conditionPart.indexOf(word.charAt(idxWord)) >= 0);
					}
					else
						match = (word.charAt(idxWord) == firstChar);

					if(!match)
						break;
				}

				idxWord += (type == AffixEntry.Type.PREFIX? 1: -1);
			}
		}

		return match;
	}

}
