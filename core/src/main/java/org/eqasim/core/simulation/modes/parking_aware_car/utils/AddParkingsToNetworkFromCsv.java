package org.eqasim.core.simulation.modes.parking_aware_car.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

    public class AddParkingsToNetworkFromCsv {

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("csv-path", "network-path", "parking-type", "output-network-path")
                .allowOptions("link-column", "capacity-columns", "separator", "init-missing-entries")
                .allowOptions("allow-missing-links")
                .build();

        String csvPath = commandLine.getOptionStrict("csv-path");
        String networkPath = commandLine.getOptionStrict("network-path");
        String parkingType = commandLine.getOptionStrict("parking-type");

        String linkColumn = commandLine.getOption("link-column").orElse("link_id");
        Set<String> capacityColumns = Arrays.stream(commandLine.getOption("capacity-columns").orElse("capacity").split(",")).collect(Collectors.toSet());
        String separator = commandLine.getOption("separator").orElse(";");

        boolean allowMissingLinks = commandLine.hasOption("allow-missing-links");

        boolean initMissingEntries = commandLine.hasOption("init-missing-entries");

        BufferedReader fileReader = new BufferedReader(new FileReader(csvPath));
        List<String> header = Arrays.stream(fileReader.readLine().split(separator)).toList();

        int linkIdIndex = header.indexOf(linkColumn);
        List<Integer> capacityIndices = capacityColumns.stream().map(header::indexOf).toList();

        if(linkIdIndex < 0 || capacityIndices.stream().anyMatch(i -> i < 0)) {
            throw new IllegalStateException(String.format("Required columns %s and %s not found in header", String.join(", ", capacityColumns), linkColumn));
        }

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);

        fileReader.lines().forEach(line -> {
            List<String> items = Arrays.asList(line.split(separator));
            Id<Link> linkId = Id.createLinkId(items.get(linkIdIndex));
            int capacity = (int) capacityIndices.stream().map(items::get).mapToDouble(Double::parseDouble).sum();
            if(capacity == 0 && !initMissingEntries) {
                return;
            }
            Link link = scenario.getNetwork().getLinks().get(linkId);
            if (link == null) {
                if(allowMissingLinks) {
                    return;
                }
                throw new IllegalStateException(String.format("Link %s not found", linkId.toString()));
            }
            link.getAttributes().putAttribute("parking:"+parkingType, capacity);
        });

        if(initMissingEntries) {
            for(Link link: scenario.getNetwork().getLinks().values()) {
                if(link.getAttributes().getAttribute("parking:"+parkingType) == null) {
                    link.getAttributes().putAttribute("parking:"+parkingType, 0);
                }
            }
        }


        new NetworkWriter(scenario.getNetwork()).write(commandLine.getOptionStrict("output-network-path"));

    }
}
