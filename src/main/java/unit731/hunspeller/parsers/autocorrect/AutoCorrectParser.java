package unit731.hunspeller.parsers.autocorrect;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import unit731.hunspeller.parsers.thesaurus.DuplicationResult;
import unit731.hunspeller.services.XMLParser;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;


/*
The autocorrect file contains 3 XML files:
- SentenceExceptList.xml – abbreviations that end with a fullstop that should be ignored when determining the end of a sentence
- WordExceptList.xml – Words that may contain more than 2 leading capital eg. CDs
*/
/** Manages pairs of mistyped words and their correct spelling */
public class AutoCorrectParser{

	private static final MessageFormat BAD_INCORRECT_QUOTE = new MessageFormat("Incorrect form cannot contain apostrophes or double quotes: ''{0}''");
	private static final MessageFormat BAD_CORRECT_QUOTE = new MessageFormat("Correct form cannot contain apostrophes or double quotes: ''{0}''");
	private static final MessageFormat DUPLICATE_DETECTED = new MessageFormat("Duplicate detected for ''{0}''");

	private static final String AUTO_CORRECT_ROOT_ELEMENT = "block-list:block-list";
	private static final String AUTO_CORRECT_BLOCK = "block-list:block";
	private static final String AUTO_CORRECT_INCORRECT_FORM = "block-list:abbreviated-name";
	private static final String AUTO_CORRECT_CORRECT_FORM = "block-list:name";

	private static final Pair<String, String>[] XML_PROPERTIES = new Pair[]{
		Pair.of(OutputKeys.VERSION, "1.0"),
		Pair.of(OutputKeys.ENCODING, StandardCharsets.UTF_8.name())
	};


	private final List<CorrectionEntry> dictionary = new ArrayList<>();


	/**
	 * Parse the rows out from a `DocumentList.xml` file.
	 *
	 * @param acoPath	The reference to the auto-correct file
	 * @throws IOException	If an I/O error occurs
	 * @throws SAXException	If an parsing error occurs on the `xml` file
	 */
	public void parse(final Path acoPath) throws IOException, SAXException{
		clear();

		final Document doc = XMLParser.parseXMLDocument(acoPath);

		final Element rootElement = doc.getDocumentElement();
		if(!AUTO_CORRECT_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new IllegalArgumentException("Invalid root element, expected '" + AUTO_CORRECT_ROOT_ELEMENT + "', was "
				+ rootElement.getNodeName());

		final NodeList entries = rootElement.getChildNodes();
		for(int i = 0; i < entries.getLength(); i ++){
			final Node entry = entries.item(i);
			if(XMLParser.isElement(entry, AUTO_CORRECT_BLOCK)){
				final Node mediaType = XMLParser.extractAttribute(entry, AUTO_CORRECT_INCORRECT_FORM);
				if(mediaType != null)
					dictionary.add(new CorrectionEntry(mediaType.getNodeValue(),
						XMLParser.extractAttributeValue(entry, AUTO_CORRECT_CORRECT_FORM)));
			}
		}
	}

	public List<CorrectionEntry> getCorrectionsDictionary(){
		return dictionary;
	}

	public int getCorrectionsCounter(){
		return dictionary.size();
	}

	public void setCorrection(final int index, final String incorrect, final String correct){
		dictionary.set(index, new CorrectionEntry(incorrect, correct));
	}

	/**
	 * @param incorrect	The incorrect form
	 * @param correct	The correct form
	 * @param duplicatesDiscriminator	Function called to ask the user what to do if duplicates are found
	 * 	(return <code>true</code> to force insertion)
	 * @return The duplication result
	 */
	public DuplicationResult<CorrectionEntry> insertCorrection(final String incorrect, final String correct,
			final Supplier<Boolean> duplicatesDiscriminator){
		if(incorrect.contains("'") || incorrect.contains("\""))
			throw new IllegalArgumentException(BAD_INCORRECT_QUOTE.format(new Object[]{incorrect}));
		if(correct.contains("'") || correct.contains("\""))
			throw new IllegalArgumentException(BAD_CORRECT_QUOTE.format(new Object[]{incorrect}));

		boolean forceInsertion = false;
		final List<CorrectionEntry> duplicates = extractDuplicates(incorrect, correct);
		if(!duplicates.isEmpty()){
			forceInsertion = duplicatesDiscriminator.get();
			if(!forceInsertion)
				throw new IllegalArgumentException(DUPLICATE_DETECTED.format(
					new Object[]{duplicates.stream().map(CorrectionEntry::toString).collect(Collectors.joining(", "))}));
		}

		if(duplicates.isEmpty() || forceInsertion)
			dictionary.add(new CorrectionEntry(incorrect, correct));

		return new DuplicationResult(duplicates, forceInsertion);
	}

	public void deleteCorrections(final int[] selectedRowIDs){
		final int count = selectedRowIDs.length;
		for(int i = 0; i < count; i ++)
			dictionary.remove(selectedRowIDs[i] - i);
	}

	/** Find if there is a duplicate with the same incorrect and correct forms */
	private List<CorrectionEntry> extractDuplicates(final String incorrect, final String correct){
		return dictionary.stream()
			.filter(correction -> correction.getIncorrectForm().equals(incorrect) && correction.getCorrectForm().equals(correct))
			.collect(Collectors.toList());
	}

	/** Find if there is a duplicate with the same incorrect and correct forms */
	public boolean isAlreadyContained(final String incorrect, final String correct){
		return dictionary.stream()
			.anyMatch(elem -> !incorrect.isEmpty() && !correct.isEmpty()
				&& elem.getIncorrectForm().equals(incorrect) && elem.getCorrectForm().equals(correct));
	}

	public static Pair<String, String> extractComponentsForFilter(final String incorrect, final String correct){
		return Pair.of(clearFilter(incorrect), clearFilter(correct));
	}

	private static String clearFilter(final String text){
		//escape special characters
		return Matcher.quoteReplacement(StringUtils.strip(text));
	}

	public static Pair<String, String> prepareTextForFilter(final String incorrect, String correct){
		//extract part of speech if present
		final String incorrectFilter = (!incorrect.isEmpty()? incorrect: ".+");
		final String correctFilter = (!correct.isEmpty()? correct: ".+");

		//compose filter regexp
		return Pair.of(incorrectFilter, correctFilter);
	}

	public void save(final File acoFile) throws TransformerException{
		final Document doc = XMLParser.newXMLDocument();

		//remove `standalone="no"` from XML declaration
		doc.setXmlStandalone(true);

		//root element
		final Element root = doc.createElement(AUTO_CORRECT_ROOT_ELEMENT);
		root.setAttribute("xmlns:block-list", "http://openoffice.org/2001/block-list");
		doc.appendChild(root);

		for(final CorrectionEntry correction : dictionary){
			//correction element
			final Element elem = doc.createElement(AUTO_CORRECT_BLOCK);
			elem.setAttribute(AUTO_CORRECT_INCORRECT_FORM, correction.getEscapedIncorrectForm());
			elem.setAttribute(AUTO_CORRECT_CORRECT_FORM, correction.getEscapedCorrectForm());
			root.appendChild(elem);
		}

		XMLParser.createXML(acoFile, doc, XML_PROPERTIES);
	}

	public void clear(){
		dictionary.clear();
	}

}
