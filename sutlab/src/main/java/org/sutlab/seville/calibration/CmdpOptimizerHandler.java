package org.sutlab.seville.calibration;

import org.eqasim.core.components.calibration.CalibrationConfigGroup;
import org.eqasim.core.components.calibration.Optimizer;
import org.eqasim.core.components.calibration.optimizer.StandardOptimizerHandler;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.sutlab.seville.mode_choice.parameters.SevilleModeParameters;

import java.io.File;

public class CmdpOptimizerHandler extends StandardOptimizerHandler {
    // This class allows access to ModeParameters, which are not accessible in StandardOptimizerHandler where only ModeParameters are available.
    // Therefore, overlapping parameters from the child object (ModeParameters) cannot be accessed in the parent class.
    private final SevilleModeParameters modeParameters;

    public CmdpOptimizerHandler(CalibrationConfigGroup calibrationConfig, OutputDirectoryHierarchy outputDirectoryHierarchy,
                                EqasimConfigGroup eqasimConfigGroup, SevilleModeParameters parameters, Optimizer optimizer) {
        super(calibrationConfig, outputDirectoryHierarchy, eqasimConfigGroup, parameters, optimizer);
        this.modeParameters = parameters;
    }

    @Override
    protected void applyParameters(String parameterFilePath, boolean isOptimized) {
        File file = new File(parameterFilePath);
        if (file.exists() && file.isFile()) {
            eqasimConfigGroup.setModeParametersPath(parameterFilePath);
            ParameterDefinition.applyFile(file, modeParameters);

            String label = isOptimized ? "New" : "Fallback";
            logger.info(String.format("%s car alpha  : %f", label, modeParameters.car.alpha_u));
            logger.info(String.format("%s bike alpha : %f", label, modeParameters.bike.alpha_u));
            logger.info(String.format("%s walk alpha : %f", label, modeParameters.walk.alpha_u));
            logger.info(String.format("%s pt alpha   : %f", label, modeParameters.pt.alpha_u));
            logger.info(String.format("%s car_passenger alpha   : %f", label, modeParameters.carPassenger.alpha_u));
        } else {
            logger.warn("Parameters file does not exist at: " + parameterFilePath);
        }
    }
}
