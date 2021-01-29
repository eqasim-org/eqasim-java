package org.eqasim.ile_de_france.flow.calibration;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;

public class CapacityAdjustment {
	public CapacityAdjustment(CommandLine cmd) {
		cmd.get
	}
	
	public void apply(Config config, Network network) {
		double baseFactor = config.qsim().getFlowCapFactor();
		
		config.qsim().setFlowCapFactor(1.0);
		config.qsim().setStorageCapFactor(baseFactor);
		
		for (Link link : network.getLinks().values()) {
			String osmType = (String) link.getAttributes().getAttribute("osm:highway");
			
			if (osmType.contains("motorway")) {
				
			} else if (osmType.contains("trunk")) {
				
			} else if (osmType.contains("primary")) {
				
			} else if (osmType.contains("secondary")) {
				
			} else {
				
			}
		}
	}
}
