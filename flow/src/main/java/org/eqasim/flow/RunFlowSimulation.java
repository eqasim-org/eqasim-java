package org.eqasim.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioUtils;

public class RunFlowSimulation {
	private final static Logger logger = Logger.getLogger(RunFlowSimulation.class);

	static public void main(String[] args) throws ConfigurationException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "population-path") //
				.allowOptions("earliest-departure-time", "latest-departure-time", "network-mode", "trip-mode",
						"sampling-rate", "threads", "batch-size", "output-xml") //
				.build();

		double earliestDepartureTime = cmd.getOption("earliest-departure-time").map(Double::parseDouble)
				.orElse(6.5 * 3600.0);
		double latestDepartureTime = cmd.getOption("latest-departure-time").map(Double::parseDouble)
				.orElse(8.5 * 3600.0);

		String networkMode = cmd.getOption("network-mode").orElse("car");
		String tripMode = cmd.getOption("trip-mode").orElse("car");

		// Load data

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		logger.info("Loading data sets ...");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("network-path"));
		new PopulationReader(scenario).readFile(cmd.getOptionStrict("population-path"));

		// Prepare routing network

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton(networkMode));

		// Prepare trips

		logger.info("Preparing trips ...");
		List<FlowTrip> trips = new LinkedList<>();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();

			for (Trip trip : TripStructureUtils.getTrips(plan)) {
				String mode = TripStructureUtils.getRoutingModeIdentifier().identifyMainMode(trip.getTripElements());

				if (mode.equals(tripMode)) {
					Link originLink = roadNetwork.getLinks().get(trip.getOriginActivity().getLinkId());
					Link destinationLink = roadNetwork.getLinks().get(trip.getDestinationActivity().getLinkId());

					FlowTrip flowTrip = new FlowTrip(originLink, destinationLink);
					trips.add(flowTrip);
				}
			}
		}

		logger.info(String.format("  Found %d trips.", trips.size()));

		// Initialize router

		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		LeastCostPathCalculatorFactory factory = new FastAStarEuclideanFactory();

		FlowRouter router = new FlowRouter(factory, numberOfThreads, batchSize);

		// Initialize flow and travel times
		logger.info(String.format("Initializing flow and travel times ..."));

		List<Link> links = new ArrayList<>(roadNetwork.getLinks().values());

		IdMap<Link, Double> flow = new IdMap<>(Link.class);
		IdMap<Link, Double> travelTimes = new IdMap<>(Link.class);

		for (Link link : links) {
			flow.put(link.getId(), 0.0); // Zero flow
			travelTimes.put(link.getId(), link.getLength() / link.getFreespeed()); // Freeflow speed
		}

		// Perform initial all-or-nothing assignment
		logger.info(String.format("Performing initial assignment ..."));

		for (FlowRouter.Result result : router.calculatePaths(trips, roadNetwork, travelTimes)) {
			result.trip.updatePath(result.path);

			for (Link link : result.path.links) {
				flow.compute(link.getId(), (id, v) -> v + 1.0);
			}
		}

		// Perform MSA assignment
		logger.info(String.format("Starting MSA assignment ..."));

		double msaFactor = 0.1;
		double maximumDeviation = 0.01;
		double convergenceShare = 0.01;
		int maximumIterations = 1000;

		double interval = latestDepartureTime - earliestDepartureTime;
		double capacityFactor = interval / scenario.getNetwork().getCapacityPeriod();

		double scalingFactor = 1.0 / cmd.getOption("sampling-rate").map(Double::parseDouble).orElse(1.0);

		int iteration = 0;

		while (true) {
			iteration++;
			logger.info(String.format("Starting MSA iteration %d ...", iteration));

			// Update travel times based on flows and MSA

			for (Link link : links) {
				double linkCapacity = link.getCapacity() * capacityFactor;
				double flowValue = flow.get(link.getId()) * scalingFactor;
				double ratio = flowValue / linkCapacity;

				double previousTravelTime = travelTimes.get(link.getId());
				double freeFlowTravelTime = link.getLength() / link.getFreespeed();

				// BPR Function
				double updatedTravelTime = freeFlowTravelTime * (1.0 + 0.15 * Math.pow(ratio, 4.0));

				travelTimes.put(link.getId(), (1.0 - msaFactor) * previousTravelTime + msaFactor * updatedTravelTime);
			}

			// Select trips that have changed by more than the maximumGap

			List<FlowTrip> selectedTrips = new LinkedList<>();
			List<FlowTrip> unselectedTrips = new LinkedList<>();

			for (FlowTrip trip : trips) {
				double previousTravelTime = trip.travelTime;
				double updatedTravelTime = calculateTravelTime(trip.path, travelTimes);

				double relativeDeviation = (updatedTravelTime / previousTravelTime) - 1.0;

				if (relativeDeviation > maximumDeviation) {
					selectedTrips.add(trip);
				} else {
					unselectedTrips.add(trip);
				}
			}

			// Check convergence

			logger.info(String.format("Updating %d/%d trips in MSA iteration %d", selectedTrips.size(), trips.size(),
					iteration));

			double numberOfSelectedTrips = selectedTrips.size();
			double numberOfTrips = trips.size();

			if (numberOfSelectedTrips / numberOfTrips < convergenceShare || iteration >= maximumIterations) {
				break; // Less than convergenceShare % of trips change by more than maximumGap
			}

			// Derive new all-or-nothing flows from newly assigned paths ...

			for (Link link : links) {
				flow.put(link.getId(), 0.0);
			}

			for (FlowRouter.Result result : router.calculatePaths(selectedTrips, roadNetwork, travelTimes)) {
				result.trip.updatePath(result.path);

				for (Link link : result.path.links) {
					flow.compute(link.getId(), (id, v) -> v + 1.0);
				}
			}

			// ... and non-updated paths

			for (FlowTrip trip : unselectedTrips) {
				trip.updateTravelTime(calculateTravelTime(trip.path, travelTimes));

				for (Link link : trip.path.links) {
					flow.compute(link.getId(), (id, v) -> v + 1.0);
				}
			}
		}

		logger.info(String.format("MSA assignment has finished after %d iterations.", iteration));

		for (Link link : links) {
			link.getAttributes().putAttribute("travelTime", travelTimes.get(link.getId()));
			link.getAttributes().putAttribute("flow", flow.get(link.getId()));
		}

		logger.info("Writing output data ...");

		if (cmd.hasOption("output-xml")) {
			new NetworkWriter(roadNetwork).write(cmd.getOptionStrict("output-xml"));
		}
	}

	private static double calculateTravelTime(Path path, IdMap<Link, Double> travelTimes) {
		double value = 0.0;

		for (Link link : path.links) {
			value += travelTimes.get(link.getId());
		}

		return value;
	}
}
