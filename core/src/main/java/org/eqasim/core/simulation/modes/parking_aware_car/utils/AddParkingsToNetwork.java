package org.eqasim.core.simulation.modes.parking_aware_car.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AddParkingsToNetwork {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("input-path", "output-path")
                .allowOptions("link-modes")
                .allowPrefixes("parking")
                .build();

        String inputPath = cmd.getOptionStrict("input-path");
        String outputPath = cmd.getOptionStrict("output-path");
        Set<String> linkModes = cmd.getOption("link-modes").map(s -> Set.of(s.split(","))).orElse(new HashSet<>());

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new MatsimNetworkReader(scenario.getNetwork()).readFile(inputPath);

        Map<String, Double> parkingSpecs = new HashMap<>();

        for(String option: cmd.getAvailableOptions()) {
            if(option.startsWith("parking:")) {
                double metersPerParking = Double.parseDouble(cmd.getOptionStrict(option));
                parkingSpecs.put(option, metersPerParking);
            }
        }

        for(Link link: scenario.getNetwork().getLinks().values()) {
            if(link.getAllowedModes().stream().anyMatch(linkModes::contains)) {
                for(Map.Entry<String, Double> entry: parkingSpecs.entrySet()) {
                    int parkingSpace = (int) (link.getLength() / entry.getValue());
                    link.getAttributes().putAttribute(entry.getKey(), parkingSpace);
                }
            }
        }

        new NetworkWriter(scenario.getNetwork()).write(outputPath);
    }

}
