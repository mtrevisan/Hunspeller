package unit731.hunspeller.parsers.thesaurus;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public class ThesaurusEntry implements Comparable<ThesaurusEntry>{

	private static final String PIPE = "|";
	private static final String ESCAPED_PIPE = Pattern.quote(PIPE);

	private String synonym;
	@Setter private List<MeaningEntry> meanings;


	public ThesaurusEntry(String line, BufferedReader br) throws IOException{
		Objects.nonNull(line);
		Objects.nonNull(br);

		String[] data = line.split(ESCAPED_PIPE);

		synonym = data[0];
		int numEntries = Integer.parseInt(data[1]);
		meanings = new ArrayList<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			String meaning = br.readLine();
			meanings.add(new MeaningEntry(meaning));
		}
	}

	@Override
	public String toString(){
		StringJoiner sj = new StringJoiner(": ");
		sj.add(synonym);
		for(MeaningEntry meaning : meanings)
			sj.add(StringUtils.join(meaning, ", "));
		return sj.toString();
	}

	@Override
	public int compareTo(ThesaurusEntry other){
		return new CompareToBuilder()
			.append(synonym, other.synonym)
			.toComparison();
	}

}
