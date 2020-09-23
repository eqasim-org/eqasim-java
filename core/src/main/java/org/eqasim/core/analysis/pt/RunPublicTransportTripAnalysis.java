package org.eqasim.core.analysis.pt;

import java.io.IOException;
import java.util.Collection;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

public class RunPublicTransportTripAnalysis {
	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("events-path", "output-path") //
				.build();

		String outputPath = cmd.getOptionStrict("output-path");
		String eventsPath = cmd.getOptionStrict("events-path");

		PublicTransportTripListener tripListener = new PublicTransportTripListener();
		PublicTransportTripReader reader = new PublicTransportTripReader(tripListener);
		Collection<PublicTransportTripItem> trips = reader.readTrips(eventsPath);

		new PublicTransportTripWriter(trips).write(outputPath);
	}
}
