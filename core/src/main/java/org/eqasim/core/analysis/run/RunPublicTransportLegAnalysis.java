package org.eqasim.core.analysis.run;

import java.io.IOException;
import java.util.Collection;

import org.eqasim.core.analysis.pt.PublicTransportLegItem;
import org.eqasim.core.analysis.pt.PublicTransportLegListener;
import org.eqasim.core.analysis.pt.PublicTransportLegReader;
import org.eqasim.core.analysis.pt.PublicTransportLegWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

public class RunPublicTransportLegAnalysis {
	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("events-path", "output-path") //
				.build();

		String outputPath = cmd.getOptionStrict("output-path");
		String eventsPath = cmd.getOptionStrict("events-path");

		PublicTransportLegListener tripListener = new PublicTransportLegListener();
		PublicTransportLegReader reader = new PublicTransportLegReader(tripListener);
		Collection<PublicTransportLegItem> trips = reader.readTrips(eventsPath);

		new PublicTransportLegWriter(trips).write(outputPath);
	}
}
