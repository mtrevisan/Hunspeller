package unit731.hunspeller.parsers.dictionary.dtos;

import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.Getter;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.languages.builders.OrthographyBuilder;
import unit731.hunspeller.parsers.dictionary.valueobjects.Frequency;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;


/**
 * @see <a href="https://home.ubalt.edu/ntsbarsh/Business-stat/otherapplets/PoissonTest.htm">Goodness-of-Fit for Poisson</a>
 */
@Getter
public class DictionaryStatistics{

	public static final DecimalFormat PERCENT_FORMATTER = (DecimalFormat)NumberFormat.getInstance(Locale.US);
	static{
		DecimalFormatSymbols symbols = PERCENT_FORMATTER.getDecimalFormatSymbols();
		PERCENT_FORMATTER.setMultiplier(100);
		PERCENT_FORMATTER.setPositiveSuffix("%");
		symbols.setGroupingSeparator(' ');
		PERCENT_FORMATTER.setDecimalFormatSymbols(symbols);
		PERCENT_FORMATTER.setMinimumFractionDigits(1);
		PERCENT_FORMATTER.setMaximumFractionDigits(1);
	}


	private int totalProductions;
	private final Frequency<Integer> lengthsFrequencies = new Frequency<>();
	private final Frequency<Integer> syllabeLengthsFrequencies = new Frequency<>();
	private final Frequency<Integer> stressFromLastFrequencies = new Frequency<>();
	private final Frequency<String> syllabesFrequencies = new Frequency<>();
	private int longestWordCountByCharacters;
	private final List<String> longestWordsByCharacters = new ArrayList<>();
	private int longestWordCountBySyllabes;
	private final List<String> longestWordsBySyllabes = new ArrayList<>();
	private final BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(BitArrayBuilder.Type.FAST, 40_000_000, 0.000_000_01, 1.3);

	@Getter
	private final Orthography orthography;


	public DictionaryStatistics(String language, Charset charset){
		bloomFilter.setCharset(charset);
		orthography = OrthographyBuilder.getOrthography(language);
	}

	public void addData(Hyphenation hyphenation){
		if(!hyphenation.hasErrors()){
			List<String> syllabes = hyphenation.getSyllabes();

			List<Integer> stressIndexes = getStressIndexFromLast(syllabes);
			if(stressIndexes != null)
				stressFromLastFrequencies.addValue(stressIndexes.get(stressIndexes.size() - 1));
			int syllabeCount = syllabes.size();
			syllabeLengthsFrequencies.addValue(syllabeCount);
			StringBuilder sb = new StringBuilder();
			for(String syllabe : syllabes){
				sb.append(syllabe);
				if(orthography.countGraphemes(syllabe) == syllabe.length())
					syllabesFrequencies.addValue(syllabe);
			}
			lengthsFrequencies.addValue(sb.length());
			storeLongestWord(sb.toString(), syllabeCount);
			totalProductions ++;
		}
	}

	private List<Integer> getStressIndexFromLast(List<String> syllabes){
		int size = syllabes.size() - 1;
		for(int i = 0; i <= size; i ++)
			if(orthography.hasStressedGrapheme(syllabes.get(size - i)))
				return Arrays.asList(i);
		return null;
	}

	private void storeLongestWord(String word, int syllabes){
		int letterCount = orthography.countGraphemes(word);
		if(letterCount > longestWordCountByCharacters){
			longestWordsByCharacters.clear();
			longestWordsByCharacters.add(word);
			longestWordCountByCharacters = letterCount;
		}
		else if(letterCount == longestWordCountByCharacters)
			longestWordsByCharacters.add(word);

		if(syllabes > longestWordCountBySyllabes){
			longestWordsBySyllabes.clear();
			longestWordsBySyllabes.add(word);
			longestWordCountBySyllabes = letterCount;
		}
		else if(letterCount == longestWordCountBySyllabes)
			longestWordsBySyllabes.add(word);

		bloomFilter.add(word);
	}

	public long getTotalProductions(){
		return totalProductions;
	}

	public List<String> getMostCommonSyllabes(int size){
		return syllabesFrequencies.getMostCommonValues(5).stream()
			.map(value -> value + " (" + PERCENT_FORMATTER.format(syllabesFrequencies.getPercentOf(value)) + ")")
			.collect(Collectors.toList());
//		return syllabesFrequencies.getMode().stream()
//			.limit(size)
//			.map(String.class::cast)
//			.collect(Collectors.toList());
	}

	/** Returns the percentage of unique words */
	public double uniqueWords(){
		return (double)bloomFilter.getAddedElements() / totalProductions;
	}

	public void clear(){
		totalProductions = 0;
		lengthsFrequencies.clear();
		syllabeLengthsFrequencies.clear();
		stressFromLastFrequencies.clear();
		syllabesFrequencies.clear();
		longestWordCountByCharacters = 0;
		longestWordsByCharacters.clear();
		longestWordCountBySyllabes = 0;
		longestWordsBySyllabes.clear();
		bloomFilter.clear();
	}

}
