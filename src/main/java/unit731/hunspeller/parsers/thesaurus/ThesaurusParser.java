package unit731.hunspeller.parsers.thesaurus;

import unit731.hunspeller.parsers.thesaurus.dtos.ThesaurusEntry;
import unit731.hunspeller.parsers.thesaurus.dtos.MeaningEntry;
import unit731.hunspeller.parsers.thesaurus.dtos.DuplicationResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.interfaces.Undoable;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.memento.CaretakerInterface;
import unit731.hunspeller.services.memento.OriginatorInterface;


@Slf4j
@Getter
public class ThesaurusParser implements OriginatorInterface<ThesaurusParser.Memento>{

	private final ReentrantLock LOCK_SAVING = new ReentrantLock();

	//NOTE: All members are private and accessible only by Originator
	@AllArgsConstructor
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	protected static class Memento{

		@JsonProperty
		private ThesaurusDictionary dictionary;

	}

	private final ThesaurusDictionary dictionary = new ThesaurusDictionary();

	private final Undoable undoable;
	private final CaretakerInterface<Memento> undoCaretaker = new ThesaurusCaretaker();
	private final CaretakerInterface<Memento> redoCaretaker = new ThesaurusCaretaker();


	public ThesaurusParser(Undoable undoable){
		this.undoable = undoable;
	}

	/**
	 * Parse the rows out from a .aid file.
	 *
	 * @param theFile	The content of the thesaurus file
	 * @throws IOException	If an I/O error occurse
	 */
	public void parse(File theFile) throws IOException{
		acquireLock();

		Charset charset = FileService.determineCharset(theFile.toPath());
		try(BufferedReader br = Files.newBufferedReader(theFile.toPath(), charset)){
			String line = br.readLine();
			//ignore any BOM marker on first line
			if(line.startsWith(FileService.BOM_MARKER))
				line = line.substring(1);
			//line should be a charset
			try{ Charsets.toCharset(line); }
			catch(UnsupportedCharsetException e){
				throw new IllegalArgumentException("Thesaurus file malformed, the first line is not a charset");
			}

			while(Objects.nonNull(line = br.readLine()))
				if(!line.isEmpty())
					dictionary.add(new ThesaurusEntry(line, br));
		}
		finally{
			dictionary.resetModified();

			releaseLock();
		}
//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(theParser.synonyms));
//6 035 792 B
	}

	public void acquireLock(){
		LOCK_SAVING.lock();
	}

	public void releaseLock(){
		LOCK_SAVING.unlock();
	}

	public int getSynonymsCounter(){
		acquireLock();

		try{
			return dictionary.size();
		}
		finally{
			releaseLock();
		}
	}

	public boolean isDictionaryModified(){
		acquireLock();

		try{
			return dictionary.isModified();
		}
		finally{
			releaseLock();
		}
	}

	public List<ThesaurusEntry> getSynonymsDictionary(){
		acquireLock();

		try{
			return dictionary.getSynonyms();
		}
		finally{
			releaseLock();
		}
	}

	/**
	 * @param synonymAndMeanings			The line representing all the synonyms of a word along with their part of speech
	 * @param duplicatesDiscriminator	Function called to ask the user what to do is duplicates are found (return <code>true</code> to force
	 *												insertion)
	 * @return The duplication result
	 */
	public DuplicationResult insertMeanings(String synonymAndMeanings, Supplier<Boolean> duplicatesDiscriminator){
		acquireLock();

		try{
			String[] partOfSpeechAndMeanings = StringUtils.split(synonymAndMeanings, ThesaurusEntry.POS_MEANS, 2);
			if(partOfSpeechAndMeanings.length != 2)
				throw new IllegalArgumentException("Wrong format: " + synonymAndMeanings);

			String partOfSpeech = StringUtils.strip(partOfSpeechAndMeanings[0]);
			StringBuilder sb = new StringBuilder();
			if(!partOfSpeech.startsWith("("))
				sb.append('(');
			sb.append(partOfSpeech);
			if(!partOfSpeech.endsWith("("))
				sb.append(')');
			partOfSpeech = sb.toString();

			String[] means = StringUtils.split(partOfSpeechAndMeanings[1], ThesaurusEntry.MEANS);
			List<String> meanings = Arrays.stream(means)
				.filter(StringUtils::isNotBlank)
				.map(String::trim)
				.distinct()
				.collect(Collectors.toList());
			if(meanings.size() < 1)
				throw new IllegalArgumentException("Not enough meanings are supplied (at least one should be present): " + synonymAndMeanings);

			DuplicationResult duplicationResult = extractDuplicates(meanings, partOfSpeech, duplicatesDiscriminator);

			if(duplicationResult.isForcedInsertion() || duplicationResult.getDuplicates().isEmpty()){
				try{
					undoCaretaker.pushMemento(createMemento());

					if(Objects.nonNull(undoable))
						undoable.onUndoChange(true);
				}
				catch(IOException ex){
					log.warn("Error while storing a memento", ex);
				}

				dictionary.add(partOfSpeech, meanings);
			}

			return duplicationResult;
		}
		finally{
			releaseLock();
		}
	}

