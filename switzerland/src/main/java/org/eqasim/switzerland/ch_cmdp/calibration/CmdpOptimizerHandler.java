package org.eqasim.switzerland.ch_cmdp.calibration;

import org.eqasim.core.components.calibration.CalibrationConfigGroup;
import org.eqasim.core.components.calibration.Optimizer;
import org.eqasim.core.components.calibration.optimizer.StandardOptimizerHandler;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.io.File;

public class CmdpOptimizerHandler extends StandardOptimizerHandler {
    // This class allows access to SwissCmdpModeParameters, which are not accessible in StandardOptimizerHandler where only ModeParameters are available.
    // Therefore, overlapping parameters from the child object (SwissCmdpModeParameters) cannot be accessed in the parent class.
    private final SwissCmdpModeParameters swissParameters;

    public CmdpOptimizerHandler(CalibrationConfigGroup calibrationConfig, OutputDirectoryHierarchy outputDirectoryHierarchy,
                                EqasimConfigGroup eqasimConfigGroup, SwissCmdpModeParameters parameters, Optimizer optimizer) {
        super(calibrationConfig, outputDirectoryHierarchy, eqasimConfigGroup, parameters, optimizer);
        this.swissParameters = parameters;
    }

    @Override
    protected void applyParameters(String parameterFilePath, boolean isOptimized) {
        File file = new File(parameterFilePath);
        if (file.exists() && file.isFile()) {
            eqasimConfigGroup.setModeParametersPath(parameterFilePath);
            ParameterDefinition.applyFile(file, swissParameters);

            String label = isOptimized ? "New" : "Fallback";
            logger.info(String.format("%s car alpha  : %f", label, swissParameters.car.alpha_u));
            logger.info(String.format("%s bike alpha : %f", label, swissParameters.bike.alpha_u));
            logger.info(String.format("%s walk alpha : %f", label, swissParameters.walk.alpha_u));
            logger.info(String.format("%s pt alpha   : %f", label, swissParameters.pt.alpha_u));
        } else {
            logger.warn("Parameters file does not exist at: " + parameterFilePath);
        }
    }
}
