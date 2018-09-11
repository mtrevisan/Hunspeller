package unit731.hunspeller.services.regexgenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import lombok.AllArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;


/**
 * The {@code NFA} class provides a data type for creating a <em>Nondeterministic Finite state Automaton</em> (NFA) from a regular
 * expression and testing whether a given string is matched by that regular expression.
 * <p>
 * It supports the following operations: <em>concatenation</em> <em>closure</em>, <em>binary or</em>, and <em>parentheses</em>.
 * It does not support <em>mutiway or</em>, <em>character classes</em>, <em>metacharacters</em> (either in the text or pattern),
 * <em>capturing capabilities</em>, <em>greedy</em> or <em>relucantant</em> modifiers, and other features in industrial-strength implementations
 * such as {@link java.util.regex.Pattern} and {@link java.util.regex.Matcher}.
 * <p>
 * This implementation builds the NFA using a digraph and a stack and simulates the NFA using digraph search (see the textbook for details).
 * The constructor takes time proportional to <em>m</em>, where <em>m</em> is the number of characters in the regular expression.
 * <p>
 * The <em>recognizes</em> method takes time proportional to <em>m n</em>, where <em>n</em> is the number of characters in the text.
 * <p>
 * For additional documentation, see <a href="https://algs4.cs.princeton.edu/54regexp">Section 5.4</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 * 
 * @see <a href="https://algs4.cs.princeton.edu/54regexp/NFA.java.html">NFA.java</a>
 * @see <a href="https://algs4.cs.princeton.edu/lectures/54RegularExpressions.pdf">Algorithms - Robert Sedgewick, Kevin Wayne</a>
 * @see <a href="https://www.dennis-grinch.co.uk/tutorial/enfa">ε-NFA in Java</a>
 * @see <a href="https://pdfs.semanticscholar.org/presentation/e14c/b69f0feb2856734a5e5e85b6ae1a210ab936.pdf">Automata & Languages</a>
 * @see <a href="http://www.dfki.de/compling/pdfs/SS06-fsa-presentation.pdf">Finite-State Automata and Algorithms</a>
 */
@ToString
public class HunspellRegexWordGenerator{

	@AllArgsConstructor
	private static class GeneratedElement{
		private final List<String> word;
		private final int stateIndex;
	}


	private final Digraph<String> graph = new Digraph<>();
	private final int finalStateIndex;


	/**
	 * Initializes the NFA from the specified regular expression.
	 * <p>
	 * NOTE: each element should be enclosed in parentheses (eg. <code>(as)(ert)?(b)*</code>), the managed operations are <code>*</code> and <code>?</code>
	 *
	 * @param regexpParts	The regular expression already subdivided into input and modifiers (eg. ["ag", "ert", "?", "b", "*"])
	 */
	public HunspellRegexWordGenerator(String[] regexpParts){
		int offset = 0;
		for(int i = 0; i + offset < regexpParts.length; i ++){
			int operatorIndex = i + offset + 1;
			char next = (operatorIndex < regexpParts.length && regexpParts[operatorIndex].length() == 1? regexpParts[operatorIndex].charAt(0): 0);
			switch(next){
				//zero or more
				case '*':
					graph.addEdge(i, i + 1);
					graph.addEdge(i, i, regexpParts[operatorIndex - 1]);

					//skip operator
					offset ++;
					break;

				//zero or one
				case '?':
					graph.addEdge(i, i + 1, regexpParts[operatorIndex - 1]);
					graph.addEdge(i, i + 1);

					//skip operator
					offset ++;
					break;

				default:
					//one
					graph.addEdge(i, i + 1, regexpParts[operatorIndex - 1]);
			}
		}

		finalStateIndex = regexpParts.length - offset;
	}

	/**
	 * Generate a subList with a maximum size of <code>limit</code> of words that matches the given regex.
	 * <p>
	 * The Strings are ordered in lexicographical order.
	 *
	 * @param minimumSubwords	The minimum number of compounds that forms the generated word
	 * @param limit	The maximum size of the list
	 * @return	The list of words that matcher the given regex
	 */
	public List<List<String>> generateAll(int minimumSubwords, int limit){
		List<List<String>> matchedWords = new ArrayList<>(limit);

		Queue<GeneratedElement> queue = new LinkedList<>();
		queue.add(new GeneratedElement(new ArrayList<>(0), 0));
		while(!queue.isEmpty()){
			GeneratedElement elem = queue.remove();
			List<String> subword = elem.word;
			int stateIndex = elem.stateIndex;

			//final state not reached, add transitions
			if(stateIndex < finalStateIndex){
				Iterable<Pair<Integer, String>> transitions = graph.adjacentVertices(stateIndex);
				for(Pair<Integer, String> transition : transitions){
					int key = transition.getKey();
					String value = transition.getValue();

					List<String> nextword = subword;
					if(StringUtils.isNotBlank(value)){
						nextword = new ArrayList<>(subword);
						nextword.add(value);
					}

					queue.add(new GeneratedElement(nextword, key));
				}
			}
			//if this is the accepting state add the generated word (skip empty generated word)
			else if(subword.size() >= minimumSubwords){
				matchedWords.add(subword);

				if(matchedWords.size() == limit)
					break;
			}
		}

		return matchedWords;
	}

}
