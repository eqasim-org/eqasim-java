package org.eqasim.core.simulation.modes.feeder_drt.analysis.run;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.modes.drt.analysis.utils.VehicleRegistry;
import org.eqasim.core.simulation.modes.feeder_drt.analysis.passengers.FeederTripSequenceListener;
import org.eqasim.core.simulation.modes.feeder_drt.analysis.passengers.FeederTripSequenceWriter;
import org.eqasim.core.simulation.modes.feeder_drt.config.FeederDrtConfigGroup;
import org.eqasim.core.simulation.modes.feeder_drt.config.MultiModeFeederDrtConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.File;
import java.io.IOException;
import java.util.Map;


public class RunFeederDrtPassengerAnalysis {

    private final FeederTripSequenceListener feederTripSequenceListener;
    private final String outputPath;
    private final VehicleRegistry vehicleRegistry;

    public RunFeederDrtPassengerAnalysis(Map<String, FeederDrtConfigGroup> modeConfigs, Network network, String outputPath) {
        this.vehicleRegistry = new VehicleRegistry();
        this.feederTripSequenceListener = new FeederTripSequenceListener(modeConfigs, this.vehicleRegistry, network);
        this.outputPath = outputPath;
    }

    public void prepare(EventsManager eventsManager) {
        eventsManager.addHandler(this.feederTripSequenceListener);
        eventsManager.addHandler(this.vehicleRegistry);
    }

    public void writeAnalysis() throws IOException {
        new FeederTripSequenceWriter(this.feederTripSequenceListener).writeTripItems(new File(this.outputPath));
    }

    static public void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "events-path", "network-path", "output-path") //
                .build();

        String configPath = cmd.getOptionStrict("config-path");
        String eventsPath = cmd.getOptionStrict("events-path");
        String networkPath = cmd.getOptionStrict("network-path");
        String outputPath = cmd.getOptionStrict("output-path");

        EqasimConfigurator configurator = new EqasimConfigurator();
        Config config = ConfigUtils.loadConfig(configPath, configurator.getConfigGroups());
        configurator.addOptionalConfigGroups(config);

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(networkPath);

        if(!config.getModules().containsKey(MultiModeFeederDrtConfigGroup.GROUP_NAME)) {
            throw new IllegalStateException(String.format("Config must contain a '%s' module", MultiModeFeederDrtConfigGroup.GROUP_NAME));
        }
        MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup = (MultiModeFeederDrtConfigGroup) config.getModules().get(MultiModeFeederDrtConfigGroup.GROUP_NAME);

        RunFeederDrtPassengerAnalysis analysis = new RunFeederDrtPassengerAnalysis(multiModeFeederDrtConfigGroup.getModeConfigs(), network, outputPath);
        EventsManager eventsManager = EventsUtils.createEventsManager();
        analysis.prepare(eventsManager);


        eventsManager.initProcessing();
        DrtEventsReaders.createEventsReader(eventsManager).readFile(eventsPath);
        eventsManager.finishProcessing();

        analysis.writeAnalysis();
    }
}
