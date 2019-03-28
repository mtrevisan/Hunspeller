package unit731.hunspeller.parsers.hyphenation.dtos;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;


public class HyphenationBreak{

	@FunctionalInterface
	public interface NoHyphenationManageFunction{
		int manage(Map<Integer, Pair<Integer, String>> indexesAndRules, List<String> syllabes, String nohyp, int wordLength, int syllabesCount);
	}

	private static final Map<String, NoHyphenationManageFunction> NO_HYPHENATION_MANAGE_METHODS = new HashMap<>();
	static{
		NO_HYPHENATION_MANAGE_METHODS.put("  ", HyphenationBreak::manageInside);
		NO_HYPHENATION_MANAGE_METHODS.put("^ ", HyphenationBreak::manageStartsWith);
		NO_HYPHENATION_MANAGE_METHODS.put(" $", HyphenationBreak::manageEndsWith);
		NO_HYPHENATION_MANAGE_METHODS.put("^$", HyphenationBreak::manageWhole);
	}

	public static final Pair<Integer, String> EMPTY_PAIR = Pair.of(0, null);


	private final Map<Integer, Pair<Integer, String>> indexesAndRules;
	private final int size;


	public static HyphenationBreak getEmptyInstance(){
		return new HyphenationBreak(Collections.<Integer, Pair<Integer, String>>emptyMap(), 0);
	}

	public HyphenationBreak(Map<Integer, Pair<Integer, String>> indexesAndRules, int size){
		Objects.requireNonNull(indexesAndRules);

		this.indexesAndRules = indexesAndRules;
		this.size = size;
	}

	public int getSize(){
		return size;
	}

	public boolean isBreakpoint(int index){
		return (indexesAndRules.getOrDefault(index, EMPTY_PAIR).getKey() % 2 != 0);
	}

	public String getRule(int index){
		return indexesAndRules.getOrDefault(index, EMPTY_PAIR).getValue();
	}

	public List<String> getRules(){
		return indexesAndRules.values().stream()
			.map(Pair::getValue)
			.collect(Collectors.toList());
	}

	public void enforceNoHyphens(List<String> syllabes, Set<String> noHyphen){
		int syllabesCount = syllabes.size();
		if(syllabesCount > 1){
			int wordLength = syllabes.stream()
				.map(String::length)
				.mapToInt(x -> x)
				.sum();
			for(String nohyp : noHyphen){
				String reducedKey = reduceKey(nohyp);
				NoHyphenationManageFunction fun = NO_HYPHENATION_MANAGE_METHODS.get(reducedKey);
				syllabesCount = fun.manage(indexesAndRules, syllabes, nohyp, wordLength, syllabesCount);
				if(syllabesCount <= 1)
					break;
			}
		}
	}

	private static int manageInside(Map<Integer, Pair<Integer, String>> indexesAndRules, List<String> syllabes, String nohyp, int wordLength,
			int syllabesCount){
		int nohypLength = nohyp.length();

		int index = 0;
		for(int i = 0; syllabesCount > 1 && i < syllabesCount; i ++){
			String syllabe = syllabes.get(i);
			
			if(syllabe.equals(nohyp)){
				indexesAndRules.remove(index);
				indexesAndRules.remove(index + nohypLength);
				
				if(i == 0){
					//merge syllabe with following
					String removedSyllabe = syllabes.remove(0);
					syllabes.set(0, removedSyllabe + syllabes.get(0));
					
					syllabesCount --;
				}
				else if(i == syllabesCount - 1){
					//merge syllabe with previous
					syllabesCount --;

					String removedSyllabe = syllabes.remove(syllabesCount);
					syllabes.set(syllabesCount - 1, syllabes.get(syllabesCount - 1) + removedSyllabe);
				}
				else{
					//merge syllabe with previous
					String removedSyllabe1 = (i >= 0? syllabes.remove(i): StringUtils.EMPTY);
					String removedSyllabe0 = (i >= 0? syllabes.remove(i): StringUtils.EMPTY);
					if(syllabes.isEmpty())
						syllabes.add(removedSyllabe1 + removedSyllabe0);
					else
						syllabes.set(i - 1, syllabes.get(i - 1) + removedSyllabe1 + removedSyllabe0);
					
					syllabesCount -= 2;
				}
				
				i --;
			}
			
			index += syllabe.length();
		}
		return syllabesCount;
	}

	private static int manageStartsWith(Map<Integer, Pair<Integer, String>> indexesAndRules, List<String> syllabes, String nohyp, int wordLength,
			int syllabesCount){
		if(syllabes.get(0).equals(nohyp.substring(1))){
			indexesAndRules.remove(1);
			indexesAndRules.remove(nohyp.length());
			
			if(syllabesCount > 1){
				//merge syllabe with following
				String removedSyllabe = syllabes.remove(0);
				syllabes.set(0, removedSyllabe + syllabes.get(0));
				
				syllabesCount --;
			}
		}
		return syllabesCount;
	}

	private static int manageEndsWith(Map<Integer, Pair<Integer, String>> indexesAndRules, List<String> syllabes, String nohyp, int wordLength,
			int syllabesCount){
		int nohypLength = nohyp.length();
		if(syllabes.get(syllabesCount - 1).equals(nohyp.substring(0, nohypLength - 1))){
			indexesAndRules.remove(wordLength - nohypLength - 1);
			indexesAndRules.remove(wordLength - 1);
			
			if(syllabesCount > 1){
				//merge syllabe with previous
				syllabesCount --;

				String removedSyllabe = syllabes.remove(syllabesCount);
				syllabes.set(syllabesCount - 1, syllabes.get(syllabesCount - 1) + removedSyllabe);
			}
		}
		return syllabesCount;
	}

	private static int manageWhole(Map<Integer, Pair<Integer, String>> indexesAndRules, List<String> syllabes, String nohyp, int wordLength,
			int syllabesCount){
		nohyp = nohyp.substring(1, nohyp.length() - 1);
		return manageInside(indexesAndRules, syllabes, nohyp, wordLength, syllabesCount);
	}

	private String reduceKey(String key){
		return (isStarting(key)? "^": " ") + (isEnding(key)? "$": " ");
	}

	private boolean isStarting(String key){
		return (key.charAt(0) == '^');
	}

	private boolean isEnding(String key){
		return (key.charAt(key.length() - 1) == '$');
	}

}
