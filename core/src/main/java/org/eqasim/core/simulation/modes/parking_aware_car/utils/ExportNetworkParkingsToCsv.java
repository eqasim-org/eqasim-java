package org.eqasim.core.simulation.modes.parking_aware_car.utils;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ExportNetworkParkingsToCsv {

    public static void export(Network network, String outputPath) throws IOException {
        NetworkWideParkingSpaceStore networkWideParkingSpaceStore = new NetworkWideParkingSpaceStore(network);
        export(network, networkWideParkingSpaceStore, outputPath);
    }

    public static void export(Network network, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, String outputPath) throws IOException {
        List<Id<ParkingType>> parkingTypes = networkWideParkingSpaceStore.getParkingTypes().keySet().stream().toList();
        CompactCSVWriter csvWriter = new CompactCSVWriter(new BufferedWriter(new FileWriter(outputPath)), ';');
        CSVLineBuilder csvLineBuilder = new CSVLineBuilder();
        csvLineBuilder.add("linkId");
        csvLineBuilder.addAll(parkingTypes.stream().map(Id::toString).toList());
        csvWriter.writeNext(csvLineBuilder.build());

        for(Id<Link> linkId: network.getLinks().keySet()) {
            csvLineBuilder = new CSVLineBuilder();
            csvLineBuilder.add(linkId.toString());
            for(Id<ParkingType> parkingTypeId: parkingTypes) {
                int capacity = Optional.ofNullable(networkWideParkingSpaceStore.getLinkParkingSpaces(linkId).get(parkingTypeId)).map(ParkingSpace::capacity).orElse(0);
                csvLineBuilder.add(String.valueOf(capacity));
            }
            csvWriter.writeNext(csvLineBuilder.build());
        }
        csvWriter.close();
    }

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("network-path", "output-path")
                .build();

        String networkPath = commandLine.getOptionStrict("network-path");
        String outputPath = commandLine.getOptionStrict("output-path");

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);

        export(scenario.getNetwork(), outputPath);
    }
}
