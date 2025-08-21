package org.eqasim.core.components.calibration.optimizer;

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

public class Optimizer {
    private final Logger logger = LogManager.getLogger(Optimizer.class.getName());

    private final CalibrationConfigGroup calibrationConfig;
    private final int startIteration;
    private final String repoUrl;
    private final String repoBranch;
    private final String repoCommit;
    private final String optimizerPath;
    private final String runScript;
    private final String pythonPath;
    public Optimizer(CalibrationConfigGroup calibrationConfig){
        this.calibrationConfig = calibrationConfig;
        this.startIteration = calibrationConfig.getStartIteration();
        this.repoUrl = calibrationConfig.getRepoUrl();
        this.repoBranch = calibrationConfig.getRepoBranch();
        this.repoCommit = calibrationConfig.getRepoCommit();
        this.optimizerPath = calibrationConfig.getOptimizerPath();
        this.runScript = optimizerPath + "/run.py";
        this.pythonPath = calibrationConfig.getPythonPath();

        if (calibrationConfig.isActivated()){
            getOptimizerIfMissing();
        }
    }

    private void getOptimizerIfMissing() {
        Path path = Paths.get(runScript);
        if (Files.notExists(path)) {
            logger.info("Optimizer path not found. Attempting to clone repository...");
            cloneRepository();
            if (Files.notExists(path)) {
                throw new RuntimeException("Optimizer path does not exist after cloning: " + optimizerPath);
            }
        }
        checkoutRepoBranch();
    }

    private void cloneRepository() {
        try {
            ProcessBuilder builder = new ProcessBuilder("git", "clone", repoUrl, optimizerPath);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(line);
                }
            }

            int exitCode = process.waitFor();
            logger.info("Exited with code: " + exitCode);
            if (exitCode != 0) {
                throw new RuntimeException("Git clone failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Exception during git clone", e);
            throw new RuntimeException("Failed to clone repository", e);
        }
    }

    private void checkoutRepoBranch() {
        try {
            // Always fetch latest before checkout to ensure commit exists
            ProcessBuilder fetchBuilder = new ProcessBuilder("git", "-C", optimizerPath, "fetch", "--all");
            fetchBuilder.redirectErrorStream(true);
            Process fetchProcess = fetchBuilder.start();
            try (BufferedReader fetchReader = new BufferedReader(new InputStreamReader(fetchProcess.getInputStream()))) {
                String line;
                while ((line = fetchReader.readLine()) != null) {
                    logger.info(line);
                }
            }
            int fetchExitCode = fetchProcess.waitFor();
            logger.info("Fetch exited with code: " + fetchExitCode);
            if (fetchExitCode != 0) {
                throw new RuntimeException("Git fetch failed with exit code: " + fetchExitCode);
            }

            ProcessBuilder builder;
            if (repoCommit == null) {
                builder = new ProcessBuilder("git", "-C", optimizerPath, "checkout", repoBranch);
            } else {
                builder = new ProcessBuilder("git", "-C", optimizerPath, "checkout", repoCommit);
            }
            builder.redirectErrorStream(true);
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info(line);
                }
            }

            int exitCode = process.waitFor();
            logger.info("Checkout exited with code: " + exitCode);
            if (exitCode != 0) {
                throw new RuntimeException("Git checkout failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Exception during git checkout", e);
            throw new RuntimeException("Failed to checkout branch", e);
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
            command.add(pythonPath);
            command.add(runScript);

            // Build command-line arguments based on config
            Map<String, String> args = new HashMap<>();

            // Add arguments from calibrationConfig
            args.put("--variables-path", variablesIterationPath);
            args.put("--selector", calibrationConfig.getSelector());
            args.put("--input-parameters", lastParametersFilePath);
            args.put("--output-parameters", newParametersFilePath);
            args.put("--eqasim-cache-path", calibrationConfig.getEqasimCachePath());
            args.put("--iteration", String.valueOf(iteration));
            args.put("--bounds", calibrationConfig.getBounds());
            args.put("--metric", calibrationConfig.getMetric());
            args.put("--optimizer", calibrationConfig.getOptimizer());
            args.put("--momentum", calibrationConfig.getMomentum());
            args.put("--beta-momentum", String.valueOf(calibrationConfig.getBetaMomentum()));
            args.put("--max-evals", String.valueOf(calibrationConfig.getMaxEval()));
            args.put("--population-sample", String.valueOf(calibrationConfig.getPopulationSample()));
            args.put("--objectives", calibrationConfig.getObjectives());
            args.put("--optimizer-cache", optimizerPath + "/optimizerCache");
            args.put("--distance-bins", calibrationConfig.getDistanceBins());

            // Append to command list
            args.forEach((key, value) -> {
                command.add(key);
                command.add(value);
            });

            // Execute the Python script
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            logger.info("\n\nRunning calibration script...");
            logger.info("Command: " + String.join(" ", command));

            Process process = processBuilder.start();

            // Read output
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("     [CALIBRATION] " + line);
            }

            int exitCode = process.waitFor();
            logger.info("Calibration script exited with code: " + exitCode+"\n\n");

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute calibration script", e);
        }
    }


}