package org.eqasim.ile_de_france;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.eqasim.core.simulation.modes.drt.utils.CreateDrtVehicles;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
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
                process = builder.inheritIO().start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                process.waitFor();
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

        int[] fleetSizes = IntStream.rangeClosed(minFleetSize / fleetSizeStep, maxFleetSize / fleetSizeStep).map(i -> fleetSizeStep * i).toArray();
        summary.put("fleet_sizes", fleetSizes);

        ExecutorService executorService = Executors.newFixedThreadPool(processes);
        int totalSimulations = 0;
        int skippedSimulations = 0;
        for (int fleetSize : fleetSizes) {
            Path vehiclesPath = Paths.get(configPath, "..", "drt_vehicles", "drt_vehicles_" + fleetSize + ".xml").toAbsolutePath().normalize();
            CreateDrtVehicles.main(new String[]{
                    "--network-path", Paths.get(configPath, "..", config.network().getInputFile()).toAbsolutePath().normalize().toString(),
                    "--output-vehicles-path", vehiclesPath.toString(),
                    "--vehicles-number", String.valueOf(fleetSize)
            });
            for(List<Object> argValues: cartesianProductValues) {
                totalSimulations+=1;
                List<String> argValuesString = argValues.stream().map(Object::toString).toList();
                Path outputDirectoryPath = Paths.get(mainOutputDirectory, String.format("output_%d_%s", fleetSize, String.join("_", argValuesString))).normalize();

                if (Files.exists(outputDirectoryPath.resolve("eqasim_trips.csv"))) {
                    System.out.println("Skipping simulation with outputDirectory " + outputDirectoryPath);
                    skippedSimulations+=1;
                    continue;
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
                executorService.execute(new MainClassRunnable(RunSimulation.class, ManagementFactory.getRuntimeMXBean().getInputArguments(), simArgs));
            }
        }
        var shutdownListener = new Thread(() -> {
            System.out.println("shutdown in 5s");
            try {
                Thread.sleep(5000);
                executorService.shutdownNow();
            } catch (InterruptedException e) {}
        });

        Runtime.getRuntime().addShutdownHook(shutdownListener);

        executorService.shutdown();
        boolean success = executorService.awaitTermination(2, TimeUnit.DAYS);
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
}
