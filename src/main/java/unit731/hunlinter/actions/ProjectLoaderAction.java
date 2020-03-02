package unit731.hunlinter.actions;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import unit731.hunlinter.LanguageChooserDialog;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.downloader.DownloaderHelper;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.exceptions.LanguageNotChosenException;
import unit731.hunlinter.workers.exceptions.ProjectNotFoundException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class ProjectLoaderAction extends AbstractAction{

	private static final Pattern LANGUAGE_SAMPLE_EXTRACTOR = PatternHelper.pattern("(?:TRY |FX [^ ]+ )([^\r\n\\d]+)[\r\n]+");


	private final Path projectPath;
	private final Packager packager;
	private final WorkerManager workerManager;
	private final Runnable completed;
	private final Consumer<Exception> cancelled;
	private final JFrame parentFrame;
	private final PropertyChangeListener propertyChangeListener;


	public ProjectLoaderAction(final Path projectPath, final Packager packager, final WorkerManager workerManager, final Runnable completed,
			final Consumer<Exception> cancelled, final JFrame parentFrame, final PropertyChangeListener propertyChangeListener){
		super("project.load");

		Objects.requireNonNull(packager);
		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(completed);
		Objects.requireNonNull(cancelled);
		Objects.requireNonNull(parentFrame);
		Objects.requireNonNull(propertyChangeListener);

		this.projectPath = projectPath;
		this.packager = packager;
		this.workerManager = workerManager;
		this.completed = completed;
		this.cancelled = cancelled;
		this.parentFrame = parentFrame;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		workerManager.createProjectLoaderWorker(
			worker -> {
				try{
					packager.reload(projectPath != null? projectPath: packager.getProjectPath());

					final List<String> availableLanguages = packager.getAvailableLanguages();
					final AtomicReference<String> language = new AtomicReference<>(availableLanguages.get(0));
					if(availableLanguages.size() > 1){
						//choose between available languages
						final Consumer<String> onSelection = language::set;
						final LanguageChooserDialog dialog = new LanguageChooserDialog(availableLanguages, onSelection, parentFrame);
						GUIUtils.addCancelByEscapeKey(dialog);
						dialog.setLocationRelativeTo(parentFrame);
						dialog.setVisible(true);

						if(!dialog.languageChosen())
							throw new LanguageNotChosenException("Language not chosen loading " + projectPath);
					}
					//load appropriate files based on current language
					packager.extractConfigurationFolders(language.get());

					parentFrame.setTitle(DownloaderHelper.getApplicationProperties().get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID) + " : "
						+ packager.getLanguage());

					final Font temporaryFont = temporarilyChooseAFont();
//FIXME
//					parsingResultTextArea.setFont(temporaryFont);

//FIXME
//					filOpenProjectMenuItem.setEnabled(false);

					worker.addPropertyChangeListener(propertyChangeListener);
					worker.execute();
				}
				catch(final IOException | SAXException | ProjectNotFoundException | LanguageNotChosenException e){
					throw new RuntimeException(e);
				}
			},
			completed, cancelled);
	}

	/** Chooses one font (in case of reading errors) */
	private Font temporarilyChooseAFont() throws IOException{
		final Path affixPath = packager.getAffixFile().toPath();
		final String content = new String(Files.readAllBytes(affixPath));
		final String[] extractions = PatternHelper.extract(content, LANGUAGE_SAMPLE_EXTRACTOR, 10);
		final String sample = String.join(StringUtils.EMPTY, String.join(StringUtils.EMPTY, extractions).chars()
			.mapToObj(Character::toString).collect(Collectors.toSet()));
		return GUIUtils.chooseBestFont(sample);
	}

}