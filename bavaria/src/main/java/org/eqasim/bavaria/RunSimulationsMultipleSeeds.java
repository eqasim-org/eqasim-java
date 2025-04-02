package org.eqasim.bavaria;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RunSimulationsMultipleSeeds extends SimulationRunnerBase {
    private static final Logger LOGGER = Logger.getLogger(RunSimulationsMultipleSeeds.class.getName());

    static public void main(String[] args) throws Exception {
        // Check if city parameter is provided
        if (args.length < 2 || !args[0].equals("--city")) {
            LOGGER.severe("Please provide the city name using the --city parameter");
            LOGGER.severe("Usage: java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city <city_name>");
            System.exit(1);
        }

        final String city = args[1];
        LOGGER.info("Running simulation for city: " + city);

        // Configuration settings
        String configPath = city + "_config.xml";
        String workingDirectory = "bavaria/data/simulation_input/simulations_for_cities/" + city + "/";

        LOGGER.info("Starting simulation with the following settings:");
        LOGGER.info("Configuration file: " + configPath);
        LOGGER.info("Working directory: " + workingDirectory);

        // Create a fixed thread pool with 6 threads
        ExecutorService executor = Executors.newFixedThreadPool(6);
        LOGGER.info("Created thread pool with 6 threads");

        final String networkFile = city + "_network.xml.gz";
        LOGGER.info("Using network file: " + networkFile);

        final String outputDirectory = "bavaria/data/simulation_output/basecases/simulations_for_cities/" + city + "/";
        LOGGER.info("Output will be written to: " + outputDirectory);   

        // Check if the output file exists
        boolean simulationRanSuccessfully = checkIfFileExists(outputDirectory, "output_links.csv.gz");
        LOGGER.info("Checking if output exists: " + simulationRanSuccessfully);

        if (!simulationRanSuccessfully) {
            try {
                if (outputDirectoryExists(outputDirectory)) {
                    createAndEmptyDirectory(outputDirectory);
                    LOGGER.info("Emptied output directory while preserving log files: " + outputDirectory);
                } else {
                    Files.createDirectories(Paths.get(outputDirectory));
                    LOGGER.info("Created output directory: " + outputDirectory);
                }

                executor.submit(() -> {
                    LOGGER.info("Starting simulation task for: " + networkFile);
                    try {
                        runSimulation(configPath, networkFile, outputDirectory, workingDirectory, args, 1, "8", "8", null);
                        LOGGER.info("Completed simulation for: " + networkFile);
                        deleteUnwantedFiles(outputDirectory);
                        LOGGER.info("Deleted unwanted files for: " + networkFile);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LOGGER.log(Level.SEVERE, "Simulation interrupted for: " + networkFile, e);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error in simulation for: " + networkFile, e);
                    }
                });
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to setup output directory: " + outputDirectory, e);
                throw e;
            }
        } else {
            LOGGER.info("Skipping simulation - output already exists in: " + outputDirectory);
        }

        // Shutdown the executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(300, TimeUnit.HOURS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(360, TimeUnit.SECONDS)) {
                    LOGGER.severe("Executor did not terminate properly");
                }
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            LOGGER.log(Level.SEVERE, "Executor was interrupted", ie);
        }
        LOGGER.info("All simulations completed");
    }
}




        // for (int i = 1000; i <= 3000; i += 1000) {
        //     String folder = "networks_" + i;
        //     List<String> networkFiles = networkFilesMap.get(folder);
        //     if (networkFiles == null || networkFiles.isEmpty()) {
        //         continue;
        //     }

        //     for (String networkFile : networkFiles) {
        //         final String finalNetworkFile = networkFile; // Final variable for lambda capture
        //         final String networkName = finalNetworkFile.replace(".xml.gz", "");
        //         System.out.println("Network name: " + networkName);
        //         final int randomSeed = Integer.parseInt(networkName.split("_")[4]);
        //         System.out.println("Random seed: " + randomSeed);
        //         final String outputDirectory = Paths.get(workingDirectory, "output_" + folder, networkName).toString();
        //         System.out.println("Submitting task for: " + networkName);

        //         // Check if the file exists in the directory
        //         boolean fileExists = checkIfFileExists(outputDirectory, "output_links.csv.gz");

        //         if (!outputDirectoryExists(outputDirectory) || !fileExists) {
        //             try {
        //                 createAndEmptyDirectory(outputDirectory);
        //                 System.out.println("The directory " + outputDirectory + " has been emptied.");
        //             } catch (IOException e) {
        //                 System.err.println("An error occurred while creating or emptying the directory: " + e.getMessage());
        //                 continue; // Skip to the next iteration if directory creation or emptying fails
        //             }

        //             executor.submit(() -> {
        //                 System.out.println("Starting task for: " + finalNetworkFile);
        //                 try {
        //                     runSimulation(configPath, Paths.get("networks", folder, networkFile).toString(), outputDirectory, workingDirectory, args, randomSeed, true, "8", "8", null);
        //                     deleteUnwantedFiles(outputDirectory);
        //                     System.out.println("Deleted unwanted files for: " + networkFile);
        //                     System.out.println("Processed file: " + networkFile);
        //                 } catch (InterruptedException e) {
        //                     Thread.currentThread().interrupt();
        //                     LOGGER.log(Level.SEVERE, "Task interrupted for file: " + finalNetworkFile, e);
        //                 } 
        //                 catch (Exception e) {
        //                     LOGGER.log(Level.SEVERE, "Error processing file: " + networkFile, e);
        //                 }
        //             });
        //         } else {
        //             LOGGER.info("Skipping simulation for existing output directory: " + outputDirectory);
        //         }
        //     }
        // }

