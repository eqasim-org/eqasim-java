package org.eqasim.ile_de_france.standalone_mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import javax.inject.Named;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class StandaloneModeChoiceModule extends AbstractModule {
    private final StandaloneModeChoiceConfigGroup configGroup;
    private final int numberOfThreads;
    private final long randomSeed;

    @Inject
    public StandaloneModeChoiceModule(Config config) {
        try {
            this.configGroup = (StandaloneModeChoiceConfigGroup) config.getModules().get(StandaloneModeChoiceConfigGroup.GROUP_NAME);
        } catch (NullPointerException e) {
            throw new IllegalStateException(String.format("%s module is required in the config", StandaloneModeChoiceConfigGroup.GROUP_NAME), e);
        } catch (ClassCastException e) {
            throw new IllegalStateException(String.format("%s module is present in the config but its corresponding configGroup wasn't set while loading it", StandaloneModeChoiceConfigGroup.GROUP_NAME), e);
        }
        try {
            DiscreteModeChoiceConfigGroup discreteModeChoiceConfigGroup = (DiscreteModeChoiceConfigGroup) config.getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
            if (!discreteModeChoiceConfigGroup.getFallbackBehaviour().equals(DiscreteModeChoiceModel.FallbackBehaviour.EXCEPTION) && this.configGroup.isRemovePersonsWithNoValidAlternative()) {
                throw new IllegalStateException(String.format("The %s module relies on the exceptions thrown by the %s module to filter out persons with no alternatives. " +
                        "The %s attribute of the latter needs to be set to %s when the %s attribute of the former is set to true", StandaloneModeChoiceConfigGroup.GROUP_NAME, DiscreteModeChoiceConfigGroup.GROUP_NAME, DiscreteModeChoiceConfigGroup.FALLBACK_BEHAVIOUR, DiscreteModeChoiceModel.FallbackBehaviour.EXCEPTION, StandaloneModeChoiceConfigGroup.REMOVE_PERSON_WITH_NO_VALID_ALTERNATIVES));
            }
        } catch (NullPointerException e) {
            throw new IllegalStateException(String.format("%s module is required in the config", DiscreteModeChoiceConfigGroup.GROUP_NAME), e);
        } catch (ClassCastException e) {
            throw new IllegalStateException(String.format("%s module is present in the config but its corresponding configGroup wasn't set while loading it", DiscreteModeChoiceConfigGroup.GROUP_NAME), e);
        }
        this.numberOfThreads = config.global().getNumberOfThreads();
        this.randomSeed = config.global().getRandomSeed();
    }

    @Override
    public void install() {

    }

    @Provides
    public StandaloneModeChoicePerformer provideBadPlansFilter(Provider<DiscreteModeChoiceModel> discreteModeChoiceModelProvider, Population population, @Named("StandaloneModeChoice") OutputDirectoryHierarchy outputDirectoryHierarchy, Scenario scenario) {
        return new StandaloneModeChoicePerformer(discreteModeChoiceModelProvider, configGroup, population, this.numberOfThreads, this.randomSeed, outputDirectoryHierarchy, scenario);
    }

    @Provides
    @Named("StandaloneModeChoice")
    @Singleton
    public OutputDirectoryHierarchy provideOutputDirectoryHierarchy() {
        OutputDirectoryHierarchy outputDirectoryHierarchy = new OutputDirectoryHierarchy(this.configGroup.getOutputDirectory(), null, OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles, false, ControlerConfigGroup.CompressionType.gzip);
        File outputDir = new File(outputDirectoryHierarchy.getOutputPath());
        if (outputDir.exists()) {
            if (outputDir.isFile()) {
                throw new RuntimeException("Cannot create output directory. "
                        + outputDirectoryHierarchy.getOutputPath() + " is a file and cannot be replaced by a directory.");
            }
            if (Objects.requireNonNull(outputDir.list()).length > 0) {
                IOUtils.deleteDirectoryRecursively(outputDir.toPath());
            }
        }
        try {
            OutputDirectoryLogging.initLoggingWithOutputDirectory(this.configGroup.getOutputDirectory());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputDirectoryHierarchy;
    }
}
