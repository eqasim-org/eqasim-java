package org.eqasim.switzerland.ch_cmdp.calibration;


import org.eqasim.core.components.calibration.CalibrationConfigGroup;
import org.eqasim.core.components.calibration.optimizer.StandardOptimizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmdpOptimizer extends StandardOptimizer {
    private double beta = 0.2;

    public CmdpOptimizer(CalibrationConfigGroup calibrationConfig) {
        super(calibrationConfig);
    }

    /**
     * Executes the external Python script with dynamically built arguments.
     */
    @Override
    protected void runPythonScript(int iteration, String newParametersFilePath, String lastParametersFilePath, String variablesIterationPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add(pythonPath);
            command.add(runScript);
            // this beta will be used to smooth the variations in the parameters
            if (iteration <= startIteration ){
                beta = calibrationConfig.getBetaMomentum();
            } else if (iteration%30==0){
                beta = Math.min(0.98, 1.0 - (1.0-beta)/2.0);
            }
            logger.info("Using beta = " + beta + " for iteration " + iteration);

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
            args.put("--beta-momentum", String.valueOf(beta));
            args.put("--max-evals", String.valueOf(calibrationConfig.getMaxEval()));
            args.put("--population-sample", String.valueOf(calibrationConfig.getPopulationSample()));
            args.put("--objectives", calibrationConfig.getObjectives());
            args.put("--optimizer-cache", optimizerPath + "/optimizerCache");
            args.put("--distance-bins", calibrationConfig.getDistanceBins());
            args.put("--utilities", "ch_cmdp");
            args.put("--modes-in-loss", "car,pt,walk,bike,car_passenger");
            args.put("--beta-decay-method", "constant");
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