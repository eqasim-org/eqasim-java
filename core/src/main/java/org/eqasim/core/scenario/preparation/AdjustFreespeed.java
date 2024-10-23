package org.eqasim.core.scenario.preparation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

public class AdjustFreespeed {
	static public final String INITIAL_FREESPEED = "eqasim:initialFreespeed";

	static public void main(String[] args) throws ConfigurationException {
		CommandLine commandLine = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.allowPrefixes("freespeed") //
				.build();
		
		

	}

	static public void run(Network network) {

	}

	static public void validate(Scenario scenario) {
		boolean valid = false;

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAttributes().getAttribute(INITIAL_FREESPEED) != null) {
				valid = true;
				break;
			}
		}

		if (!valid) {
			throw new IllegalStateException("Did not find initial freespeed. Did you call AdjustFreespeed?");
		}
	}
}
