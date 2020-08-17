package org.eqasim.ile_de_france.feeder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.transit.routing.EnrichedTransitRoute;
import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Injector;
import com.google.inject.Provider;

public class DemandEstimator {
	private final double walkOffset = 5.0 * 60.0;
	private final double busSpeed = 30.0; // km / h

	private final Provider<TripRouter> tripRouterProvider;
	private final LeastCostPathCalculatorFactory routerFactory;

	private final Iterator<? extends Person> personIterator;
	private final ParallelProgress progress;
	private final Network network;
	private final QuadTree<Facility> stopIndex;
	private final BufferedWriter writer;

	public DemandEstimator(Injector injector, BufferedWriter writer) throws IOException {
		this.tripRouterProvider = injector.getProvider(TripRouter.class);
		this.routerFactory = injector.getInstance(LeastCostPathCalculatorFactory.class);

		Population population = injector.getInstance(Population.class);
		this.personIterator = population.getPersons().values().iterator();

		this.network = injector.getInstance(Network.class);

		double[] dimensions = NetworkUtils.getBoundingBox(network.getNodes().values());
		this.stopIndex = new QuadTree<Facility>(dimensions[0], dimensions[1], dimensions[2], dimensions[3]);

		Network carNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(carNetwork, Collections.singleton("car"));
		new NetworkCleaner().run(carNetwork);

		for (TransitStopFacility facility : injector.getInstance(TransitSchedule.class).getFacilities().values()) {
			if (facility.getId().toString().contains("GPE:")) {
				Link closestLink = network.getLinks()
						.get(NetworkUtils.getNearestLink(carNetwork, facility.getCoord()).getId());
				this.stopIndex.put(closestLink.getCoord().getX(), closestLink.getCoord().getY(),
						new LinkWrapperFacility(network.getLinks().get(closestLink.getId())));
			}
		}

		this.progress = new ParallelProgress("Estimating demand ...", population.getPersons().size());

		this.writer = writer;
		writer.write(
				"person_id;trip_index;origin_x;origin_y;destination_x;destination_y;add_origin;add_destination;initial_travel_time;proposed_travel_time;wkt;access_time;egress_time\n");

		writer.flush();
	}

