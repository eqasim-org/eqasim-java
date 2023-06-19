package org.eqasim.ile_de_france;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;

public class OsmNetworkAdjustment {
	static public final String CAPACITY_PREFIX = "osm-capacity";
	static public final String SPEED_PREFIX = "osm-speed";

	private final double majorCapacityFactor;
	private final double intermediateCapacityFactor;
	private final double minorCapacityFactor;
	private final double linkCapacityFactor;

	private final double majorSpeedFactor;
	private final double intermediateSpeedFactor;
	private final double minorSpeedFactor;
	private final double linkSpeedFactor;

	public OsmNetworkAdjustment(CommandLine cmd) throws NumberFormatException, ConfigurationException {
		double majorCapacityFactor = 1.0;
		double intermediateCapacityFactor = 1.0;
		double minorCapacityFactor = 1.0;
		double linkCapacityFactor = 1.0;

		for (String option : cmd.getAvailableOptions()) {
			if (option.startsWith(CAPACITY_PREFIX + ":")) {
				String slot = option.replace(CAPACITY_PREFIX + ":", "");

				switch (slot) {
				case "major":
					majorCapacityFactor = Double.parseDouble(cmd.getOptionStrict(CAPACITY_PREFIX + ":major"));
					break;
				case "intermediate":
					intermediateCapacityFactor = Double
							.parseDouble(cmd.getOptionStrict(CAPACITY_PREFIX + ":intermediate"));
					break;
				case "minor":
					minorCapacityFactor = Double.parseDouble(cmd.getOptionStrict(CAPACITY_PREFIX + ":minor"));
					break;
				case "link":
					linkCapacityFactor = Double.parseDouble(cmd.getOptionStrict(CAPACITY_PREFIX + ":link"));
					break;
				default:
					throw new IllegalStateException("Unknown slot: " + slot);
				}
			}
		}

		this.majorCapacityFactor = majorCapacityFactor;
		this.intermediateCapacityFactor = intermediateCapacityFactor;
		this.minorCapacityFactor = minorCapacityFactor;
		this.linkCapacityFactor = linkCapacityFactor;

		double majorSpeedFactor = 1.0;
		double intermediateSpeedFactor = 1.0;
		double minorSpeedFactor = 1.0;
		double linkSpeedFactor = 1.0;

		for (String option : cmd.getAvailableOptions()) {
			if (option.startsWith(SPEED_PREFIX + ":")) {
				String slot = option.replace(SPEED_PREFIX + ":", "");

				switch (slot) {
				case "major":
					majorSpeedFactor = Double.parseDouble(cmd.getOptionStrict(SPEED_PREFIX + ":major"));
					break;
				case "intermediate":
					intermediateSpeedFactor = Double.parseDouble(cmd.getOptionStrict(SPEED_PREFIX + ":intermediate"));
					break;
				case "minor":
					minorSpeedFactor = Double.parseDouble(cmd.getOptionStrict(SPEED_PREFIX + ":minor"));
					break;
				case "link":
					linkSpeedFactor = Double.parseDouble(cmd.getOptionStrict(SPEED_PREFIX + ":link"));
					break;
				default:
					throw new IllegalStateException("Unknown slot: " + slot);
				}
			}
		}

		this.majorSpeedFactor = majorSpeedFactor;
		this.intermediateSpeedFactor = intermediateSpeedFactor;
		this.minorSpeedFactor = minorSpeedFactor;
		this.linkSpeedFactor = linkSpeedFactor;
	}

	public void apply(Config config, Network network) {
		for (Link link : network.getLinks().values()) {
			String osmType = (String) link.getAttributes().getAttribute("osm:way:highway");

			if (osmType != null) {
				if (osmType.contains("motorway") || osmType.contains("trunk")) {
					link.setCapacity(link.getCapacity() * majorCapacityFactor);
					link.setFreespeed(link.getFreespeed() * majorSpeedFactor);
				} else if (osmType.contains("primary") || osmType.contains("secondary")) {
					link.setCapacity(link.getCapacity() * intermediateCapacityFactor);
					link.setFreespeed(link.getFreespeed() * intermediateSpeedFactor);
				} else {
					link.setCapacity(link.getCapacity() * minorCapacityFactor);
					link.setFreespeed(link.getFreespeed() * minorSpeedFactor);
				}

				if (osmType.contains("_link")) {
					link.setCapacity(link.getCapacity() * linkCapacityFactor);
					link.setFreespeed(link.getFreespeed() * linkSpeedFactor);
				}
			}
		}
	}
}