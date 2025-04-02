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

/**
 * With this class, we can run multiple simulations for a specified city using different random seeds.
 * The class parses command line arguments to determine the city name and the number of seeds to use.
 * It sets up the configuration and working directory for the simulation, and creates a thread pool to run the simulations concurrently.
 * 
 * To call this class, use the following command:
 * nohup java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city bamberg > output.log 2>&1 &
 * 
 * If you wnat to run multiple seeds, you can do so by adding the --seeds parameter, i.e.:
 * nohup java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city bamberg --seeds 3 > output.log 2>&1 &
 * 
 * TODO: Consider adding methodology for running all cities in one run. But it could be that we don't need this.
 */

public class RunSimulationsMultipleSeeds extends SimulationRunnerBase {
    private static final Logger LOGGER = Logger.getLogger(RunSimulationsMultipleSeeds.class.getName());

    static public void main(String[] args) throws Exception {
        // Parse command line arguments
        String city = null;
        int numSeeds = 1; // Default to 1 seed if not specified

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--city") && i + 1 < args.length) {
                city = args[i + 1];
                i++; // Skip the next argument
            } else if (args[i].equals("--seeds") && i + 1 < args.length) {
                try {
                    numSeeds = Integer.parseInt(args[i + 1]);
                    if (numSeeds < 1) {
                        throw new NumberFormatException("Number of seeds must be positive");
                    }
                    i++; // Skip the next argument
                } catch (NumberFormatException e) {
                    LOGGER.severe("Invalid number of seeds. Please provide a positive integer.");
                    LOGGER.severe("Usage: java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city <city_name> [--seeds <number_of_seeds>]");
                    System.exit(1);
                }
            }
        }

        // Check if required city parameter is provided
        if (city == null) {
            LOGGER.severe("Please provide the city name using the --city parameter");
            LOGGER.severe("Usage: java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleSeeds --city <city_name> [--seeds <number_of_seeds>]");
            System.exit(1);
        }

        LOGGER.info("Running simulation for city: " + city + " with " + numSeeds + " random seeds");

        // Configuration settings
        String configPath = city + "_config.xml";
        String workingDirectory = "bavaria/data/simulation_input/simulations_for_cities/" + city + "/";

        LOGGER.info("Starting simulation with the following settings:");
        LOGGER.info("Configuration file: " + configPath);
        LOGGER.info("Working directory: " + workingDirectory);
        LOGGER.info("Number of random seeds: " + numSeeds);

        // Create a fixed thread pool with 6 threads
        ExecutorService executor = Executors.newFixedThreadPool(6);
        LOGGER.info("Created thread pool with 6 threads");

        final String networkFile = city + "_network.xml.gz";
        LOGGER.info("Using network file: " + networkFile);

        for (int seed = 1; seed <= numSeeds; seed++) {
            final int currentSeed = seed;
            final String seedOutputDirectory = "bavaria/data/simulation_output/basecases/simulations_for_cities/" + city + "/" + city + "_seed_" + currentSeed + "/";
            LOGGER.info("Output for seed " + currentSeed + " will be written to: " + seedOutputDirectory);

            // Check if the output file exists for the current seed
            boolean seedSimulationRanSuccessfully = checkIfFileExists(seedOutputDirectory, "output_links.csv.gz");
            LOGGER.info("Checking if output exists for seed " + currentSeed + ": " + seedSimulationRanSuccessfully);

            if (!seedSimulationRanSuccessfully) {
                try {
                    if (outputDirectoryExists(seedOutputDirectory)) {
                        createAndEmptyDirectory(seedOutputDirectory);
                        LOGGER.info("Emptied output directory while preserving log files: " + seedOutputDirectory);
                    } else {
                        Files.createDirectories(Paths.get(seedOutputDirectory));
                        LOGGER.info("Created output directory: " + seedOutputDirectory);
                    }

                    // Submit task for the current seed
                    executor.submit(() -> {
                        LOGGER.info("Starting simulation task for: " + networkFile + " with seed " + currentSeed);
                        try {
                            runSimulation(configPath, networkFile, seedOutputDirectory, workingDirectory, args, currentSeed, "8", "8", null);
                            LOGGER.info("Completed simulation for: " + networkFile + " with seed " + currentSeed);
                            deleteUnwantedFiles(seedOutputDirectory);
                            LOGGER.info("Deleted unwanted files for: " + networkFile + " with seed " + currentSeed);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            LOGGER.log(Level.SEVERE, "Simulation interrupted for: " + networkFile + " with seed " + currentSeed, e);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error in simulation for: " + networkFile + " with seed " + currentSeed, e);
                        }
                    });
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Failed to setup output directory: " + seedOutputDirectory, e);
                    throw e;
                }
            } else {
                LOGGER.info("Skipping simulation for seed " + currentSeed + " - output already exists in: " + seedOutputDirectory);
            }
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

