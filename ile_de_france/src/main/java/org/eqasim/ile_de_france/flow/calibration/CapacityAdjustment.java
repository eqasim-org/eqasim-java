package org.eqasim.ile_de_france.flow.calibration;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;

public class CapacityAdjustment {
	private final double majorFactor;
	private final double immediateFactor;
	private final double minorFactor;

	public CapacityAdjustment(CommandLine cmd) throws NumberFormatException, ConfigurationException {
		this.majorFactor = cmd.getOption("capacity:major").map(Double::parseDouble).orElse(1.0);
		this.immediateFactor = cmd.getOption("capacity:immediate").map(Double::parseDouble).orElse(1.0);
		this.minorFactor = cmd.getOption("capacity:minor").map(Double::parseDouble).orElse(1.0);
	}

	public void apply(Config config, Network network) {
		for (Link link : network.getLinks().values()) {
			String osmType = (String) link.getAttributes().getAttribute("osm:way:highway");

			if (osmType != null) {
				if (osmType.contains("motorway") || osmType.contains("trunk")) {
					link.setCapacity(link.getCapacity() * majorFactor);
				} else if (osmType.contains("primary") || osmType.contains("secondary")
						|| osmType.contains("tertiary")) {
					link.setCapacity(link.getCapacity() * immediateFactor);
				} else {
					link.setCapacity(link.getCapacity() * minorFactor);
				}
			}
		}
	}
}
