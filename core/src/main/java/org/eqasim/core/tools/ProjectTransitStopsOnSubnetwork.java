package org.eqasim.core.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Set;

public class ProjectTransitStopsOnSubnetwork {

    private static Logger logger = LogManager.getLogger(ProjectTransitStopsOnSubnetwork.class);

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("schedule-path", "network-path", "mode", "output-path")
                .allowOptions("max-distance")
                .build();

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new TransitScheduleReader(scenario).readFile(commandLine.getOptionStrict("schedule-path"));
        new MatsimNetworkReader(scenario.getNetwork()).readFile(commandLine.getOptionStrict("network-path"));

        Network subNetwork = NetworkUtils.createNetwork();
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(subNetwork, Set.of(commandLine.getOptionStrict("mode")));

        TransitSchedule transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();

        int maxDistance = Integer.parseInt(commandLine.getOption("max-distance").orElse("250"));

        for(TransitStopFacility transitStopFacility: scenario.getTransitSchedule().getFacilities().values()) {
            Node nearestNode = NetworkUtils.getNearestNode(subNetwork, transitStopFacility.getCoord());
            Link link = NetworkUtils.getNearestLink(subNetwork, transitStopFacility.getCoord());
            double distance = NetworkUtils.getEuclideanDistance(transitStopFacility.getCoord(), NetworkUtils.getNearestNode(subNetwork, nearestNode.getCoord()).getCoord());
            if(distance > maxDistance) {
                logger.info(String.format("Link %s ignored because distance to nearest node in the subnetwork (%f meters) is above the %d meters limit", transitStopFacility.getId().toString(), distance, maxDistance));
            } else {
                transitStopFacility.setLinkId(link.getId());
                transitSchedule.addStopFacility(transitStopFacility);
            }
        }

        new TransitScheduleWriter(transitSchedule).writeFile(commandLine.getOptionStrict("output-path"));
    }
}
