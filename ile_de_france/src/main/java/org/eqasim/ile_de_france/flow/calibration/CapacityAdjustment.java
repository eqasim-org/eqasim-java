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
		this.majorFactor = Double.parseDouble(cmd.getOptionStrict("capacity:major"));
		this.immediateFactor = Double.parseDouble(cmd.getOptionStrict("capacity:immediate"));
		this.minorFactor = Double.parseDouble(cmd.getOptionStrict("capacity:minor"));
	}

	public void apply(Config config, Network network) {
		double baseFactor = config.qsim().getFlowCapFactor();

		config.qsim().setFlowCapFactor(1.0);
		config.qsim().setStorageCapFactor(baseFactor);

		for (Link link : network.getLinks().values()) {
			String osmType = (String) link.getAttributes().getAttribute("osm:highway");

			if (osmType.contains("motorway") || osmType.contains("trunk")) {
				link.setCapacity(link.getCapacity() * baseFactor * majorFactor);
			} else if (osmType.contains("primary") || osmType.contains("secondary") || osmType.contains("tertiary")) {
				link.setCapacity(link.getCapacity() * baseFactor * immediateFactor);
			} else {
				link.setCapacity(link.getCapacity() * baseFactor * minorFactor);
			}
		}
	}
}
