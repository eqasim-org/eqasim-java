package org.eqasim.san_francisco.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class AdaptNetworkSpeeds {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(args[0]);

		Config config2 = ConfigUtils.createConfig();
		Scenario scenario2 = ScenarioUtils.createMutableScenario(config2);
		MatsimNetworkReader netReader2 = new MatsimNetworkReader(scenario2.getNetwork());
		netReader2.readFile(args[1]);

		for (Link link2 : scenario2.getNetwork().getLinks().values()) {
			Link link = scenario.getNetwork().getLinks().get(link2.getId());
			if (link.getAttributes().getAttribute("osm:way:highway") != null
					&& link.getAttributes().getAttribute("osm:way:highway").equals("secondary"))
				link.setFreespeed(36.6);
			if (link.getAttributes().getAttribute("osm:way:highway") != null
					&& link.getAttributes().getAttribute("osm:way:highway").equals("tertiary"))
				link.setFreespeed(36.6);

		}

		new NetworkWriter(scenario.getNetwork()).write(args[2]);
	}

}
