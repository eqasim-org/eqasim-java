package ch.sbb.matsim.routing.pt.raptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eqasim.switzerland.ch_cmdp.config.SwissIntermodalAccessEgressConfigGroup;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class SwissHomeActivityRaptorStopFinder implements RaptorStopFinder {
	private final DefaultRaptorStopFinder delegate;
	private final SwissIntermodalAccessEgressConfigGroup config;

	@Inject
	public SwissHomeActivityRaptorStopFinder(Config config, RaptorIntermodalAccessEgress intermodalAE,
			Map<String, Provider<RoutingModule>> routingModuleProviders) {
		this.delegate = new DefaultRaptorStopFinder(config, intermodalAE, routingModuleProviders);
		this.config = ConfigUtils.addOrGetModule(config, SwissIntermodalAccessEgressConfigGroup.class);
	}

	public SwissHomeActivityRaptorStopFinder(DefaultRaptorStopFinder delegate,
			SwissIntermodalAccessEgressConfigGroup config) {
		this.delegate = delegate;
		this.config = config;
	}

	@Override
	public List<InitialStop> findStops(Facility fromFacility, Facility toFacility, Person person, double departureTime,
			Attributes routingAttributes, RaptorParameters parameters, SwissRailRaptorData data, Direction type) {
		List<InitialStop> stops = delegate.findStops(fromFacility, toFacility, person, departureTime, routingAttributes,
				parameters, data, type);

		if (!config.restrictBikeToHomeActivity()) {
			return stops;
		}

		if (type == Direction.ACCESS && isRestrictedActivity(findOriginActivity(person, fromFacility, departureTime))) {
			return stops;
		}

		if (type == Direction.EGRESS && isRestrictedActivity(findDestinationActivity(person, toFacility, departureTime))) {
			return stops;
		}

		return stops.stream().filter(Predicate.not(this::usesRestrictedMode)).collect(Collectors.toList());
	}

	private boolean isRestrictedActivity(Optional<Activity> activity) {
		return activity.map(Activity::getType).filter(config.getBikeRestrictedActivityType()::equals).isPresent();
	}

	private Optional<Activity> findOriginActivity(Person person, Facility fromFacility, double departureTime) {
		return findTrip(person, fromFacility, null, departureTime).map(Trip::getOriginActivity);
	}

	private Optional<Activity> findDestinationActivity(Person person, Facility toFacility, double departureTime) {
		return findTrip(person, null, toFacility, departureTime).map(Trip::getDestinationActivity);
	}

	private Optional<Trip> findTrip(Person person, Facility fromFacility, Facility toFacility, double departureTime) {
		if (person == null || person.getSelectedPlan() == null) {
			return Optional.empty();
		}

		for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
			if (fromFacility != null && !sameFacility(trip.getOriginActivity(), fromFacility)) {
				continue;
			}

			if (toFacility != null && !sameFacility(trip.getDestinationActivity(), toFacility)) {
				continue;
			}

			if (trip.getOriginActivity().getEndTime().isDefined()
					&& Math.abs(trip.getOriginActivity().getEndTime().seconds() - departureTime) > 1e-6) {
				continue;
			}

			return Optional.of(trip);
		}

		return Optional.empty();
	}

	private boolean sameFacility(Activity activity, Facility facility) {
		if (activity.getFacilityId() != null && facility instanceof ActivityFacility activityFacility
				&& activity.getFacilityId().equals(activityFacility.getId())) {
			return true;
		}

		if (activity.getLinkId() != null && facility.getLinkId() != null && activity.getLinkId().equals(facility.getLinkId())) {
			return true;
		}

		return activity.getCoord() != null && facility.getCoord() != null
				&& activity.getCoord().getX() == facility.getCoord().getX()
				&& activity.getCoord().getY() == facility.getCoord().getY();
	}

	private boolean usesRestrictedMode(InitialStop stop) {
		if (config.getBikeRestrictedMode().equals(stop.mode)) {
			return true;
		}

		if (stop.planElements == null) {
			return false;
		}

		for (PlanElement element : stop.planElements) {
			if (element instanceof Leg leg && config.getBikeRestrictedMode().equals(leg.getMode())) {
				return true;
			}
		}

		return false;
	}
}
