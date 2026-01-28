package org.eqasim.core.components.calibration.optimizer;


import com.google.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.calibration.CalibrationConfigGroup;
import org.eqasim.core.components.calibration.Optimizer;
import org.eqasim.core.components.calibration.OptimizerHandler;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.matsim.core.controler.listener.ShutdownListener;


import java.io.File;

import static org.apache.commons.io.FileUtils.copyFile;

public class StandardOptimizerHandler implements OptimizerHandler, ShutdownListener {
    protected final Logger logger = LogManager.getLogger(OptimizerHandler.class);
    protected final OutputDirectoryHierarchy outputDirectoryHierarchy;
    protected final CalibrationConfigGroup calibrationConfig;
    protected final Optimizer optimizer;
    protected final ModeParameters parameters;
    protected final EqasimConfigGroup eqasimConfigGroup;

    @Inject
    public StandardOptimizerHandler(CalibrationConfigGroup calibrationConfig, OutputDirectoryHierarchy outputDirectoryHierarchy,
                                    EqasimConfigGroup eqasimConfigGroup, ModeParameters parameters, Optimizer optimizer){
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
        String lastParametersFilePath = getLastParametersFilePath();
        String variablesIterationPath = getVariablesIterationPath(iteration);

        boolean optimizationSucceeded = runOptimizer(iteration, newParametersFilePath, lastParametersFilePath, variablesIterationPath);
        String parametersFileToUse = optimizationSucceeded ? newParametersFilePath : lastParametersFilePath;

        applyParameters(parametersFileToUse, optimizationSucceeded);
    }

    protected boolean shouldRunCalibration(IterationStartsEvent event) {
        boolean runCalibration = calibrationConfig.getRunCalibration();
        int iteration = event.getIteration();
        int startIteration = calibrationConfig.getStartIteration();
        return runCalibration && iteration >= startIteration;
    }

    protected String getNewParametersFilePath(int iteration) {
        return outputDirectoryHierarchy.getIterationFilename(iteration, "optimal_parameters.yml");
    }

    protected String getLastParametersFilePath() {
        return eqasimConfigGroup.getModeParametersPath();
    }

    protected String getVariablesIterationPath(int iteration) {
        String paths = outputDirectoryHierarchy.getIterationPath(iteration - 1);
        // this would results in a much more stable calibration
        if (iteration > calibrationConfig.getStartIteration()) {
            paths += "," + outputDirectoryHierarchy.getIterationPath(iteration - 2);
            if (iteration > calibrationConfig.getStartIteration() + 1) {
                paths += "," + outputDirectoryHierarchy.getIterationPath(iteration - 3);
                if (iteration > calibrationConfig.getStartIteration() + 2) {
                    paths += "," + outputDirectoryHierarchy.getIterationPath(iteration - 4);
                }
            }
        }
        logger.info("Variables paths for iteration {}: {}", iteration, paths);
        return paths;
    }

    protected boolean runOptimizer(int iteration, String newPath, String lastPath, String variablesPath) {
        try {
            optimizer.run(iteration, newPath, lastPath, variablesPath);
            return true;
        } catch (Exception e) {
            logger.warn("Optimizer failed to run for iteration " + iteration);
            return false;
        }
    }

    protected void applyParameters(String parameterFilePath, boolean isOptimized) {
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

    @Override
    public void notifyShutdown(ShutdownEvent shutdownEvent) {
        String finalParametersPath = getLastParametersFilePath();
        // copy the file to the output directory
        String outputPath = outputDirectoryHierarchy.getOutputFilename("final_mode_parameters.yml");
        try {
            File source = new File(finalParametersPath);
            File target = new File(outputPath);
            copyFile(source, target);
            logger.info("Final parameters copied to: " + outputPath);
        } catch (Exception e) {
            logger.warn("Could not copy final parameters to output directory: " + outputPath);
        }
    }
}
