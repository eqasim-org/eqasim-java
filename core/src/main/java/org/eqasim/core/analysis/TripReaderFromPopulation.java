package org.eqasim.core.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TripReaderFromPopulation {
	final private Network network;
	final private StageActivityTypes stageActivityTypes;
	final private MainModeIdentifier mainModeIdentifier;
	final private PersonAnalysisFilter personFilter;
	private Collection<TripItem> tripItems = new LinkedList<>();
	private Collection<LegItem> legItems = new LinkedList<>();

	public TripReaderFromPopulation(Network network, StageActivityTypes stageActivityTypes,
									MainModeIdentifier mainModeIdentifier, PersonAnalysisFilter personFilter) {
		this.network = network;
		this.stageActivityTypes = stageActivityTypes;
		this.mainModeIdentifier = mainModeIdentifier;
		this.personFilter = personFilter;
	}

	public Collection<TripItem> getTripItems() {
		return tripItems;
	}

	public Collection<LegItem> getLegItems() {
		return legItems;
	}

	public void read(String populationPath) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(populationPath);
		read(scenario.getPopulation());
	}

	public void read(Population population) {
		for (Person person : population.getPersons().values()) {
			if (personFilter.analyzePerson(person.getId())) {
				List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan(),
						stageActivityTypes);

				int personTripIndex = 0;

				for (TripStructureUtils.Trip trip : trips) {

					// process trip
					boolean isHomeTrip = trip.getDestinationActivity().getType().equals("home");

					// TODO: fix the travel times to reflect the actual travel time and not the plan
					tripItems.add(new TripItem(person.getId(), personTripIndex, trip.getOriginActivity().getCoord(),
							trip.getDestinationActivity().getCoord(), trip.getOriginActivity().getEndTime(),
							trip.getDestinationActivity().getStartTime() - trip.getOriginActivity().getEndTime(),
							getNetworkDistance(trip), mainModeIdentifier.identifyMainMode(trip.getTripElements()),
							trip.getOriginActivity().getType(), trip.getDestinationActivity().getType(), isHomeTrip,
							CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
									trip.getDestinationActivity().getCoord())));

					// process trip legs
					Activity preceedingActivity = trip.getOriginActivity();
					LegItem legItem = null;
					int tripLegIndex = 0;

					for (PlanElement element : trip.getTripElements()) {
						if (element instanceof Leg) {
							Leg leg = (Leg) element;

							legItem = new LegItem(person.getId(), personTripIndex, tripLegIndex,
									preceedingActivity.getCoord(), null,
									leg.getDepartureTime(), leg.getTravelTime(),
									getNetworkDistance(leg), leg.getMode(), preceedingActivity.getType(),
									null, false, 0.0);

						} else if (element instanceof Activity) {
							if (legItem != null) {
								Activity followingActivity = (Activity) element;
								legItem.destination = followingActivity.getCoord();
								legItem.followingPurpose = followingActivity.getType();
								legItem.returning = followingActivity.getType().equals("home");
								legItem.crowflyDistance = CoordUtils.calcEuclideanDistance(preceedingActivity.getCoord(),
										followingActivity.getCoord());

								legItems.add(legItem);

								preceedingActivity = followingActivity;
								tripLegIndex++;
							}
						}
					}

					// finish last leg
					if (legItem != null) {
						Activity followingActivity = trip.getDestinationActivity();
						legItem.destination = followingActivity.getCoord();
						legItem.followingPurpose = followingActivity.getType();
						legItem.returning = followingActivity.getType().equals("home");
						legItem.crowflyDistance = CoordUtils.calcEuclideanDistance(preceedingActivity.getCoord(),
								followingActivity.getCoord());

						legItems.add(legItem);
					}

					personTripIndex++;
				}
			}
		}
	}

	private double getNetworkDistance(TripStructureUtils.Trip trip) {

		double distance = 0.0;

		for (Leg leg : trip.getLegsOnly()) {
			if (leg.getRoute() != null) {
				if (leg.getRoute() instanceof NetworkRoute) {
					NetworkRoute route = (NetworkRoute) leg.getRoute();
					if (route != null) {
						for (Id<Link> linkId : route.getLinkIds()) {
							distance += network.getLinks().get(linkId).getLength();
						}
					}
				} else {
					distance += leg.getRoute().getDistance();
				}
			}
		}

		return distance;

	}

	private double getNetworkDistance(Leg leg) {

		double distance = 0.0;

		if (leg.getRoute() == null) {
			return Double.NaN;
		} else {
			distance += leg.getRoute().getDistance();
		}
		return distance;

	}
}
