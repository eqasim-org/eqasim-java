package org.eqasim.bavaria;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.logging.*;

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

public abstract class SimulationRunnerBase {
    protected static final Logger LOGGER = Logger.getLogger(SimulationRunnerBase.class.getName());

   /**
     * Runs the MATSim simulation with the given configuration path and output directory.
     *
     * @param configPath      The path to the configuration file.
     * @param networkFile     The network file to use for the simulation.
     * @param outputDirectory The directory where output files will be stored.
     * @param workingDirectory The working directory.
     * @param args            Command line arguments.
     * @param randomSeed      The random seed for the simulation.
     * @throws Exception if an error occurs during the simulation setup or execution.
     */
    protected static void runSimulation(final String configPath, final String networkFile, final String outputDirectory, 
        final String workingDirectory, final String[] args, 
        final int randomSeed) throws Exception {
        runSimulation(configPath, networkFile, outputDirectory, workingDirectory, args, randomSeed, "12", "12", null);
    }

   /**
     * Runs the MATSim simulation with the given configuration path and output directory.
     *
     * @param configPath      The path to the configuration file.
     * @param networkFile     The network file to use for the simulation.
     * @param outputDirectory The directory where output files will be stored.
     * @param workingDirectory The working directory.
     * @param args            Command line arguments.
     * @throws Exception if an error occurs during the simulation setup or execution.
     */
    protected static void runSimulation(final String configPath, final String networkFile, final String outputDirectory, 
    final String workingDirectory, final String[] args, 
    final int randomSeed,
    final String numberOfThreads,
    final String numberOfThreadsQSim,
    final String memoryAllocation) throws Exception {

        String fullConfigPath = Paths.get(workingDirectory, configPath).toString();

        final String memoryAllocationString;
        final List<String> arguments;
        if (memoryAllocation != null && !memoryAllocation.isEmpty()) {
            memoryAllocationString = "-Xms" + memoryAllocation + "g -Xmx" + memoryAllocation + "g";
            arguments = Arrays.asList("java", memoryAllocationString, "-cp",
            "bavaria/target/bavaria-1.5.0.jar",
            "org.eqasim.bavaria.RunSimulation10pct",
            "--config:global.numberOfThreads", numberOfThreads,
            "--config:qsim.numberOfThreads", numberOfThreadsQSim,
            "--config:global.randomSeed", String.valueOf(randomSeed),
            "--config:network.inputNetworkFile", networkFile,
            "--config:controler.outputDirectory", outputDirectory,
            "--config-path", fullConfigPath);

        } else {
            arguments = Arrays.asList("java", "-cp",
                "bavaria/target/bavaria-1.5.0.jar",
                "org.eqasim.bavaria.RunSimulation10pct",
                "--config:global.numberOfThreads", numberOfThreads,
                "--config:qsim.numberOfThreads", numberOfThreadsQSim,
                "--config:global.randomSeed", String.valueOf(randomSeed),
                "--config:network.inputNetworkFile", networkFile,
                "--config:controler.outputDirectory", outputDirectory,
                "--config-path", fullConfigPath);
        }

        System.out.println("Arguments for simulation:");
        for (String argument : arguments) {
            System.out.println(argument);
        }
        final File logFile = new File("simulation_" + networkFile.replace("_network.xml.gz", "") + "_seed_" + randomSeed + ".log"); 
        final File errorLogFile = new File("simulation_" + networkFile.replace("_network.xml.gz", "") + "_seed_" + randomSeed + ".error.log");
        System.out.println("Log file: " + logFile);
        System.out.println("Error log file: " + errorLogFile);

        Process process = new ProcessBuilder(arguments)
                .redirectOutput(logFile)
                .redirectError(errorLogFile)
                .start();
        System.out.println("Started process: " + outputDirectory);

        boolean interrupted = false;
        try {
            boolean finished = process.waitFor(3000, TimeUnit.HOURS);  // Increase wait time
            if (!finished) {
                process.destroy();  // destroy process if it times out
                throw new InterruptedException("Simulation process timed out: " + networkFile);
            }
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                throw new IOException("Simulation process failed with exit code " + exitValue + ": " + networkFile);
            }
        } catch (InterruptedException e) {
            interrupted = true;
            process.destroy();  // ensure process is destroyed if interrupted
            throw e;  // rethrow the exception to be handled in the calling method
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("Completed simulation for: " + networkFile);
    }

    protected static void createAndEmptyDirectory(String directory) throws IOException {
        Path dirPath = Paths.get(directory);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        } else if (Files.isDirectory(dirPath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                for (Path entry : stream) {
                    String fileName = entry.getFileName().toString();
                    // Preserve log files and essential output files
                    if (!fileName.equals("output_links.csv.gz")) {
                        deleteRecursively(entry);
                    }
                }
            }
        } else {
            throw new IOException("The path specified is not a directory: " + directory);
        }
    }

    protected static boolean checkIfFileExists(String directory, String fileName) {
        Path dirPath = Paths.get(directory);
        Path filePath = dirPath.resolve(fileName);
        boolean exists = Files.exists(filePath) && !Files.isDirectory(filePath);
        LOGGER.info("Checking if file exists: " + filePath + " - " + exists);
        return exists;
    }

    protected static Map<String, List<String>> getNetworkFiles(String directoryPath) {
        File mainDirectory = new File(directoryPath);
        File[] subDirs = mainDirectory.listFiles(File::isDirectory);

        if (subDirs == null) {
            System.out.println("The specified directory does not exist or is not a directory.");
            return Map.of();
        }

        return Arrays.stream(subDirs)
                .collect(Collectors.toMap(
                        File::getName,
                        subDir -> {
                            File[] filesList = subDir.listFiles((dir, name) -> name.endsWith(".xml.gz"));
                            List<String> xmlGzFiles = new ArrayList<>();
                            if (filesList != null) {
                                for (File file : filesList) {
                                    if (file.isFile()) {
                                        xmlGzFiles.add(file.getName());
                                    }
                                }
                                // Sort the list of file names
                                Collections.sort(xmlGzFiles);
                            }
                            return xmlGzFiles;
                        }
                ));
    }


    protected static boolean outputDirectoryExists(String outputDirectory) {
        File dir = new File(outputDirectory);
        boolean exists = dir.exists() && dir.isDirectory();
        LOGGER.info("Checking if output directory exists: " + outputDirectory + " - " + exists);
        return exists;
    }

    protected static void deleteUnwantedFiles(String outputDirectory) throws IOException {
        Path dir = Paths.get(outputDirectory);
        if (!Files.exists(dir)) {
            LOGGER.warning("Output directory does not exist: " + outputDirectory);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    LOGGER.info("Deleting directory: " + path);
                    deleteDirectoryRecursively(path);
                } else {
                    String fileName = path.getFileName().toString();
                    if (!fileName.equals("output_links.csv.gz")
                            && !fileName.equals("eqasim_pt.csv")
                            && !fileName.equals("eqasim_trips.csv")) {
                        Files.delete(path);
                        LOGGER.info("Deleted file: " + path);
                    } else {
                        LOGGER.info("Skipping file: " + path);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error deleting files in directory: " + outputDirectory, e);
        }
    }

    protected static void deleteDirectoryRecursively(Path directory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    deleteDirectoryRecursively(entry);
                } else {
                    Files.delete(entry);
                    LOGGER.info("Deleted file: " + entry);
                }
            }
        }
        Files.delete(directory);
        LOGGER.info("Deleted directory: " + directory);
    }

    protected static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }
}

