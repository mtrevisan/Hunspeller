package unit731.hunspeller.languages;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orthography{

	private static final Matcher REGEX_APOSTROPHE = Pattern.compile("['‘’]").matcher(StringUtils.EMPTY);

	protected static final String APOSTROPHE = "ʼ";

	private static class SingletonHelper{
		private static final Orthography INSTANCE = new Orthography();
	}


	public static Orthography getInstance(){
		return SingletonHelper.INSTANCE;
	}

	public String correctOrthography(String word){
		//apply stress
		return correctApostrophes(word);
	}

	protected String correctApostrophes(String word){
		return PatternService.replaceAll(word, REGEX_APOSTROPHE, APOSTROPHE);
	}

	public boolean[] getSyllabationErrors(List<String> syllabes){
		return new boolean[syllabes.size()];
	}

}
