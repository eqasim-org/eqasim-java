package org.eqasim.core.components.calibration.calibration;


import com.google.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.calibration.CalibrationConfigGroup;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;


import java.io.File;

public class CalibrationHandler implements IterationStartsListener {
    private final Logger logger = LogManager.getLogger(CalibrationHandler.class);
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final CalibrationConfigGroup calibrationConfig;
    private final RunParametersCalibration optimizer;
    private final ModeParameters parameters;
    private final EqasimConfigGroup eqasimConfigGroup;

    @Inject
    public CalibrationHandler(CalibrationConfigGroup calibrationConfig, OutputDirectoryHierarchy outputDirectoryHierarchy,
                              EqasimConfigGroup eqasimConfigGroup, ModeParameters parameters, RunParametersCalibration optimizer){
        this.calibrationConfig = calibrationConfig;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.optimizer = optimizer;
        this.parameters = parameters;
        this.eqasimConfigGroup = eqasimConfigGroup;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (!shouldRunCalibration(event)) {
            return;
        }

        int iteration = event.getIteration();
        String newParametersFilePath = getNewParametersFilePath(iteration);
        String lastParametersFilePath = getLastParametersFilePath(iteration);
        String variablesIterationPath = getVariablesIterationPath(iteration);

        boolean optimizationSucceeded = runOptimizer(iteration, newParametersFilePath, lastParametersFilePath, variablesIterationPath);
        String parametersFileToUse = optimizationSucceeded ? newParametersFilePath : lastParametersFilePath;

        applyParameters(parametersFileToUse, optimizationSucceeded);
    }

    private boolean shouldRunCalibration(IterationStartsEvent event) {
        boolean runCalibration = calibrationConfig.getRunCalibration();
        int iteration = event.getIteration();
        int startIteration = calibrationConfig.getStartIteration();
        return runCalibration && iteration >= startIteration;
    }

    private String getNewParametersFilePath(int iteration) {
        return outputDirectoryHierarchy.getIterationFilename(iteration, "optimized_parameters.yml");
    }

    private String getLastParametersFilePath(int iteration) {
        String lastPath = outputDirectoryHierarchy.getIterationFilename(iteration - 1, "optimized_parameters.yml");
        File lastFile = new File(lastPath);
        int startIteration = calibrationConfig.getStartIteration();

        if (iteration == startIteration || !lastFile.exists()) {
            logger.info("Using initial mode parameters file as initial parameters FilePath.");
            return eqasimConfigGroup.getModeParametersPath();
        }

        return lastPath;
    }

    private String getVariablesIterationPath(int iteration) {
        return outputDirectoryHierarchy.getIterationPath(iteration - 1);
    }

    private boolean runOptimizer(int iteration, String newPath, String lastPath, String variablesPath) {
        try {
            optimizer.run(iteration, newPath, lastPath, variablesPath);
            return true;
        } catch (Exception e) {
            logger.warn("Optimizer failed to run for iteration " + iteration);
            return false;
        }
    }

    private void applyParameters(String parameterFilePath, boolean isOptimized) {
        File file = new File(parameterFilePath);
        if (file.exists() && file.isFile()) {
            eqasimConfigGroup.setModeParametersPath(parameterFilePath);
            ParameterDefinition.applyFile(file, parameters);

            String label = isOptimized ? "New" : "Fallback";
            logger.info(String.format("%s car alpha  : %f", label, parameters.car.alpha_u));
            logger.info(String.format("%s bike alpha : %f", label, parameters.bike.alpha_u));
            logger.info(String.format("%s walk alpha : %f", label, parameters.walk.alpha_u));
            logger.info(String.format("%s pt alpha   : %f", label, parameters.pt.alpha_u));
        } else {
            logger.warn("Parameters file does not exist at: " + parameterFilePath);
        }
    }

}
