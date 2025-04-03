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
 * This class runs simulations for multiple network files organized in a specific folder structure.
 * For each city, there are multiple network size folders (e.g., networks_1000, networks_2000),
 * and each folder contains 1000 network files representing different scenarios.
 * 
 * The script automatically resumes from where it left off by checking which scenarios have been completed.
 * 
 * Call it like this: Process all network sizes (1000-10000)
 * java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleNetworkFiles --city augsburg
 * 
 * Process only specific sizes
 * java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleNetworkFiles --city augsburg --network-sizes 1000,2000
 * 
 * To compile (if made changes to the code, run this first): mvn clean package -Pstandalone --projects bavaria --also-make -DskipTests=true
 */
public class RunSimulationsMultipleNetworkFiles extends SimulationRunnerBase {
    private static final Logger LOGGER = Logger.getLogger(RunSimulationsMultipleNetworkFiles.class.getName());

    /**
     * Configuration class to hold all simulation parameters
     */
    private static class Config {
        String city = null;
        int threads = 12;   // Default to 12 threads
        int memory = 60;   // Default to 60GB
        // Default to all network sizes from 1000 to 10000
        List<Integer> networkSizes = Arrays.asList(1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000);
        int startScenario = 1;  // Default start from first scenario
        int endScenario = 1000; // Default end at last scenario

        @Override
        public String toString() {
            return String.format("Config{city='%s', threads=%d, memory=%dGB, networkSizes=%s, scenarios=%d-%d}", 
                city, threads, memory, networkSizes, startScenario, endScenario);
        }
    }

