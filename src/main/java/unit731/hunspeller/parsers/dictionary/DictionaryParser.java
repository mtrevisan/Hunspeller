package unit731.hunspeller.parsers.dictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.externalsorter.ExternalSorter;


public class DictionaryParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryParser.class);

	private static final Pattern PATTERN_COMMENT = PatternHelper.pattern("(^\\s*|\\s+)[#\\/].*$");

	//thin space
	public static final char COUNTER_GROUPING_SEPARATOR = '\u2009';
	//figure space
//	public static final char COUNTER_GROUPING_SEPARATOR = '\u2007';
	public static final DecimalFormat COUNTER_FORMATTER = (DecimalFormat)NumberFormat.getInstance(Locale.US);
	static{
		DecimalFormatSymbols symbols = COUNTER_FORMATTER.getDecimalFormatSymbols();
		symbols.setGroupingSeparator(COUNTER_GROUPING_SEPARATOR);
		COUNTER_FORMATTER.setDecimalFormatSymbols(symbols);
	}
	public static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("0.#####%", DecimalFormatSymbols.getInstance(Locale.US));
	public static final DecimalFormat PERCENT_FORMATTER_1 = new DecimalFormat("0.0%", DecimalFormatSymbols.getInstance(Locale.US));
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US);
	public static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");


	private final File dicFile;
	private final String language;
	private final Charset charset;
	private final ExternalSorter sorter = new ExternalSorter();

	private final NavigableMap<Integer, Integer> boundaries = new TreeMap<>();


	public DictionaryParser(File dicFile, String language, Charset charset){
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(charset);

		this.dicFile = dicFile;
		this.language = language;
		this.charset = charset;
	}

	public File getDicFile(){
		return dicFile;
	}

	public String getLanguage(){
		return language;
	}

	public Charset getCharset(){
		return charset;
	}

	public ExternalSorter getSorter(){
		return sorter;
	}

	public NavigableMap<Integer, Integer> getBoundaries(){
		return boundaries;
	}

	//sorter worker
	public final Map.Entry<Integer, Integer> getBoundary(int lineIndex){
		return Optional.ofNullable(boundaries.floorEntry(lineIndex))
			.filter(e -> lineIndex <= e.getValue())
			.orElse(null);
	}

	public final int getBoundaryIndex(int lineIndex){
		calculateDictionaryBoundaries();

		return searchBoundary(lineIndex)
			.map(e -> boundaries.headMap(lineIndex, true).size() - 1)
			.orElse(-1);
	}

	public final int getNextBoundaryIndex(int lineIndex){
		return Optional.ofNullable(boundaries.higherEntry(lineIndex))
			.map(Map.Entry::getKey)
			.orElse(-1);
	}

	public final int getPreviousBoundaryIndex(int lineIndex){
		return Optional.ofNullable(boundaries.lowerEntry(lineIndex))
			.map(Map.Entry::getKey)
			.orElse(-1);
	}

	public final boolean isInBoundary(int lineIndex){
		return searchBoundary(lineIndex)
			.isPresent();
	}

	private Optional<Map.Entry<Integer, Integer>> searchBoundary(int lineIndex){
		return Optional.ofNullable(boundaries.floorEntry(lineIndex))
			.filter(e -> lineIndex <= e.getValue());
	}

	public final void calculateDictionaryBoundaries(){
		if(boundaries.isEmpty()){
			int lineIndex = 0;
			try(BufferedReader br = Files.newBufferedReader(dicFile.toPath(), charset)){
				String prevLine = null;
				String line;
				int startSection = -1;
				boolean needSorting = false;
				Comparator<String> comparator = ComparatorBuilder.getComparator(language);
				while((line = br.readLine()) != null){
					if(isComment(line) || StringUtils.isBlank(line)){
						if(startSection >= 0){
							//filter out single word that doesn't need to be sorted
							if(lineIndex - startSection > 2 && needSorting)
								boundaries.put(startSection, lineIndex - 1);
							prevLine = null;
							startSection = -1;
							needSorting = false;
						}
					}
					else{
						if(startSection < 0)
							startSection = lineIndex;

						if(!needSorting && StringUtils.isNotBlank(prevLine))
							needSorting = (comparator.compare(line, prevLine) < 0);
						prevLine = line;
					}

					lineIndex ++;
				}
				//filter out single word that doesn't need to be sorted
				if(startSection >= 0 && lineIndex - startSection > 2 && needSorting)
					boundaries.put(startSection, lineIndex - 1);
			}
			catch(IOException e){
				LOGGER.error(null, e);
			}
		}
	}


	private boolean isComment(String line){
		return PatternHelper.find(line, PATTERN_COMMENT);
	}

	public final void clear(){
		if(boundaries != null)
			boundaries.clear();
	}

	/**
	 * Removes comment lines and then cleans up blank lines and trailing whitespace.
	 * 
	 * @param line	The line to be cleaned
	 * @return	The cleaned line (withou comments or spaces at the beginning or at the end)
	 */
	public static String cleanLine(String line){
		//remove comments
		line = PatternHelper.clear(line, PATTERN_COMMENT);
		//trim the entire string
		line = StringUtils.strip(line);
		return line;
	}

}
