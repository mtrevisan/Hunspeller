package unit731.hunspeller.parsers.dictionary.workers.core;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.ExceptionHelper;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class WorkerDictionaryRead extends WorkerBase<String, Integer>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerDictionaryRead.class);


	protected boolean preventExceptionRelaunch;

	private final File dicFile;

	public WorkerDictionaryRead(String workerName, File dicFile, Charset charset, BiConsumer<String, Integer> lineReader,
			Runnable completed, Runnable cancelled, ReadWriteLockable lockable){
		Objects.requireNonNull(workerName);
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(charset);
		Objects.requireNonNull(lineReader);
		Objects.requireNonNull(lockable);

		this.workerName = workerName;
		this.dicFile = dicFile;
		this.charset = charset;
		this.lineReader = lineReader;
		this.completed = completed;
		this.cancelled = cancelled;
		this.lockable = lockable;
	}

	public boolean isPreventExceptionRelaunch(){
		return preventExceptionRelaunch;
	}

	@Override
	protected Void doInBackground() throws IOException{
		LOGGER.info(Backbone.MARKER_APPLICATION, "Opening Dictionary file (pass 1/2)");

		watch.reset();

		lockable.acquireReadLock();

		setProgress(0);
		long totalSize = dicFile.length();
		List<String> lines = new ArrayList<>();
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), charset))){
			String line = br.readLine();
			if(line == null)
				throw new IllegalArgumentException("Dictionary file empty");

			long readSoFar = line.getBytes(charset).length + 2;

			//ignore any BOM marker on first line
			if(br.getLineNumber() == 1)
				line = FileHelper.clearBOMMarker(line);
			if(!NumberUtils.isCreatable(line))
				throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

			while((line = br.readLine()) != null){
				readSoFar += line.getBytes(charset).length + 2;

				line = DictionaryParser.cleanLine(line);
				if(!line.isEmpty())
					lines.add(line);

				setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
			}
		}
		catch(Exception e){
			if(e instanceof ClosedChannelException)
				LOGGER.warn("Thread interrupted");
			else{
				String message = ExceptionHelper.getMessage(e);
				LOGGER.error("{}: {}", e.getClass().getSimpleName(), message);
			}

			LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing Dictionary file");

			cancel(true);
		}
		finally{
			lockable.releaseReadLock();
		}

		if(!isCancelled()){
			try{
				LOGGER.info(Backbone.MARKER_APPLICATION, workerName + " (pass 2/2)");
				setProgress(0);

				int totalLines = lines.size();
				int processingIndex = 0;
				for(String line : lines){
					try{
						processingIndex ++;

						lineReader.accept(line, processingIndex);

						setProgress((int)Math.ceil((processingIndex * 100.) / totalLines));
					}
					catch(Exception e){
						LOGGER.info(Backbone.MARKER_APPLICATION, "{} on line {}: {}", e.getMessage(), processingIndex, line);

						if(!preventExceptionRelaunch)
							throw e;
					}
				}


				watch.stop();

				setProgress(100);

				LOGGER.info(Backbone.MARKER_APPLICATION, "Successfully processed dictionary file (it takes {})", watch.toStringMinuteSeconds());
			}
			catch(Exception e){
				if(e instanceof ClosedChannelException)
					LOGGER.warn("Thread interrupted");
				else{
					String message = ExceptionHelper.getMessage(e);
					LOGGER.error("{}: {}", e.getClass().getSimpleName(), message);
				}

				LOGGER.info(Backbone.MARKER_APPLICATION, "Stopped processing Dictionary file");

				cancel(true);
			}
		}

		return null;
	}

	@Override
	protected void done(){
		if(!isCancelled() && completed != null)
			completed.run();
		else if(isCancelled() && cancelled != null)
			cancelled.run();
	}

}
