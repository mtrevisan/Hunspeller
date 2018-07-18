package unit731.hunspeller.parsers.hyphenation.dtos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;


@AllArgsConstructor
public class HyphenationBreak{

	public static final Pair<Integer, String> EMPTY_PAIR = Pair.of(0, null);


	@NonNull
	private Map<Integer, Pair<Integer, String>> indexesAndRules = new HashMap<>();
	@Getter
	private final int size;


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

	public boolean enforceNoHyphens(List<String> syllabes, Set<String> noHyphen){
		boolean modified = false;
		int size = syllabes.size();
		for(String nohyp : noHyphen){
			int nohypLength = nohyp.length();
			if(nohyp.charAt(0) == '^'){
				if(syllabes.get(0).startsWith(nohyp.substring(1))){
					resetBreakpoint(0);
					resetBreakpoint(nohypLength - 1);

					modified = true;
				}
			}
			else if(nohyp.charAt(nohypLength - 1) == '$'){
				if(syllabes.get(syllabes.size() - 1).endsWith(nohyp.substring(0, nohypLength - 1))){
					resetBreakpoint(size - nohypLength - 1);
					resetBreakpoint(size - 2);

					modified = true;
				}
			}
			else
				for(int i = 0; i < size; i ++)
					if(nohyp.equals(syllabes.get(i))){
						resetBreakpoint(i);
						resetBreakpoint(i + nohypLength);

						modified = true;
					}
		}
		return modified;
	}

	private void resetBreakpoint(int index){
		if(index < indexesAndRules.size())
			indexesAndRules.put(index, EMPTY_PAIR);
	}

}
