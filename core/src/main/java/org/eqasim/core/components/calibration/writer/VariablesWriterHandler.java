package org.eqasim.core.components.calibration.writer;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.CalibrationConfigGroup;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.io.IOException;


public class VariablesWriterHandler implements IterationStartsListener, IterationEndsListener {
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final DiscreteModeChoiceConfigGroup discreteModeChoiceConfigGroup;
    private final EqasimConfigGroup eqasimConfigGroup;
    private final CalibrationConfigGroup calibrationConfig;

    @Inject
    public VariablesWriterHandler(DiscreteModeChoiceConfigGroup discreteModeChoiceConfigGroup, OutputDirectoryHierarchy outputDirectoryHierarchy,
                                  EqasimConfigGroup eqasimConfigGroup, CalibrationConfigGroup calibrationConfig) {
        this.discreteModeChoiceConfigGroup = discreteModeChoiceConfigGroup;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.eqasimConfigGroup = eqasimConfigGroup;
        this.calibrationConfig = calibrationConfig;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        boolean writeDetailedUtilities = discreteModeChoiceConfigGroup.getMultinomialLogitSelectorConfig().getWriteDetailedUtilities();
        boolean calibrateParameters = calibrationConfig.getRunCalibration();
        if (writeDetailedUtilities||calibrateParameters) {
            // Write the detailed utilities if it is requested, or if the calibration is to be run.
            String filePath = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "choice_variables.csv");
            VariablesWriter.init(filePath);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        try {
            VariablesWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
