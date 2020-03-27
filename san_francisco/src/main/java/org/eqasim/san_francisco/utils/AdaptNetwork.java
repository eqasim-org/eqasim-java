package org.eqasim.san_francisco.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class AdaptNetwork {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile(args[0]);
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			
			if (link.getAttributes().getAttribute("osm:way:highway") != null &&
					link.getAttributes().getAttribute("osm:way:highway").equals("unclassified")) {
				link.setCapacity(link.getCapacity() * 2);					
			}
			
			if (link.getAttributes().getAttribute("osm:way:highway") != null &&
					link.getAttributes().getAttribute("osm:way:highway").equals("tertiary")) {
				link.setCapacity(link.getCapacity() * 1.5);					
			}
			
			if (link.getAttributes().getAttribute("osm:way:highway") != null &&
					link.getAttributes().getAttribute("osm:way:highway").equals("secondary")) {
				link.setCapacity(link.getCapacity() / 11 * 12);					
			}
		}
		
		new NetworkWriter(scenario.getNetwork()).write(args[1]);
		
	}

}
