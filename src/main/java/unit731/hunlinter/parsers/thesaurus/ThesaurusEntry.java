package unit731.hunlinter.parsers.thesaurus;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.services.StringHelper;


public class ThesaurusEntry implements Comparable<ThesaurusEntry>{

	public static final String PIPE = "|";
	public static final String PART_OF_SPEECH_AND_SYNONYMS_SEPARATOR = PIPE + ":";
	public static final String SYNONYMS_SEPARATOR = PIPE + ",";


	private final String definition;
	private final List<DefinitionSynonymsEntry> synonyms;


	public ThesaurusEntry(final String definition, final List<DefinitionSynonymsEntry> synonyms){
		Objects.requireNonNull(definition);
		Objects.requireNonNull(synonyms);

		this.definition = definition;
		this.synonyms = synonyms;
	}

	public ThesaurusEntry(final String line, final LineNumberReader br) throws IOException{
		Objects.requireNonNull(line);
		Objects.requireNonNull(br);

		//all entries should be in lowercase
		final String[] data = StringUtils.split(line.toLowerCase(Locale.ROOT), PART_OF_SPEECH_AND_SYNONYMS_SEPARATOR);

		definition = data[0];
		final int numEntries = Integer.parseInt(data[1]);
		synonyms = new ArrayList<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			final String definitionAndSynonyms = br.readLine();
			if(definitionAndSynonyms == null)
				throw new EOFException("Unexpected EOF while reading Thesaurus file");

			synonyms.add(new DefinitionSynonymsEntry(definitionAndSynonyms));
		}
	}

	public String getDefinition(){
		return definition;
	}

	public String joinSynonyms(final String separator){
		return StringUtils.join(synonyms, separator);
	}

	public void addSynonym(DefinitionSynonymsEntry definitionSynonymsEntry){
		synonyms.add(definitionSynonymsEntry);
	}

	public int getSynonymsEntries(){
		return synonyms.size();
	}

	public boolean hasSamePartOfSpeech(final String[] partOfSpeeches){
		return synonyms.stream()
			.map(DefinitionSynonymsEntry::getPartOfSpeeches)
			.anyMatch(pos -> Arrays.equals(pos, partOfSpeeches));
	}

	public void saveToIndex(BufferedWriter writer, int idx) throws IOException{
		writer.write(definition);
		writer.write(ThesaurusEntry.PIPE);
		writer.write(Integer.toString(idx));
		writer.write(StringUtils.LF);
	}

	public int saveToData(final BufferedWriter dataWriter, final Charset charset) throws IOException{
		final int synonymsEntries = getSynonymsEntries();
		saveToIndex(dataWriter, synonymsEntries);
		int synonymsLength = 1;
		for(final DefinitionSynonymsEntry synonym : synonyms){
			final String s = synonym.toString();
			dataWriter.write(s);
			dataWriter.write(StringUtils.LF);

			synonymsLength += s.getBytes(charset).length;
		}
		return synonymsLength + StringUtils.LF.length() * synonymsEntries;
	}

	public boolean contains(final String[] partOfSpeeches, final String[] synonyms){
		final List<String> ss = Arrays.asList(synonyms);
		return (ss.remove(definition) && ArrayUtils.contains(synonyms, definition) && this.synonyms.stream().anyMatch(entry -> entry.containsAllSynonyms(partOfSpeeches, ss)));
	}

	public boolean containsSynonym(final String synonym){
		return synonyms.stream()
			.anyMatch(entry -> entry.containsSynonym(synonym));
	}

	@Override
	public String toString(){
		return synonyms.stream()
			.map(DefinitionSynonymsEntry::toString)
			.map(s -> definition + ": " + String.join(", ", s))
			.collect(Collectors.joining("\\r\\n"));
	}

	@Override
	public int compareTo(final ThesaurusEntry other){
		return new CompareToBuilder()
			.append(definition, other.definition)
			.append(synonyms, other.synonyms)
			.toComparison();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final ThesaurusEntry rhs = (ThesaurusEntry)obj;
		return new EqualsBuilder()
			.append(definition, rhs.definition)
			.append(synonyms, rhs.synonyms)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(definition)
			.append(synonyms)
			.toHashCode();
	}

}
