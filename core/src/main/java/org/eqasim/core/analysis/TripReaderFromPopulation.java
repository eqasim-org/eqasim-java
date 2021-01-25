package org.eqasim.core.analysis;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
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
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class TripReaderFromPopulation {
	final private Collection<String> networkModes;
	final private MainModeIdentifier mainModeIdentifier;
	final private PersonAnalysisFilter personFilter;
	final private Optional<Network> network;
	final private Optional<ActivityFacilities> facilities;

	public TripReaderFromPopulation(Collection<String> networkModes, MainModeIdentifier mainModeIdentifier,
			PersonAnalysisFilter personFilter, Optional<Network> network, Optional<ActivityFacilities> facilities) {
		this.networkModes = networkModes;
		this.mainModeIdentifier = mainModeIdentifier;
		this.personFilter = personFilter;
		this.network = network;
		this.facilities = facilities;
	}

	public Collection<TripItem> readTrips(String populationPath) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(populationPath);
		return readTrips(scenario.getPopulation());
	}

	private Coord getCoordinate(Id<Person> personId, Activity activity) {
		Id<ActivityFacility> facilityId = activity.getFacilityId();
		Id<Link> linkId = activity.getLinkId();
		Coord coord = activity.getCoord();

		if (facilityId != null && facilities.isPresent()) {
			ActivityFacility facility = facilities.get().getFacilities().get(facilityId);

			if (facility == null) {
				throw new IllegalStateException("Could not find facility: " + facilityId);
			} else {
				coord = facility.getCoord();
				linkId = facility.getLinkId();
			}
		}

		if (linkId != null && network.isPresent()) {
			Link link = network.get().getLinks().get(linkId);

			if (link == null) {
				throw new IllegalStateException("Could not find link: " + linkId);
			} else {
				coord = link.getCoord();
			}
		}

		if (coord == null) {
			throw new IllegalStateException("Could not find coordinate for activity of " + personId);
		}

		return coord;
	}

	public Collection<TripItem> readTrips(Population population) {
		List<TripItem> tripItems = new LinkedList<>();

		for (Person person : population.getPersons().values()) {
			if (personFilter.analyzePerson(person.getId())) {
				List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());

				int personTripIndex = 0;

				for (TripStructureUtils.Trip trip : trips) {
					boolean isHomeTrip = trip.getDestinationActivity().getType().equals("home");

					Coord originCoord = getCoordinate(person.getId(), trip.getOriginActivity());
					Coord destinationCoord = getCoordinate(person.getId(), trip.getDestinationActivity());

					tripItems.add(new TripItem(person.getId(), personTripIndex, originCoord, destinationCoord,
							trip.getOriginActivity().getEndTime().seconds(),
							trip.getDestinationActivity().getStartTime().seconds()
									- trip.getOriginActivity().getEndTime().seconds(),
							getVehicleDistance(trip), getRoutedDistance(trip),
							mainModeIdentifier.identifyMainMode(trip.getTripElements()),
							trip.getOriginActivity().getType(), trip.getDestinationActivity().getType(), isHomeTrip,
							CoordUtils.calcEuclideanDistance(originCoord, destinationCoord)));

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
