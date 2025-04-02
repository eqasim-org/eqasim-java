package org.eqasim.bavaria;

import java.io.File;
import java.io.IOException;
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
        // Configuration settings
        String configPath = "bavaria_config.xml";
        String workingDirectory = "bavaria/data/simulation_input/bavaria/";

        // List all files in the directory
        // Map<String, List<String>> networkFilesMap = getNetworkFiles(networkDirectory);

        // Create a fixed thread pool with 5 threads
        ExecutorService executor = Executors.newFixedThreadPool(6);

        // Create a fixed thread pool with 2 threads
        LOGGER.info("Starting simulations");

        final String networkFile = "bavaria_network.xml.gz";
        final String networkName = networkFile.replace(".xml.gz", "");
        System.out.println("Network name: " + networkName);

        final String outputDirectory = "bavaria/data/simulation_output/bavaria/";
        boolean fileExists = checkIfFileExists(outputDirectory, "output_links.csv.gz");

        if (!outputDirectoryExists(outputDirectory) || !fileExists) {
            try {
                createAndEmptyDirectory(outputDirectory);
                System.out.println("The directory " + outputDirectory + " has been emptied.");
            } catch (IOException e) {
                System.err.println("An error occurred while creating or emptying the directory: " + e.getMessage());
            }

            executor.submit(() -> {
                System.out.println("Starting task for: " + networkFile);
                try {
                    runSimulation(configPath, networkFile, outputDirectory, workingDirectory, args, 1, true, "8", "8", null);
                    deleteUnwantedFiles(outputDirectory);
                    System.out.println("Deleted unwanted files for: " + networkFile);
                    System.out.println("Processed file: " + networkFile);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.log(Level.SEVERE, "Task interrupted for file: " + networkFile, e);
                } 
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error processing file: " + networkFile, e);
                }
            });
        } else {
            LOGGER.info("Skipping simulation for existing output directory: " + outputDirectory);
        }
        
        // Shutdown the executor
        executor.shutdown();
        try {
            // Increase the wait time for all tasks to complete
            if (!executor.awaitTermination(300, TimeUnit.HOURS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(360, TimeUnit.SECONDS)) {
                    LOGGER.severe("Executor did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("Simulations completed");
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

