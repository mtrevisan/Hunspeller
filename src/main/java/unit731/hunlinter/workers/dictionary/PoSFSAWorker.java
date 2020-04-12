package unit731.hunlinter.workers.dictionary;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.enums.InflectionTag;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.enums.PartOfSpeechTag;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.SetHelper;
import unit731.hunlinter.services.datastructures.DynamicArray;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.builders.LexicographicalComparator;
import unit731.hunlinter.services.fsa.serializers.CFSA2Serializer;
import unit731.hunlinter.services.fsa.builders.FSABuilder;
import unit731.hunlinter.services.fsa.stemming.BufferUtils;
import unit731.hunlinter.services.fsa.stemming.DictionaryMetadata;
import unit731.hunlinter.services.fsa.stemming.SequenceEncoderInterface;
import unit731.hunlinter.services.sorters.SmoothSort;
import unit731.hunlinter.services.system.TimeWatch;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class PoSFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(PoSFSAWorker.class);

	public static final String WORKER_NAME = "Part–of–Speech FSA Extractor";

	private static final String SINGLE_POS_NOT_PRESENT = "Part-of-Speech not unique";
	private static final byte POS_FSA_TAG_SEPARATOR = (byte)'+';


	public PoSFSAWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		final Charset charset = dicParser.getCharset();
		final DictionaryMetadata metadata = readMetadata(charset, outputFile);
		final byte separator = metadata.getSeparator();
		final SequenceEncoderInterface sequenceEncoder = metadata.getSequenceEncoderType().get();


		final DynamicArray<byte[]> encodings = new DynamicArray<>(byte[].class, 40_000_000, 1.2f);
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

			final DynamicArray<byte[]> currentEncodings = encode(inflections, separator, sequenceEncoder);

			encodings.addAll(currentEncodings);

			sleepOnPause();
		};
		final FSABuilder builder = new FSABuilder();
		final Consumer<IndexDataPair<byte[]>> fsaProcessor = indexData -> builder.add(indexData.getData());

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		final Function<Void, DynamicArray<byte[]>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/4)");

			final Path dicPath = dicParser.getDicFile().toPath();
			processLines(dicPath, charset, lineProcessor);

			return encodings;
		};
		final Function<DynamicArray<byte[]>, DynamicArray<byte[]>> step2 = list -> {
			resetProcessing("Sorting (step 2/4)");

			//sort list
TimeWatch watch = TimeWatch.start();
			SmoothSort.sort(list.data, 0, list.limit, LexicographicalComparator.lexicographicalComparator(),
				percent -> {
					setProgress(percent, 100);

					sleepOnPause();
				});
watch.stop();
System.out.println(watch.toStringMillis());

			return list;
		};
		final Function<DynamicArray<byte[]>, FSA> step3 = list -> {
			resetProcessing("Creating FSA (step 3/4)");

			getWorkerData()
				.withNoHeader()
				.withSequentialProcessing();

TimeWatch watch = TimeWatch.start();
			for(int index = 0; index < list.limit; index ++){
				final byte[] encoding = list.data[index];
				fsaProcessor.accept(IndexDataPair.of(index, encoding));

				//release memory
				list.data[index] = null;

				setProgress(index, list.limit);

				sleepOnPause();
			}
watch.stop();
System.out.println(watch.toStringMillis());

			//release memory
			list.clear();

			return builder.complete();
		};
		final Function<FSA, File> step4 = fsa -> {
			resetProcessing("Compressing FSA (step 4/4)");

			final CFSA2Serializer serializer = new CFSA2Serializer();
			try(final ByteArrayOutputStream os = new ByteArrayOutputStream()){
TimeWatch watch = TimeWatch.start();
				serializer.serialize(fsa, os, (index, total) -> {
					setProgress(index, total);

					sleepOnPause();
				});
watch.stop();
System.out.println(watch.toStringMillis());

				Files.write(outputFile.toPath(), os.toByteArray());

				finalizeProcessing("Successfully processed " + workerData.getWorkerName() + ": " + outputFile.getAbsolutePath());

				return outputFile;
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}
		};
		final Function<File, Void> step5 = WorkerManager.openFolderStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4).andThen(step5));
	}

	private DictionaryMetadata readMetadata(final Charset charset, final File outputFile){
		final String filenameNoExtension = FilenameUtils.removeExtension(outputFile.getAbsolutePath());
		final File outputInfoFile = new File(filenameNoExtension + ".info");
		if(!outputInfoFile.exists()){
			final List<String> content = Arrays.asList(
				"fsa.dict.separator=" + Inflection.POS_FSA_SEPARATOR,
				"fsa.dict.encoding=" + charset.name().toLowerCase(),
				"fsa.dict.encoder=prefix");
			try{
				FileHelper.saveFile(outputInfoFile.toPath(), StringUtils.CR, charset, content);
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}
		}

		final Path metadataPath = DictionaryMetadata.getExpectedMetadataLocation(outputFile.toPath());
		try(final InputStream is = new BufferedInputStream(Files.newInputStream(metadataPath))){
			return DictionaryMetadata.read(is);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	private DynamicArray<byte[]> encode(final Inflection[] inflections, final byte separator,
			final SequenceEncoderInterface sequenceEncoder){
		ByteBuffer encoded = ByteBuffer.allocate(0);
		ByteBuffer source = ByteBuffer.allocate(0);
		ByteBuffer target = ByteBuffer.allocate(0);
		ByteBuffer tag = ByteBuffer.allocate(0);
		ByteBuffer assembled = ByteBuffer.allocate(0);

		final DynamicArray<byte[]> out = new DynamicArray<>(byte[].class, 1.2f);
		for(final Inflection inflection : inflections){
			//subdivide morphologicalFields into PART_OF_SPEECH, INFLECTIONAL_SUFFIX, INFLECTIONAL_PREFIX, and STEM
			final Map<MorphologicalTag, List<String>> bucket = extractMorphologicalTags(inflection);
			final String word = inflection.getWord();

			//extract Part-of-Speech
			final List<String> pos = bucket.get(MorphologicalTag.PART_OF_SPEECH);
			if(pos == null || pos.size() != 1)
				throw new LinterException(SINGLE_POS_NOT_PRESENT);
			//extract stem
			final List<String> stems = bucket.get(MorphologicalTag.STEM);
			final byte[][] encodedStems = new byte[stems.size()][];
			int position = 0;
			for(final String stem : stems){
				final byte[] inflectionStem = StringHelper.getRawBytes(stem);
				source = BufferUtils.clearAndEnsureCapacity(source, inflectionStem.length - 3);
				source.put(inflectionStem, 3, inflectionStem.length - 3);
				source.flip();

				final byte[] inflectedWord = StringHelper.getRawBytes(word);
				target = BufferUtils.clearAndEnsureCapacity(target, inflectedWord.length);
				target.put(inflectedWord);
				target.flip();

				//extract Inflection
				final List<String> suffixInflection = bucket.get(MorphologicalTag.INFLECTIONAL_SUFFIX);
				final List<String> prefixInflection = bucket.get(MorphologicalTag.INFLECTIONAL_PREFIX);

				tag = BufferUtils.clearAndEnsureCapacity(tag, 512);
				tag.put(StringHelper.getRawBytes(PartOfSpeechTag.createFromCodeAndValue(pos.get(0)).getTag()));
				if(suffixInflection != null)
					for(final String code : suffixInflection)
						for(final String t : InflectionTag.createFromCodeAndValue(code).getTags())
							tag.put(POS_FSA_TAG_SEPARATOR)
								.put(StringHelper.getRawBytes(t));
				if(prefixInflection != null)
					for(final String code : prefixInflection)
						for(final String t : InflectionTag.createFromCodeAndValue(code).getTags())
							tag.put(POS_FSA_TAG_SEPARATOR)
								.put(StringHelper.getRawBytes(t));
				tag.flip();


				encoded = sequenceEncoder.encode(target, source, encoded);

				assembled = BufferUtils.clearAndEnsureCapacity(assembled,
					target.capacity() + 1 + encoded.capacity() + 1 + tag.remaining());
				assembled.put(target);
				assembled.put(separator);
				assembled.put(encoded);
				if(tag.hasRemaining()){
					assembled.put(separator);
					assembled.put(tag);
				}
				assembled.flip();

				encodedStems[position ++] = BufferUtils.toArray(assembled);
			}
			out.addAll(encodedStems);
		}
		return out;
	}

	//NOTE: the only morphological tags really needed are: PART_OF_SPEECH, INFLECTIONAL_SUFFIX, INFLECTIONAL_PREFIX, and STEM
	private Map<MorphologicalTag, List<String>> extractMorphologicalTags(final Inflection inflection){
		return SetHelper.bucket(inflection.getMorphologicalFieldsAsArray(), MorphologicalTag::createFromCode,
			MorphologicalTag.class);
	}


	public static void main(final String[] args){
		final String input = "C:\\Users\\mauro\\AppData\\Local\\Temp\\hunlinter-pos.dat";
		final String[] buildOptions = {
			"--overwrite",
			"--accept-cr",
			"--exit", "false",
			"--format", "CFSA2",
			"--input", input
		};
TimeWatch watch = TimeWatch.start();
		morfologik.tools.DictCompile.main(buildOptions);
watch.stop();
System.out.println(watch.toStringMillis());
	}

}