	private double getTravelTime(List<? extends PlanElement> elements) {
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

	private boolean isRelevantType(String type) {
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

	private boolean containsGPE(List<? extends PlanElement> elements) {
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

	private void writeLine(Person person, int tripIndex, Trip trip, boolean addOrigin, boolean addDestination,
			double initialTravelTime, double proposedTravelTime, double accessTime, double egressTime) {
		synchronized (writer) {
			try {
				writer.write(String.join(";", new String[] { //
						person.getId().toString(), String.valueOf(tripIndex), //
						String.valueOf(trip.getOriginActivity().getCoord().getX()),
						String.valueOf(trip.getOriginActivity().getCoord().getY()),
						String.valueOf(trip.getDestinationActivity().getCoord().getX()),
						String.valueOf(trip.getDestinationActivity().getCoord().getY()), String.valueOf(addOrigin),
						String.valueOf(addDestination), String.valueOf(initialTravelTime),
						String.valueOf(proposedTravelTime),
						String.format("LINESTRING(%f %f, %f %f)", trip.getOriginActivity().getCoord().getX(),
								trip.getOriginActivity().getCoord().getY(),
								trip.getDestinationActivity().getCoord().getX(),
								trip.getDestinationActivity().getCoord().getY()),
						String.valueOf(accessTime), String.valueOf(egressTime) }) + "\n");
				writer.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private class Worker implements Runnable {
		private int numberOfTasks = 20;

		@Override
		public void run() {
			TripRouter tripRouter = tripRouterProvider.get();

			TravelTime constantTravelTime = new TravelTime() {
				@Override
				public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
					double speed = Math.min(busSpeed / 3.6, link.getFreespeed());
					return link.getLength() / speed;
				}
			};

			Network busNetwork = NetworkUtils.createNetwork();
			new TransportModeNetworkFilter(network).filter(busNetwork, new HashSet<>(Arrays.asList("car", "bus")));
			new NetworkCleaner().run(busNetwork);

			LeastCostPathCalculator busRouter = routerFactory.createPathCalculator(busNetwork,
					new OnlyTimeDependentTravelDisutility(constantTravelTime), constantTravelTime);

			while (true) {
				List<Person> tasks = new ArrayList<>(numberOfTasks);

				synchronized (personIterator) {
					if (!personIterator.hasNext()) {
						return;
					}

					while (tasks.size() < numberOfTasks && personIterator.hasNext()) {
						tasks.add(personIterator.next());
					}
				}

				for (Person person : tasks) {
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

							double minimumTravelTime = getTravelTime(
									tripRouter.calcRoute("pt", fromFacility, toFacility, departureTime, person));

							if (((String) person.getAttributes().getAttribute("hasLicense")).equals("yes")) {
								if (!((String) person.getAttributes().getAttribute("carAvailability")).equals("none")) {
									double carTravelTime = getTravelTime(tripRouter.calcRoute("car", fromFacility,
											toFacility, departureTime, person));

									minimumTravelTime = Math.min(minimumTravelTime, carTravelTime);
								}
							}

							Facility closestStartFacility = stopIndex.getClosest(
									trip.getOriginActivity().getCoord().getX(),
									trip.getOriginActivity().getCoord().getY());

							Facility closestEndFacility = stopIndex.getClosest(
									trip.getDestinationActivity().getCoord().getX(),
									trip.getDestinationActivity().getCoord().getY());

							if (closestStartFacility != closestEndFacility) {
								Link fromLink = busNetwork.getLinks().get(fromFacility.getLinkId());
								Link toLink = busNetwork.getLinks().get(toFacility.getLinkId());

								Link accessLink = busNetwork.getLinks().get(closestStartFacility.getLinkId());
								Link egressLink = busNetwork.getLinks().get(closestEndFacility.getLinkId());

								Path accessPath = busRouter.calcLeastCostPath(fromLink.getToNode(),
										accessLink.getFromNode(), departureTime, person, null);
								Path egressPath = busRouter.calcLeastCostPath(egressLink.getToNode(),
										toLink.getFromNode(), departureTime, person, null);

								double accessTime = accessPath.travelTime + walkOffset;
								double egressTime = egressPath.travelTime + walkOffset;

								if (accessTime + egressTime < minimumTravelTime) {
									List<? extends PlanElement> accessStopRoute = tripRouter.calcRoute("pt",
											closestStartFacility, toFacility, departureTime, person);
									List<? extends PlanElement> egressStopRoute = tripRouter.calcRoute("pt",
											fromFacility, closestEndFacility, departureTime, person);
									List<? extends PlanElement> accessEgressStopRoute = tripRouter.calcRoute("pt",
											closestStartFacility, closestEndFacility, departureTime, person);

									if (containsGPE(accessStopRoute)) {
										double accessStopTravelTime = getTravelTime(accessStopRoute) + accessTime;

										if (accessStopTravelTime < minimumTravelTime) {
											writeLine(person, tripIndex, trip, true, false, minimumTravelTime,
													accessStopTravelTime, accessTime - walkOffset,
													egressTime - walkOffset);
										}
									}

									if (containsGPE(egressStopRoute)) {
										double egressStopTravelTime = getTravelTime(egressStopRoute) + egressTime;

										if (egressStopTravelTime < minimumTravelTime) {
											writeLine(person, tripIndex, trip, false, true, minimumTravelTime,
													egressStopTravelTime, accessTime - walkOffset,
													egressTime - walkOffset);
										}
									}

									if (containsGPE(accessEgressStopRoute)) {
										double accessEgressStopTravelTime = getTravelTime(accessEgressStopRoute)
												+ accessTime + egressTime;

										if (accessEgressStopTravelTime < minimumTravelTime) {
											writeLine(person, tripIndex, trip, true, true, minimumTravelTime,
													accessEgressStopTravelTime, accessTime - walkOffset,
													egressTime - walkOffset);
										}
									}
								}

								tripIndex++;
							}
						}
					}

					progress.update();
				}
			}
		}
	}

	public void run(int numberOfThreads) throws InterruptedException {
		List<Thread> threads = new LinkedList<>();
		progress.start();

		for (int i = 0; i < numberOfThreads; i++) {
			threads.add(new Thread(new Worker()));
		}

		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();
	}
}
