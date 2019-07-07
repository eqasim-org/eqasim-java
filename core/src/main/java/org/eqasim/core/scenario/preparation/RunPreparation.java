package org.eqasim.core.scenario.preparation;

import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;

public class RunPreparation {
	static public void main(String[] args) throws ConfigurationException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-facilities-path", "output-facilities-path", "input-population-path",
						"output-population-path", "input-network-path", "output-network-path") //
				.allowOptions("threads", "batch-size") //
				.build();

		// Load data
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(scenario).readFile(cmd.getOptionStrict("input-facilities-path"));
		new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("input-network-path"));
		new PopulationReader(scenario).readFile(cmd.getOptionStrict("input-population-path"));

		// Assign links to facilities
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(10);

		RoadNetwork roadNetwork = new RoadNetwork(scenario.getNetwork());
		FacilityPlacement facilityPlacement = new FacilityPlacement(numberOfThreads, batchSize, roadNetwork);
		facilityPlacement.run(scenario.getActivityFacilities());

		// Fix freight activities (TODO: should go to the pipeline)
		FreightAssignment freightAssignment = new FreightAssignment(scenario.getNetwork(),
				scenario.getActivityFacilities());
		freightAssignment.run(scenario.getPopulation());

		// Assign links to activities which are consistent with facilities
		LinkAssignment linkAssignment = new LinkAssignment(scenario.getActivityFacilities());
		linkAssignment.run(scenario.getPopulation());

		// Adjust link lengths
		AdjustLinkLength adjustLinkLength = new AdjustLinkLength();
		adjustLinkLength.run(scenario.getNetwork());

		// Write facilities and population
		new FacilitiesWriter(scenario.getActivityFacilities()).write(cmd.getOptionStrict("output-facilities-path"));
		new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-population-path"));
		new NetworkWriter(scenario.getNetwork()).write(cmd.getOptionStrict("output-network-path"));
	}
}
