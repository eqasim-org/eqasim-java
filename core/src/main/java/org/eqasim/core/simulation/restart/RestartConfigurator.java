package org.eqasim.core.simulation.restart;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.eqasim.core.simulation.termination.EqasimTerminationModule;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.VDFUpdateListener;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * This class is supposed to be applied after all configuration is set up. It
 * will modify the paths such that each new runs ends up in a "restart-*"
 * subdirectory of the output directory defined in the controller config group.
 * Each time the simulation is restarted on the same output directory, a new
 * restart directory is created and the output files of the last successful
 * iteration of the previous run are set as input for the new restart. See
 * addDefaultMappings for the files that are reconfigured by default.
 * 
 * ATTENTION: This class is conceptual, it has not really been tested yet. It
 * has been added so the code doesn't get lost. If you are willing to test this
 * thoroughly, feel free to do so!
 */
public class RestartConfigurator {
    private final static Logger logger = LogManager.getLogger(RestartConfigurator.class);

    static public final String PREFIX = "restart_";
    static public final String CMD = "restart";

    public RestartConfigurator() {
        logger.warn(
                "ATTENTION: This class is conceptual, it has not really been tested yet. It has been added so the code doesn't get lost. If you are willing to test this thoroughly, feel free to do so!");
    }

    private record Mapping(boolean isIteration, String sourcePath, Function<Config, Consumer<String>> mapper) {
    }

    private List<Mapping> mappings = new LinkedList<>();

    private File getRestartPath(File basePath, int restartIndex) {
        return new File(basePath, PREFIX + restartIndex);
    }

    public void apply(Config config) {
        File basePath = new File(config.controller().getOutputDirectory());

        // find the number of restarts
        int maximumRestartIndex = -1;
        if (basePath.exists()) {
            for (File restartPath : basePath.listFiles()) {
                if (restartPath.getName().startsWith(PREFIX)) {
                    int restartIndex = Integer.parseInt(restartPath.getName().replace(PREFIX, ""));
                    maximumRestartIndex = Math.max(maximumRestartIndex, restartIndex);
                }
            }
        }

        int selectedRestartIndex = -1;
        int selectedIteration = -1;

        if (maximumRestartIndex == -1) {
            logger.info(String.format("No restarts found"));
        } else {
            logger.info(String.format("Examining %d restarts", maximumRestartIndex + 1));

            for (int restartIndex = 0; restartIndex <= maximumRestartIndex; restartIndex++) {
                File restartPath = getRestartPath(basePath, restartIndex);

                // find the available iterations
                int maximumIterationIndex = 0;
                for (File iterationPath : new File(restartPath, "ITERS").listFiles()) {
                    if (iterationPath.getName().startsWith("it.")) {
                        int iterationIndex = Integer.parseInt(iterationPath.getName().replace("it.", ""));
                        maximumIterationIndex = Math.max(iterationIndex, maximumIterationIndex);
                    }
                }

                // check for output level files that must be present
                config.controller().setOutputDirectory(restartPath.getAbsolutePath());
                OutputDirectoryHierarchy outputDirectoryHierarchy = new OutputDirectoryHierarchy(config);

                List<String> missing = new LinkedList<>();
                for (Mapping mapping : mappings) {
                    if (!mapping.isIteration) {
                        String expectedPath = outputDirectoryHierarchy.getOutputFilename(mapping.sourcePath);

                        if (!new File(expectedPath).exists()) {
                            missing.add(mapping.sourcePath);
                        }
                    }
                }

                if (missing.size() == 0) {
                    for (int iterationIndex = 0; iterationIndex <= maximumIterationIndex; iterationIndex++) {
                        // check for iteration level files that must be present
                        missing.clear();

                        for (Mapping mapping : mappings) {
                            if (mapping.isIteration) {
                                String expectedPath = outputDirectoryHierarchy.getIterationFilename(iterationIndex,
                                        mapping.sourcePath);

                                if (!new File(expectedPath).exists()) {
                                    missing.add(mapping.sourcePath);
                                }
                            }
                        }

                        if (missing.size() == 0) {
                            // add files are in place
                            selectedRestartIndex = restartIndex;
                            selectedIteration = iterationIndex;

                            logger.info("Found a viable restart source: restart " + restartIndex + ", iteration "
                                    + iterationIndex);
                        }
                    }
                }
            }

            // we found a viable restart source
            if (selectedRestartIndex >= 0 && selectedIteration >= 0) {
                File restartPath = getRestartPath(basePath, selectedRestartIndex);
                config.controller().setOutputDirectory(restartPath.getAbsolutePath());
                OutputDirectoryHierarchy outputHierarchy = new OutputDirectoryHierarchy(config);

                // apply all mappings
                for (Mapping mapping : mappings) {
                    if (mapping.isIteration) {
                        File sourcePath = new File(
                                outputHierarchy.getIterationFilename(selectedIteration, mapping.sourcePath));
                        mapping.mapper.apply(config).accept(sourcePath.getAbsolutePath());
                    } else {
                        File sourcePath = new File(outputHierarchy.getOutputFilename(mapping.sourcePath));
                        mapping.mapper.apply(config).accept(sourcePath.getAbsolutePath());
                    }
                }
            }
        }

        // now set up the next restart
        int nextRestartIndex = maximumRestartIndex + 1;
        File restartPath = getRestartPath(basePath, nextRestartIndex);
        config.controller().setOutputDirectory(restartPath.getAbsolutePath());

        if (selectedIteration >= 0) {
            config.controller().setFirstIteration(selectedIteration);
        }

        logger.info("Restarting simulation with index " + nextRestartIndex + " and iteration "
                + config.controller().getFirstIteration() + " at " + restartPath.getAbsolutePath());
    }

    public void addMapping(boolean isIteration, String sourcePath, Function<Config, Consumer<String>> mapper) {
        mappings.add(new Mapping(isIteration, sourcePath, mapper));
    }

    public void clearMappings() {
        mappings.clear();
    }

    public void addDefaultMappings(Config config) {
        // plans
        addMapping(false, "plans.xml", c -> c.plans()::setInputFile);

        // termination
        if (config.getModules().containsKey(EqasimTerminationConfigGroup.GROUP_NAME)) {
            addMapping(true, EqasimTerminationModule.TERMINATION_CSV_FILE,
                    c -> EqasimTerminationConfigGroup.getOrCreate(c)::setHistoryFile);
        }

        // vdf
        if (config.getModules().containsKey(VDFConfigGroup.GROUP_NAME)) {
            addMapping(true, VDFUpdateListener.FLOW_FILE, c -> VDFConfigGroup.getOrCreate(c)::setInputFlowFile);
            addMapping(true, VDFUpdateListener.TRAVEL_TIMES_FILE,
                    c -> VDFConfigGroup.getOrCreate(c)::setInputTravelTimesFile);
        }
    }

    static public void setup(CommandLine cmd, Config config) {
        if (cmd.getOption(CMD).map(Boolean::parseBoolean).orElse(false)) {
            RestartConfigurator configurator = new RestartConfigurator();
            configurator.addDefaultMappings(config);
            configurator.apply(config);
        }
    }
}
