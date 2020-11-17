package org.eqasim.projects.astra16.analysis;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class RunPublicTransportAnalysis {
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("shape-path", "schedule-path", "network-path") //
				.build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("network-path"));
		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));

		ScenarioExtent extent = new ShapeScenarioExtent.Builder(new File(cmd.getOptionStrict("shape-path")),
				Optional.empty(), Optional.empty()).build();

		Network network = scenario.getNetwork();
		double distance_m = 0.0;

		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				if (transitRoute.getTransportMode().equals("bus")) {
					NetworkRoute networkRoute = transitRoute.getRoute();
					List<Id<Link>> linkIds = new ArrayList<>(networkRoute.getLinkIds());
					linkIds.add(networkRoute.getEndLinkId());

					int departures = transitRoute.getDepartures().size();

					for (Id<Link> linkId : linkIds) {
						Link link = network.getLinks().get(linkId);

						if (extent.isInside(link.getFromNode().getCoord())
								&& extent.isInside(link.getToNode().getCoord())) {
							distance_m += link.getLength() * departures;
						}
					}
				}
			}
		}

		System.out.println(String.format("%.2f km", distance_m * 1e-3));
	}
}
