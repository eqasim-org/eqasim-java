package org.eqasim.ile_de_france;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.eqasim.core.simulation.modes.drt.utils.CreateDrtVehicles;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.Tuple;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RunFleetSizing {

    public static Stream<List<Object>> cartesianProduct(List<List<Object>> sets) {
        return cartesianProduct(sets, 0);
    }

    public static Stream<List<Object>> cartesianProduct(List<List<Object>> sets, int index) {
        if (index == sets.size()) {
            List<Object> emptyList = new ArrayList<>();
            return Stream.of(emptyList);
        }
        List<Object> currentSet = sets.get(index);
        return currentSet.stream().flatMap(element -> cartesianProduct(sets, index+1)
                .map(list -> {
                    List<Object> newList = new ArrayList<>(list);
                    newList.add(0, element);
                    return newList;
                }));
    }

    public static class MainClassRunnable implements Runnable {

        private final Class mainClass;
        private final List<String> jvmArgs;
        private final List<String> args;

        public MainClassRunnable(Class mainClass, List<String> jvmArgs, List<String> args) {
            this.mainClass = mainClass;
            this.jvmArgs = jvmArgs;
            this.args = args;
        }

        @Override
        public void run() {
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            String classpath = System.getProperty("java.class.path");
            String className = mainClass.getName();

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.addAll(jvmArgs);
            command.add("-cp");
            command.add(classpath);
            command.add(className);
            command.addAll(args);

            ProcessBuilder builder = new ProcessBuilder(command);
            Process process;
            try {
                System.out.println(String.format("Starting %s %s", mainClass.getName(), String.join(" ", args)));
                process = builder.inheritIO().start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                process.waitFor();
                System.out.println(String.format("Finished %s %s", mainClass.getName(), String.join(" ", args)));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static void main(String[] args) throws IOException, CommandLine.ConfigurationException, InterruptedException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("config-path")
                .requireOptions("base-output-path")
                .allowOptions("processes")
                .allowOptions("threads-per-process")
                .allowOptions("config-options-separator")
                .allowOptions("drt-mode")
                .requireOptions("max-fleet-size", "min-fleet-size", "fleet-size-step")
                .allowOptions("one-full-simulation-per-fleet-size")
                .allowPrefixes("mode-choice-parameter", "cost-parameter")
                .build();

        String configPath = commandLine.getOptionStrict("config-path");
        String mainOutputDirectory = commandLine.getOptionStrict("base-output-path");
        String configOptionsSeparator = commandLine.getOption("config-options-separator").orElse(";");
        int processes = Integer.parseInt(commandLine.getOption("processes").orElse("4"));
        int threadsPerProcess = Integer.parseInt(commandLine.getOption("threads-per-process").orElse("4"));
        String drtMode = commandLine.getOption("drt-mode").orElse("drt");

        int maxFleetSize = Integer.parseInt(commandLine.getOptionStrict("max-fleet-size"));
        int minFleetSize = Integer.parseInt(commandLine.getOptionStrict("min-fleet-size"));
        int fleetSizeStep = Integer.parseInt(commandLine.getOptionStrict("fleet-size-step"));

        boolean oneFullSimulationPerFleetSize = Boolean.parseBoolean(commandLine.getOption("one-full-simulation-per-fleet-size").orElse("false"));


        Map<String, Object> summary = new HashMap<>();

        List<String> options = commandLine.getAvailableOptions().stream().filter(option -> option.contains(":")).toList();
        List<List<Object>> optionsValues = options.stream().map(option -> {
            try {
                return commandLine.getOptionStrict(option);
            } catch (CommandLine.ConfigurationException e) {
                throw new RuntimeException(e);
            }
        }).map(optionValue -> Arrays.stream(optionValue.split(configOptionsSeparator)).map(s -> (Object) s).toList()).toList();

        LinkedHashMap<String, List<Object>> optionsSummary = new LinkedHashMap<>();
        IntStream.range(0, options.size()).forEach(i -> optionsSummary.put(options.get(i), optionsValues.get(i)));
        summary.put("options", optionsSummary);

        List<List<Object>> cartesianProductValues = cartesianProduct(optionsValues).toList();

        Config config = ConfigUtils.loadConfig(configPath);

        List<Integer> fleetSizes = IntStream.rangeClosed(minFleetSize / fleetSizeStep, maxFleetSize / fleetSizeStep).map(i -> fleetSizeStep * i).boxed().toList();
        summary.put("fleet_sizes", fleetSizes);

        int totalSimulations = 0;
        int skippedSimulations = 0;
        Map<Integer, Path> vehiclesPathPerFleetSize = new HashMap<>();
        for (int fleetSize : fleetSizes) {
            Path vehiclesPath = Paths.get(configPath, "..", "drt_vehicles", "drt_vehicles_" + fleetSize + ".xml").toAbsolutePath().normalize();
            CreateDrtVehicles.main(new String[]{
                    "--network-path", Paths.get(configPath, "..", config.network().getInputFile()).toAbsolutePath().normalize().toString(),
                    "--output-vehicles-path", vehiclesPath.toString(),
                    "--vehicles-number", String.valueOf(fleetSize)
            });
            vehiclesPathPerFleetSize.put(fleetSize, vehiclesPath);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(processes);
        List<Path> fullSimulationOutputDirectoryPerArgValue = new ArrayList<>();
        for(List<Object> argValues: cartesianProductValues) {
            if(oneFullSimulationPerFleetSize) {
                totalSimulations += 1;
                Tuple<Path, Boolean> runResult = startGivenSimulation(configPath, mainOutputDirectory, options, argValues, fleetSizes.get(fleetSizes.size()-1), threadsPerProcess, drtMode, vehiclesPathPerFleetSize.get(fleetSizes.get(0)), executorService, null);
                if(!runResult.getSecond()) {
                    skippedSimulations+=1;
                }
                fullSimulationOutputDirectoryPerArgValue.add(runResult.getFirst());
            } else {
                Tuple<Integer, Integer> stats = startGivenOptionsFleetSizing(configPath, mainOutputDirectory, options, argValues, fleetSizes, threadsPerProcess, drtMode, vehiclesPathPerFleetSize, executorService, null);
                totalSimulations += stats.getFirst();
                skippedSimulations += stats.getSecond();
            }
        }
        executorService.shutdown();
        boolean success = executorService.awaitTermination(2, TimeUnit.DAYS);
        if(!success) {
            if(!oneFullSimulationPerFleetSize) {
                System.out.println("All simulations did not finish successfully");
            }
        } else {
            if(oneFullSimulationPerFleetSize) {
                System.out.println("Finished Full simulations");
            } else {
                System.out.println("All simulations finished successfully");
            }
        }


        if(oneFullSimulationPerFleetSize) {
            executorService = Executors.newFixedThreadPool(processes);
            for(int i=0; i<cartesianProductValues.size(); i++) {
                List<Object> argValues = cartesianProductValues.get(i);
                Path fullSimulationOutputDirectory = fullSimulationOutputDirectoryPerArgValue.get(i);
                Path argValuesReferencePlans = Path.of(configPath).toAbsolutePath().resolve("..").toAbsolutePath().relativize(fullSimulationOutputDirectory.resolve("output_plans.xml.gz").toAbsolutePath());
                if(!Files.exists(argValuesReferencePlans)) {
                    System.out.println(String.format("Base simulation %s could not finish, won't proceed with the other simulations", fullSimulationOutputDirectory));
                    skippedSimulations += fleetSizes.size()-1;
                    continue;
                }
                Tuple<Integer, Integer> stats = startGivenOptionsFleetSizing(configPath,
                        mainOutputDirectory,
                        options,
                        argValues,
                        fleetSizes.subList(0, fleetSizes.size()-1),
                        threadsPerProcess,
                        drtMode,
                        vehiclesPathPerFleetSize,
                        executorService,
                        argValuesReferencePlans);
                totalSimulations += stats.getFirst();
                skippedSimulations += stats.getSecond();
            }
        }

        executorService.shutdown();
        success = executorService.awaitTermination(2, TimeUnit.DAYS);
        if(!success) {
            System.out.println("All simulations did not finish successfully");
        } else {
            System.out.println("All simulations finished successfully");
        }

        summary.put("total_simulations", totalSimulations);
        summary.put("skipped_simulations", skippedSimulations);
        Map<String, Object> summaryWithDate = new LinkedHashMap<>();
        Path summaryFilePath = Paths.get(mainOutputDirectory, "fleet-sizing-summary.yml").normalize();
        if(Files.exists(summaryFilePath)) {
            MapType mapType = TypeFactory.defaultInstance().constructMapType(LinkedHashMap.class, String.class, Object.class);
            Map<String, Object> oldSummary = new ObjectMapper(new YAMLFactory()).readValue(new File(summaryFilePath.toString()), mapType);
            summaryWithDate.putAll(oldSummary);
        }
        summaryWithDate.put(new Date(System.currentTimeMillis()).toString(), summary);
        new ObjectMapper(new YAMLFactory()).writeValue(new File(summaryFilePath.toString()), summaryWithDate);
    }

    private static Tuple<Integer, Integer> startGivenOptionsFleetSizing(String configPath, String mainOutputDirectory, List<String> options, List<Object> argValues, List<Integer> fleetSizes, int threadsPerProcess, String drtMode, Map<Integer, Path> vehiclesPathPerFleetSize, ExecutorService executorService, Path inputPlansPath) {
        int totalSimulations = 0;
        int skippedSimulations = 0;
        for(Integer fleetSize: fleetSizes) {
            totalSimulations+=1;
            if(!startGivenSimulation(configPath, mainOutputDirectory, options, argValues, fleetSize, threadsPerProcess, drtMode, vehiclesPathPerFleetSize.get(fleetSize), executorService, inputPlansPath).getSecond()) {
                skippedSimulations += 1;
            }
        }
        return Tuple.of(totalSimulations, skippedSimulations);
    }

    private static Tuple<Path, Boolean> startGivenSimulation(String configPath, String mainOutputDirectory, List<String> options, List<Object> argValues, int fleetSize, int threadsPerProcess, String drtMode, Path vehiclesPath, ExecutorService executorService, Path inputPlansPath) {
        List<String> argValuesString = argValues.stream().map(Object::toString).toList();
        Path outputDirectoryPath = Paths.get(mainOutputDirectory, String.format("output_%s_%d", String.join("_", argValuesString), fleetSize)).normalize();

        if (Files.exists(outputDirectoryPath.resolve("eqasim_trips.csv"))) {
            System.out.println("Skipping simulation with outputDirectory " + outputDirectoryPath);
            return Tuple.of(outputDirectoryPath, false);
        }

        List<String> simArgs = new ArrayList<>();
        simArgs.add("--config-path");
        simArgs.add(configPath);
        IntStream.range(0, argValues.size()).mapToObj(i -> List.of("--"+options.get(i), argValuesString.get(i))).forEach(simArgs::addAll);
        simArgs.add("--config:global.numberOfThreads");
        simArgs.add(String.valueOf(threadsPerProcess));
        simArgs.add("--config:controler.outputDirectory");
        simArgs.add(outputDirectoryPath.toString());
        simArgs.add(String.format("--config:multiModeDrt.drt[mode=%s].vehiclesFile", drtMode));
        simArgs.add(vehiclesPath.toString());
        if(inputPlansPath  != null) {
            simArgs.add("--config:plans.inputPlansFile");
            simArgs.add(inputPlansPath.normalize().toString());
            simArgs.add("--config:controler.lastIteration");
            simArgs.add("0");
        }
        executorService.execute(new MainClassRunnable(RunSimulation.class, Collections.emptyList(), simArgs));
        return Tuple.of(outputDirectoryPath, true);
    }
}
