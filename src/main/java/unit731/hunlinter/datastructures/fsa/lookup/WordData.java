package unit731.hunlinter.datastructures.fsa.lookup;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


/** Stem and tag data associated with a given word */
public class WordData{

	/** inflected word form data */
	private String word;
	private String stem;
	private String tag;


	WordData(){}

	WordData(final String stem, final String tag){
		this.stem = stem;
		this.tag = tag;
	}

	/**
	 * @return	Inflected word form data. Usually the parameter passed to {@link DictionaryLookup#lookup(String)}.
	 */
	public String getWord(){
		return word;
	}

	/**
	 * @return	Stem data decoded to a character sequence or <code>null</code> if no associated stem data exists.
	 */
	public String getStem(){
		return stem;
	}

	/**
	 * @return	Tag data decoded to a character sequence or <code>null</code> if no associated tag data exists.
	 */
	public String getTag(){
		return tag;
	}

	void setWord(final String word){
		this.word = word;
	}

	void setStem(final String stem){
		this.stem = stem;
	}

	void setTag(final String tag){
		this.tag = tag;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final WordData rhs = (WordData)obj;
		return new EqualsBuilder()
			.append(word, rhs.word)
			.append(stem, rhs.stem)
			.append(tag, rhs.tag)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(word)
			.append(stem)
			.append(tag)
			.toHashCode();
	}

	@Override
	public String toString(){
		return "WordData[" + word + "," + stem + "," + tag + "]";
	}

}
