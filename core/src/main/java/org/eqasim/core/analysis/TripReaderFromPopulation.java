package org.eqasim.core.analysis;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class TripReaderFromPopulation {
	final private Network network;
	final private StageActivityTypes stageActivityTypes;
	final private MainModeIdentifier mainModeIdentifier;
	final private PersonAnalysisFilter personFilter;

	public TripReaderFromPopulation(Network network, StageActivityTypes stageActivityTypes,
			MainModeIdentifier mainModeIdentifier, PersonAnalysisFilter personFilter) {
		this.network = network;
		this.stageActivityTypes = stageActivityTypes;
		this.mainModeIdentifier = mainModeIdentifier;
		this.personFilter = personFilter;
	}

	public Collection<TripItem> readTrips(String populationPath) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(populationPath);
		return readTrips(scenario.getPopulation());
	}

	public Collection<TripItem> readTrips(Population population) {
		List<TripItem> tripItems = new LinkedList<>();

		for (Person person : population.getPersons().values()) {
			if (personFilter.analyzePerson(person.getId())) {
				List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan(),
						stageActivityTypes);

				int personTripIndex = 0;

				for (TripStructureUtils.Trip trip : trips) {
					boolean isHomeTrip = trip.getDestinationActivity().getType().equals("home");

					tripItems.add(new TripItem(person.getId(), personTripIndex, trip.getOriginActivity().getCoord(),
							trip.getDestinationActivity().getCoord(), trip.getOriginActivity().getEndTime(),
							trip.getDestinationActivity().getStartTime() - trip.getOriginActivity().getEndTime(),
							getNetworkDistance(trip), mainModeIdentifier.identifyMainMode(trip.getTripElements()),
							trip.getOriginActivity().getType(), trip.getDestinationActivity().getType(), isHomeTrip,
							CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
									trip.getDestinationActivity().getCoord())));

					personTripIndex++;
				}
			}
		}

		return tripItems;
	}

	private double getNetworkDistance(TripStructureUtils.Trip trip) {
		if (mainModeIdentifier.identifyMainMode(trip.getTripElements()).equals("car")) {
			NetworkRoute route = (NetworkRoute) trip.getLegsOnly().get(0).getRoute();
			double distance = 0.0;

			if (route != null) {
				for (Id<Link> linkId : route.getLinkIds()) {
					distance += network.getLinks().get(linkId).getLength();
				}
			}

			return distance;
		} else {
			double distance = 0.0;

			for (Leg leg : trip.getLegsOnly()) {
				if (leg.getRoute() == null) {
					return Double.NaN;
				} else {
					distance += leg.getRoute().getDistance();
				}
			}

			return distance;
		}
	}
}
