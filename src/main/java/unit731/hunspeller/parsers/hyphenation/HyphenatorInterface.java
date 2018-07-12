package unit731.hunspeller.parsers.hyphenation;

import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationInterface;


public interface HyphenatorInterface{

	HyphenationInterface hyphenate(String word);

	HyphenationInterface hyphenate(String word, String addedRule, HyphenationParser.Level level) throws CloneNotSupportedException;

}
