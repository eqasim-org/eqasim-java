package org.eqasim.automated_vehicles.analysis;

import java.io.IOException;

import org.eqasim.automated_vehicles.components.AvPersonAnalysisFilter;
import org.eqasim.core.analysis.RunTripAnalysis;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

public class RunAvTripAnalysis {
	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("output-path") //
				.allowOptions("population-path", "events-path", "network-path") //
				.allowOptions("stage-activity-types", "network-modes") //
				.allowOptions("input-distance-units", "output-distance-units") //
				.build();

		RunTripAnalysis.run(cmd, new AvPersonAnalysisFilter());
	}
}