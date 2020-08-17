package org.eqasim.ile_de_france.feeder;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;

import org.eqasim.core.components.transit.routing.EnrichedTransitRoute;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Injector;

public class RunDemandEstimation {
	private static double getTravelTime(List<? extends PlanElement> elements) {
		double travelTime = 0.0;

		boolean isFirstPt = true;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;
				travelTime += leg.getTravelTime();

				if (leg.getMode().equals("pt")) {
					if (isFirstPt) {
						EnrichedTransitRoute route = (EnrichedTransitRoute) leg.getRoute();
						travelTime -= route.getWaitingTime();

						isFirstPt = false;
					}
				}
			}
		}

		return travelTime;
	}

	private static boolean isRelevantType(String type) {
		if (type.equals("work")) {
			return true;
		}

		if (type.equals("education")) {
			return true;
		}

		if (type.equals("home")) {
			return true;
		}

		return false;
	}

	private static boolean containsGPE(List<? extends PlanElement> elements) {
		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().equals("pt")) {
					EnrichedTransitRoute route = (EnrichedTransitRoute) leg.getRoute();

					if (route.getTransitLineId().toString().contains("GPE:")) {
						return true;
					}
				}
			}
		}

		return false;
	}

	static private void writeLine(BufferedWriter writer, Person person, int tripIndex, Trip trip, boolean addOrigin,
			boolean addDestination, double initialTravelTime, double proposedTravelTime, double accessTime,
			double egressTime) throws IOException {
		writer.write(String.join(";", new String[] { //
				person.getId().toString(), String.valueOf(tripIndex), //
				String.valueOf(trip.getOriginActivity().getCoord().getX()),
				String.valueOf(trip.getOriginActivity().getCoord().getY()),
				String.valueOf(trip.getDestinationActivity().getCoord().getX()),
				String.valueOf(trip.getDestinationActivity().getCoord().getY()), String.valueOf(addOrigin),
				String.valueOf(addDestination), String.valueOf(initialTravelTime), String.valueOf(proposedTravelTime),
				String.format("LINESTRING(%f %f, %f %f)", trip.getOriginActivity().getCoord().getX(),
						trip.getOriginActivity().getCoord().getY(), trip.getDestinationActivity().getCoord().getX(),
						trip.getDestinationActivity().getCoord().getY()),
				String.valueOf(accessTime), String.valueOf(egressTime) }) + "\n");
	}

	static public void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "output-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.build();

		TripRouter tripRouter = injector.getInstance(TripRouter.class);
		Network network = injector.getInstance(Network.class);

		for (Link link : network.getLinks().values()) {
			link.setFreespeed(20.0 / 3.6);
		}

		double[] dimensions = NetworkUtils.getBoundingBox(network.getNodes().values());
		QuadTree<Facility> stopIndex = new QuadTree<Facility>(dimensions[0], dimensions[1], dimensions[2],
				dimensions[3]);

		Network carNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(carNetwork, Collections.singleton("car"));
		new NetworkCleaner().run(carNetwork);

		for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
			if (facility.getId().toString().contains("GPE:")) {
				Link closestLink = NetworkUtils.getNearestLink(carNetwork, facility.getCoord());
				stopIndex.put(facility.getCoord().getX(), facility.getCoord().getY(),
						new LinkWrapperFacility(network.getLinks().get(closestLink.getId())));
			}
		}

		double walkOffset = 5.0 * 60.0;

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));
		writer.write(
				"person_id;trip_index;origin_x;origin_y;destination_x;destination_y;add_origin;add_destination;initial_travel_time;proposed_travel_time;wkt;access_time;egress_time\n");

		ParallelProgress progress = new ParallelProgress("Finding demand ...",
				scenario.getPopulation().getPersons().size());
		progress.start();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			int tripIndex = 0;

			for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan(),
					tripRouter.getStageActivityTypes())) {
				String mode = tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements());

				String originType = trip.getOriginActivity().getType();
				String destinationType = trip.getDestinationActivity().getType();

				// Consider home <-> work / home <-> education trips (and back) done by PT or
				// car
				if (isRelevantType(originType) && isRelevantType(destinationType)
						&& (mode.equals("pt") || mode.equals("car"))) {
					double departureTime = 8.0 * 3600.0;

					Facility fromFacility = new LinkWrapperFacility(
							network.getLinks().get(trip.getOriginActivity().getLinkId()));
					Facility toFacility = new LinkWrapperFacility(
							network.getLinks().get(trip.getDestinationActivity().getLinkId()));

					double carTravelTime = getTravelTime(
							tripRouter.calcRoute("car", fromFacility, toFacility, departureTime, person));
					double ptTravelTime = getTravelTime(
							tripRouter.calcRoute("pt", fromFacility, toFacility, departureTime, person));
					double minimumTravelTime = Math.min(carTravelTime, ptTravelTime);

					Facility closestStartFacility = stopIndex.getClosest(trip.getOriginActivity().getCoord().getX(),
							trip.getOriginActivity().getCoord().getY());
					double accessTime = getTravelTime(
							tripRouter.calcRoute("car", fromFacility, closestStartFacility, departureTime, person))
							+ walkOffset;

					Facility closestEndFacility = stopIndex.getClosest(trip.getDestinationActivity().getCoord().getX(),
							trip.getDestinationActivity().getCoord().getY());
					double egressTime = getTravelTime(
							tripRouter.calcRoute("car", closestEndFacility, toFacility, departureTime, person))
							+ walkOffset;

					if (accessTime + egressTime < minimumTravelTime) {
						List<? extends PlanElement> accessStopRoute = tripRouter.calcRoute("pt", closestStartFacility,
								toFacility, departureTime, person);
						List<? extends PlanElement> egressStopRoute = tripRouter.calcRoute("pt", fromFacility,
								closestEndFacility, departureTime, person);
						List<? extends PlanElement> accessEgressStopRoute = tripRouter.calcRoute("pt",
								closestStartFacility, closestEndFacility, departureTime, person);

						if (containsGPE(accessStopRoute)) {
							double accessStopTravelTime = getTravelTime(accessStopRoute) + accessTime;

							if (accessStopTravelTime < minimumTravelTime) {
								writeLine(writer, person, tripIndex, trip, true, false, minimumTravelTime,
										accessStopTravelTime, accessTime - walkOffset, egressTime - walkOffset);
							}
						}

						if (containsGPE(egressStopRoute)) {
							double egressStopTravelTime = getTravelTime(egressStopRoute) + egressTime;

							if (egressStopTravelTime < minimumTravelTime) {
								writeLine(writer, person, tripIndex, trip, false, true, minimumTravelTime,
										egressStopTravelTime, accessTime - walkOffset, egressTime - walkOffset);
							}
						}

						if (containsGPE(accessEgressStopRoute)) {
							double accessEgressStopTravelTime = getTravelTime(accessEgressStopRoute) + accessTime
									+ egressTime;

							if (accessEgressStopTravelTime < minimumTravelTime) {
								writeLine(writer, person, tripIndex, trip, true, true, minimumTravelTime,
										accessEgressStopTravelTime, accessTime - walkOffset, egressTime - walkOffset);
							}
						}
					}

					tripIndex++;
				}
			}

			progress.update();
			writer.flush();
		}

		writer.close();
		progress.interrupt();
	}
}