    /**
     * Parse and validate command line arguments
     */
    private static Config parseConfig(String[] args) {
        Config config = new Config();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--city") && i + 1 < args.length) {
                config.city = args[i + 1];
                i++;
            } else if (args[i].equals("--threads") && i + 1 < args.length) {
                try {
                    config.threads = Integer.parseInt(args[i + 1]);
                    if (config.threads < 1) {
                        throw new NumberFormatException("Number of threads must be positive");
                    }
                    i++;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number of threads. Please provide a positive integer.");
                }
            } else if (args[i].equals("--memory") && i + 1 < args.length) {
                try {
                    config.memory = Integer.parseInt(args[i + 1]);
                    if (config.memory < 1) {
                        throw new NumberFormatException("Memory allocation must be positive");
                    }
                    i++;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid memory allocation. Please provide a positive integer.");
                }
            } else if (args[i].equals("--network-sizes") && i + 1 < args.length) {
                try {
                    String[] sizes = args[i + 1].split(",");
                    config.networkSizes = new ArrayList<>();
                    for (String size : sizes) {
                        int networkSize = Integer.parseInt(size);
                        if (networkSize < 1) {
                            throw new NumberFormatException("Network size must be positive");
                        }
                        config.networkSizes.add(networkSize);
                    }
                    i++;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid network size. Please provide positive integers separated by commas.");
                }
            } else if (args[i].equals("--scenario-range") && i + 1 < args.length) {
                try {
                    String[] range = args[i + 1].split("-");
                    if (range.length != 2) {
                        throw new IllegalArgumentException("Scenario range must be in format start-end (e.g., 1-1000)");
                    }
                    config.startScenario = Integer.parseInt(range[0]);
                    config.endScenario = Integer.parseInt(range[1]);
                    if (config.startScenario < 1 || config.endScenario > 1000 || config.startScenario > config.endScenario) {
                        throw new IllegalArgumentException("Invalid scenario range. Must be between 1 and 1000, and start must be less than end.");
                    }
                    i++;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid scenario range. Please provide integers in format start-end.");
                }
            }
        }

        if (config.city == null) {
            throw new IllegalArgumentException("Please provide the city name using the --city parameter");
        }

        return config;
    }

    /**
     * Find the next scenario to process in a network size folder
     */
    private static synchronized int findNextScenario(String networkDir, String city, int startScenario, int endScenario) {
        for (int i = startScenario; i <= endScenario; i++) {
            // Format: augsburg_network_scenario_0001.xml.gz
            String networkFile = String.format("%s_network_scenario_%04d.xml.gz", city, i);
            Path networkPath = Paths.get(networkDir, networkFile);
            
            // Check if this scenario's output exists
            String outputDirectory = Paths.get(networkDir.replace("simulation_input", "simulation_output"),
                "output_networks_" + Paths.get(networkDir).getFileName().toString(),
                networkFile.replace(".xml.gz", "")).toString();
            
            if (!checkIfFileExists(outputDirectory, "output_links.csv.gz")) {
                return i;
            }
        }
        return -1; // All scenarios in range are completed
    }

    /**
     * Print usage instructions
     */
    private static void printUsage() {
        LOGGER.severe("Usage: java -cp bavaria/target/bavaria-1.5.0.jar org.eqasim.bavaria.RunSimulationsMultipleNetworkFiles " +
                     "--city <city_name> [--threads <number_of_threads>] [--memory <memory_in_GB>] " +
                     "[--network-sizes <size1,size2,...>] [--scenario-range <start-end>]\n" +
                     "Default network sizes: 1000,2000,3000,4000,5000,6000,7000,8000,9000,10000");
    }

    static public void main(String[] args) throws Exception {
        Config config;
        try {
            config = parseConfig(args);
        } catch (IllegalArgumentException e) {
            LOGGER.severe(e.getMessage());
            printUsage();
            System.exit(1);
            return;
        }

        LOGGER.info("Running simulation with configuration: " + config);

        // Create a fixed thread pool with specified number of threads
        ExecutorService executor = Executors.newFixedThreadPool(config.threads);
        LOGGER.info("Created thread pool with " + config.threads + " threads");

        // Process each network size
        for (int networkSize : config.networkSizes) {
            String networkFolder = "networks_" + networkSize;
            String baseWorkingDirectory = "bavaria/data/simulation_input/simulations_for_cities/" + config.city;
            String networkDir = Paths.get(baseWorkingDirectory, "networks", networkFolder).toString();
            
            // Check if network directory exists
            if (!Files.exists(Paths.get(networkDir))) {
                LOGGER.warning("Network directory not found: " + networkDir + ". Skipping this network size.");
                continue;
            }

            LOGGER.info("Processing network size " + networkSize + " in directory: " + networkDir);

            // Find next scenario to process
            int nextScenario;
            while ((nextScenario = findNextScenario(networkDir, config.city, config.startScenario, config.endScenario)) != -1) {
                final int scenarioNum = nextScenario;
                String networkFile = String.format("%s_network_scenario_%04d.xml.gz", config.city, scenarioNum);
                String networkPath = Paths.get(networkDir, networkFile).toString();

                if (!Files.exists(Paths.get(networkPath))) {
                    LOGGER.warning("Network file not found: " + networkPath + ". Skipping this scenario.");
                    continue;
                }

                // Process with seed 1
                final int currentSeed = 1;  // Always use seed 1
                final String outputDirectory = Paths.get(networkDir.replace("simulation_input", "simulation_output"),
                    "output_networks_" + networkSize,
                    networkFile.replace(".xml.gz", "") + "_seed_" + currentSeed).toString();

                // Check if output already exists
                if (checkIfFileExists(outputDirectory, "output_links.csv.gz")) {
                    LOGGER.info("Skipping scenario " + scenarioNum + " seed " + currentSeed + " - output exists");
                    continue;
                }

                try {
                    if (outputDirectoryExists(outputDirectory)) {
                        createAndEmptyDirectory(outputDirectory);
                        LOGGER.info("Emptied output directory: " + outputDirectory);
                    } else {
                        Files.createDirectories(Paths.get(outputDirectory));
                        LOGGER.info("Created output directory: " + outputDirectory);
                    }

                    // Submit simulation task
                    executor.submit(() -> {
                        LOGGER.info("Starting simulation for scenario " + scenarioNum + " with seed " + currentSeed);
                        try {
                            String configPath = config.city + "_config.xml";
                            runSimulation(configPath, networkPath, outputDirectory, baseWorkingDirectory, args, 
                                currentSeed, config.threads, config.threads, config.memory);
                            deleteUnwantedFiles(outputDirectory);
                            LOGGER.info("Completed simulation for scenario " + scenarioNum + " with seed " + currentSeed);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            LOGGER.log(Level.SEVERE, "Simulation interrupted for scenario " + scenarioNum + " with seed " + currentSeed, e);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error in simulation for scenario " + scenarioNum + " with seed " + currentSeed, e);
                        }
                    });
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Failed to setup output directory: " + outputDirectory, e);
                    continue;
                }
            }
            LOGGER.info("Completed all scenarios in range for network size " + networkSize);
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


