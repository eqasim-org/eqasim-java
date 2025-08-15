package org.eqasim.core.components.calibration.calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.calibration.CalibrationConfigGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import java.util.*;

public class RunParametersCalibration {
    private final Logger logger = LogManager.getLogger(RunParametersCalibration.class.getName());

    private final CalibrationConfigGroup calibrationConfig;
    private final int startIteration;
    private final String optimizerPath;

    public RunParametersCalibration(CalibrationConfigGroup calibrationConfig){
        this.calibrationConfig = calibrationConfig;
        this.startIteration = calibrationConfig.getStartIteration();
        this.optimizerPath = calibrationConfig.getOptimizerPath();
//        if (calibrationConfig.isActivated()){
//            checkOptimizerPath();
//        }
    }

    private void checkOptimizerPath(){
        Path path = Paths.get(this.optimizerPath);
        if (!Files.exists(path)) {
            throw new RuntimeException("Optimizer path does not exist: " + this.optimizerPath);
        }
    }

    /**
     * Runs the calibration process if current iteration > startIteration
     * and runCalibration flag is set to true.
     */
    public void run(int iteration, String newParametersFilePath, String lastParametersFilePath, String variablesIterationPath) {
        if (iteration >= startIteration && calibrationConfig.getRunCalibration()) {
            runPythonScript(iteration, newParametersFilePath, lastParametersFilePath, variablesIterationPath);
        }
    }

    /**
     * Executes the external Python script with dynamically built arguments.
     */
    private void runPythonScript(int iteration, String newParametersFilePath, String lastParametersFilePath, String variablesIterationPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add("python3");
            command.add(optimizerPath);

            // Build command-line arguments based on config
            Map<String, String> args = new HashMap<>();

            args.put("--selector", calibrationConfig.getSelector());
            args.put("--input-parameters", lastParametersFilePath);
            args.put("--output-parameters", newParametersFilePath);
            args.put("--variables-path", variablesIterationPath);
            args.put("--iteration", String.valueOf(iteration));
            args.put("--bounds", calibrationConfig.getBounds());
            args.put("--metric", calibrationConfig.getMetric());
            args.put("--optimizer", calibrationConfig.getOptimizer());
            args.put("--max-evals", String.valueOf(calibrationConfig.getMaxEval()));
            args.put("--population-sample", String.valueOf(calibrationConfig.getPopulationSample()));
            args.put("--momentum", calibrationConfig.getMomentum());
            args.put("--beta-momentum", String.valueOf(calibrationConfig.getBetaMomentum()));
            args.put("--calibrate-global-modeshare", String.valueOf(calibrationConfig.getCalibrateGlobalModeshare()));
            args.put("--calibrate-modeshare-distribution", String.valueOf(calibrationConfig.getCalibrateModeshareDistribution()));
            args.put("--eqasim-cache-path", calibrationConfig.getEqasimCachePath());

            // Append to command list
            args.forEach((key, value) -> {
                command.add(key);
                command.add(value);
            });

            // Execute the Python script
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            logger.info("Running calibration script...");
            logger.info("Command: " + String.join(" ", command));

            Process process = processBuilder.start();

            // Read output
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("[CALIBRATION] " + line);
            }

            int exitCode = process.waitFor();
            logger.info("Calibration script exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute calibration script", e);
        }
    }
}