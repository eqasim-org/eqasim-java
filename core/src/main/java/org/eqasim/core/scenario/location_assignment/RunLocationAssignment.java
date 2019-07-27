package org.eqasim.core.scenario.location_assignment;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.eqasim.core.location_assignment.matsim.discretizer.FacilityTypeDiscretizerFactory;
import org.eqasim.core.location_assignment.matsim.solver.MATSimAssignmentSolverBuilder;
import org.eqasim.core.misc.InteractionStageActivityTypes;
import org.eqasim.core.scenario.location_assignment.listener.StatisticsListener;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;

public class RunLocationAssignment {
	static public void main(String[] args)
			throws IOException, InterruptedException, ExecutionException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("population-path", "facilities-path", "quantiles-path", "distributions-path",
						"output-path") //
				.allowOptions("threads", "discretization-iterations", "random-seed", "batch-size", "statistics-path",
						"threshold-factor", "sample-size", "feasible-distance-iterations", "gravity-iterations") //
				.build();

		// Setting up activity types
		Set<String> relevantActivityTypes = new HashSet<>(Arrays.asList("leisure", "shop", "other"));

		// Setting up modes
		double thresholdFactor = cmd.getOption("threshold-factor").map(Double::parseDouble).orElse(1.0);

		Map<String, Double> discretizationThresholds = new HashMap<>();
		discretizationThresholds.put("car", 200.0 * thresholdFactor);
		discretizationThresholds.put("car_passenger", 200.0 * thresholdFactor);
		discretizationThresholds.put("pt", 200.0 * thresholdFactor);
		discretizationThresholds.put("bike", 100.0 * thresholdFactor);
		discretizationThresholds.put("walk", 100.0 * thresholdFactor);

		// Set up random seed
		int randomSeed = cmd.getOption("random-seed").map(Integer::parseInt).orElse(0);

		// Load population and facilities
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new PopulationReader(scenario).readFile(cmd.getOptionStrict("population-path"));
		new MatsimFacilitiesReader(scenario).readFile(cmd.getOptionStrict("facilities-path"));

		// Set up data supply
		FacilityTypeDiscretizerFactory discretizerFactory = new FacilityTypeDiscretizerFactory(relevantActivityTypes);
		discretizerFactory.loadFacilities(scenario.getActivityFacilities());

		File quantilesPath = new File(cmd.getOptionStrict("quantiles-path"));
		File distributionsPath = new File(cmd.getOptionStrict("distributions-path"));

		DistanceSamplerFactory distanceSamplerFactory = new DistanceSamplerFactory(randomSeed);
		distanceSamplerFactory.load(quantilesPath, distributionsPath);

		StageActivityTypes stageActivityTypes = new InteractionStageActivityTypes();

		// Downsampling
		Set<Id<Person>> removeIds = new HashSet<>();
		Random random = new Random(0);
		double sampleSize = cmd.getOption("sample-size").map(Double::parseDouble).orElse(1.0);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (random.nextDouble() > sampleSize) {
				removeIds.add(person.getId());
			}
		}

		for (Id<Person> personId : removeIds) {
			scenario.getPopulation().removePerson(personId);
		}

		// Some cleanup
		MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			new TripsToLegsAlgorithm(stageActivityTypes, mainModeIdentifier).run(person.getSelectedPlan());
		}

		// Set up solver
		ProblemProvider problemProvider = new ProblemProvider(distanceSamplerFactory, discretizerFactory,
				discretizationThresholds);

		MATSimAssignmentSolverBuilder builder = new MATSimAssignmentSolverBuilder();

		builder.setVariableActivityTypes(relevantActivityTypes);
		builder.setRandomSeed(randomSeed);
		builder.setStageActivityTypes(stageActivityTypes);

		builder.setDiscretizerProvider(problemProvider);
		builder.setDistanceSamplerProvider(problemProvider);
		builder.setDiscretizationThresholdProvider(problemProvider);

		int discretizationIterations = cmd.getOption("discretization-iterations").map(Integer::parseInt).orElse(1000);
		builder.setMaximumDiscretizationIterations(discretizationIterations);

		int feasibleDistanceIterations = cmd.getOption("feasible-distance-iterations").map(Integer::parseInt)
				.orElse(1000);
		builder.setMaximumFeasibleDistanceSamples(feasibleDistanceIterations);

		int gravityIterations = cmd.getOption("gravity-iterations").map(Integer::parseInt).orElse(1000);
		builder.setMaximumGravityIterations(gravityIterations);

		// Run assignment
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		LocationAssignment locationAssignment = new LocationAssignment(builder, numberOfThreads, batchSize);

		if (cmd.hasOption("statistics-path")) {
			StatisticsListener listener = new StatisticsListener(new File(cmd.getOptionStrict("statistics-path")));
			locationAssignment.run(scenario.getPopulation(), listener);
			listener.close();
		} else {
			locationAssignment.run(scenario.getPopulation());
		}

		// Write population
		new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-path"));
	}
}
