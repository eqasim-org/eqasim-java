package org.eqasim.ile_de_france.parking;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

public class RunImputeParkingPressure {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("parking-pressure-path", "network-path", "output-path") //
				.build();

		String networkPath = cmd.getOptionStrict("network-path");
		String outputPath = cmd.getOptionStrict("output-path");
		String parkingPressurePath = cmd.getOptionStrict("parking-pressure-path");

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkPath);

		ParkingPressureData data = ParkingPressureData.createFromNetwork(network, new File(parkingPressurePath));

		for (Link link : network.getLinks().values()) {
			link.getAttributes().putAttribute(ParkingPressureData.ATTRIBUTE, data.getParkingPressure(link.getId()));
		}

		new NetworkWriter(network).write(outputPath);
	}
}
