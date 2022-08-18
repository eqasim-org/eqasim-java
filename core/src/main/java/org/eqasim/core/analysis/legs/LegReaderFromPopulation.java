package org.eqasim.core.analysis.legs;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class LegReaderFromPopulation {
	final private Collection<String> networkModes;
	final private PersonAnalysisFilter personFilter;
	final private Optional<Network> network;
	final private Optional<ActivityFacilities> facilities;

	public LegReaderFromPopulation(Collection<String> networkModes, PersonAnalysisFilter personFilter,
			Optional<Network> network, Optional<ActivityFacilities> facilities) {
		this.networkModes = networkModes;
		this.personFilter = personFilter;
		this.network = network;
		this.facilities = facilities;
	}

	public Collection<LegItem> readLegs(String populationPath) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(populationPath);
		return readLegs(scenario.getPopulation());
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

	public Collection<LegItem> readLegs(Population population) {
		List<LegItem> legItems = new LinkedList<>();

		for (Person person : population.getPersons().values()) {
			if (personFilter.analyzePerson(person.getId())) {
				List<? extends PlanElement> elements = person.getSelectedPlan().getPlanElements();
				List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());

				int personTripIndex = 0;
				int legIndex = 0;

				for (TripStructureUtils.Trip trip : trips) {
					for (Leg leg : trip.getLegsOnly()) {
						int i = elements.indexOf(leg);
						Activity precedingActivity = (Activity) elements.get(i - 1);
						Activity followingActivity = (Activity) elements.get(i + 1);

						Coord originCoord = getCoordinate(person.getId(), precedingActivity);
						Coord destinationCoord = getCoordinate(person.getId(), followingActivity);

						double departureTime = leg.getDepartureTime().orElse(Double.NaN);
						double duration = leg.getTravelTime().orElse(Double.NaN);

						legItems.add(new LegItem(person.getId(), personTripIndex, legIndex, originCoord,
								destinationCoord, departureTime, duration, getVehicleDistance(leg),
								getRoutedDistance(leg), leg.getMode(),
								CoordUtils.calcEuclideanDistance(originCoord, destinationCoord)));

						legIndex++;
					}

					personTripIndex++;
				}
			}
		}

		return legItems;
	}

	private double getRoutedDistance(Leg leg) {
		if (leg.getRoute() != null) {
			return leg.getRoute().getDistance();
		}

		return Double.NaN;
	}

	private double getVehicleDistance(Leg leg) {
		if (networkModes.contains(leg.getMode())) {
			if (leg.getRoute() != null) {
				return leg.getRoute().getDistance();
			} else {
				return Double.NaN;
			}
		}

		return 0.0;
	}
}
