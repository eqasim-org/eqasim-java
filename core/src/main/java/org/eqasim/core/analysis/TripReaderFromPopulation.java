package org.eqasim.core.analysis;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class TripReaderFromPopulation {
	final private Collection<String> networkModes;
	final private MainModeIdentifier mainModeIdentifier;
	final private PersonAnalysisFilter personFilter;

	public TripReaderFromPopulation(Collection<String> networkModes, MainModeIdentifier mainModeIdentifier,
			PersonAnalysisFilter personFilter) {
		this.networkModes = networkModes;
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
				List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());

				int personTripIndex = 0;

				for (TripStructureUtils.Trip trip : trips) {
					boolean isHomeTrip = trip.getDestinationActivity().getType().equals("home");

					tripItems.add(new TripItem(person.getId(), personTripIndex, trip.getOriginActivity().getCoord(),
							trip.getDestinationActivity().getCoord(), trip.getOriginActivity().getEndTime().seconds(),
							trip.getDestinationActivity().getStartTime().seconds()
									- trip.getOriginActivity().getEndTime().seconds(),
							getVehicleDistance(trip), getRoutedDistance(trip),
							mainModeIdentifier.identifyMainMode(trip.getTripElements()),
							trip.getOriginActivity().getType(), trip.getDestinationActivity().getType(), isHomeTrip,
							CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
									trip.getDestinationActivity().getCoord())));

					personTripIndex++;
				}
			}
		}

		return tripItems;
	}

	private double getRoutedDistance(TripStructureUtils.Trip trip) {
		double vehicleDistance = 0.0;

		for (Leg leg : trip.getLegsOnly()) {
			if (leg.getRoute() == null) {
				vehicleDistance += Double.NaN;
			} else {
				vehicleDistance += leg.getRoute().getDistance();
			}
		}

		return vehicleDistance;
	}

	private double getVehicleDistance(TripStructureUtils.Trip trip) {
		double routedDistance = 0.0;

		for (Leg leg : trip.getLegsOnly()) {
			if (networkModes.contains(leg.getMode())) {
				if (leg.getRoute() == null) {
					routedDistance += Double.NaN;
				} else {
					routedDistance += leg.getRoute().getDistance();
				}
			}
		}

		return routedDistance;
	}
}