	/** Find if there is a duplicate with the same part of speech */
	private DuplicationResult extractDuplicates(List<String> means, String partOfSpeech, Supplier<Boolean> duplicatesDiscriminator)
			throws IllegalArgumentException{
		acquireLock();

		try{
			boolean forcedInsertion = false;
			List<ThesaurusEntry> duplicates = new ArrayList<>();
			try{
				List<ThesaurusEntry> synonyms = dictionary.getSynonyms();
				for(String meaning : means)
					for(ThesaurusEntry synonym : synonyms)
						if(synonym.getSynonym().equals(meaning)){
							List<MeaningEntry> meanings = synonym.getMeanings();
							long countSamePartOfSpeech = meanings.stream()
								.map(MeaningEntry::getPartOfSpeech)
								.filter(pos -> pos.equals(partOfSpeech))
								.map(m -> 1)
								.reduce(0, (accumulator, m) -> accumulator + 1);
							if(countSamePartOfSpeech > 0l)
								throw new IllegalArgumentException("Duplicate detected for " + meaning);
						}
			}
			catch(IllegalArgumentException e){
				if(!duplicatesDiscriminator.get())
					throw e;

				duplicates.clear();
				forcedInsertion = true;
			}
			return new DuplicationResult(duplicates, forcedInsertion);
		}
		finally{
			releaseLock();
		}
	}

	public void setMeanings(int index, List<MeaningEntry> meanings, String text){
		acquireLock();

		try{
			undoCaretaker.pushMemento(createMemento());

			dictionary.setMeanings(index, meanings, text);

			if(Objects.nonNull(undoable))
				undoable.onUndoChange(true);
		}
		catch(IOException e){
			try{
				undoCaretaker.popMemento();
			}
			catch(IOException ioe){
				log.warn("Error while removing a memento", ioe);
			}

			log.warn("Error while storing a memento", e);
		}
		catch(Exception e){
			try{
				undoCaretaker.popMemento();
			}
			catch(IOException ioe){
				log.warn("Error while removing a memento", ioe);
			}

			log.warn("Error while modifying the meanings", e);
		}
		finally{
			releaseLock();
		}
	}

	public void deleteMeanings(int[] selectedRowIDs){
		acquireLock();

		try{
			int count = selectedRowIDs.length;
			if(count > 0){
				try{
					undoCaretaker.pushMemento(createMemento());

					if(Objects.nonNull(undoable))
						undoable.onUndoChange(true);
				}
				catch(IOException ex){
					log.warn("Error while storing a memento", ex);
				}

				for(int i = 0; i < count; i ++)
					dictionary.remove(selectedRowIDs[i] - i);
			}
		}
		finally{
			releaseLock();
		}
	}

	public List<String> extractDuplicates(){
		acquireLock();

		try{
			return dictionary.extractDuplicates();
		}
		finally{
			releaseLock();
		}
	}

	public void save(File theIndexFile, File theDataFile) throws IOException{
		acquireLock();

		try{
			//sort the synonyms
			dictionary.sort();

			//save index and data files
			Charset charset = StandardCharsets.UTF_8;
			try(
					BufferedWriter indexWriter = Files.newBufferedWriter(theIndexFile.toPath(), charset);
					BufferedWriter dataWriter = Files.newBufferedWriter(theDataFile.toPath(), charset);
					){
				//save charset
				indexWriter.write(charset.name());
				indexWriter.write(StringUtils.LF);
				//save counter
				indexWriter.write(Integer.toString(dictionary.size()));
				indexWriter.write(StringUtils.LF);
				//save charset
				dataWriter.write(charset.name());
				dataWriter.write(StringUtils.LF);
				//save data
				int idx = charset.name().length() + 1;
				List<ThesaurusEntry> synonyms = dictionary.getSynonyms();
				for(ThesaurusEntry synonym : synonyms){
					String syn = synonym.getSynonym();
					indexWriter.write(syn);
					indexWriter.write(ThesaurusEntry.PIPE);
					indexWriter.write(Integer.toString(idx));
					indexWriter.write(StringUtils.LF);

					int meaningsCount = synonym.getMeanings().size();
					dataWriter.write(syn);
					dataWriter.write(ThesaurusEntry.PIPE);
					dataWriter.write(Integer.toString(meaningsCount));
					dataWriter.write(StringUtils.LF);
					List<MeaningEntry> meanings = synonym.getMeanings();
					int meaningsLength = 1;
					for(MeaningEntry meaning : meanings){
						dataWriter.write(meaning.toString());
						dataWriter.write(StringUtils.LF);

						meaningsLength += meaning.toString().getBytes(charset).length + 1;
					}

					idx += syn.getBytes(charset).length + meaningsLength + 2;
				}

				dictionary.resetModified();
			}
		}
		finally{
			releaseLock();
		}
	}

	public void clear(){
		acquireLock();

		try{
			dictionary.clear();
		}
		finally{
			releaseLock();
		}
	}

	public boolean canUndo(){
		return undoCaretaker.canUndo();
	}

	public boolean canRedo(){
		return redoCaretaker.canUndo();
	}

	public boolean restorePreviousSnapshot() throws IOException{
		boolean restored = false;
		if(canUndo()){
			redoCaretaker.pushMemento(createMemento());

			Memento memento = undoCaretaker.popMemento();
			if(Objects.nonNull(undoable)){
				undoable.onUndoChange(canUndo());
				undoable.onRedoChange(true);
			}

			restoreMemento(memento);

			restored = true;
		}
		return restored;
	}

	public boolean restoreNextSnapshot() throws IOException{
		boolean restored = false;
		if(canRedo()){
			undoCaretaker.pushMemento(createMemento());

			Memento memento = redoCaretaker.popMemento();
			if(Objects.nonNull(undoable)){
				undoable.onUndoChange(true);
				undoable.onRedoChange(canRedo());
			}

			restoreMemento(memento);

			restored = true;
		}
		return restored;
	}

	@Override
	public Memento createMemento(){
		return new Memento(dictionary);
	}

	@Override
	public void restoreMemento(Memento memento){
		dictionary.restore(memento.dictionary);
	}

}
