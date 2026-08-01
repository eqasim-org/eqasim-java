package ch.sbb.matsim.routing.pt.raptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eqasim.switzerland.ch_cmdp.config.SwissIntermodalAccessEgressConfigGroup;
import org.eqasim.switzerland.ch_cmdp.routing.IntermodalVehicleRoutingAttributes;
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
	private final RaptorStopFinder delegate;
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

	SwissHomeActivityRaptorStopFinder(RaptorStopFinder delegate, SwissIntermodalAccessEgressConfigGroup config) {
		this.delegate = delegate;
		this.config = config;
	}

	@Override
	public List<InitialStop> findStops(Facility fromFacility, Facility toFacility, Person person, double departureTime,
			Attributes routingAttributes, RaptorParameters parameters, SwissRailRaptorData data, Direction type) {
		List<InitialStop> stops = delegate.findStops(fromFacility, toFacility, person, departureTime, routingAttributes,
				parameters, data, type);
		// First apply hard requirements coming from DMC vehicle continuity. If a
		// bike/car is parked at a specific PT stop, only that mode and stop remain
		// feasible for the matching access or egress side.
		stops = filterIntermodalVehicleRequirements(stops, routingAttributes, type);

		if (!config.restrictVehicleToHomeActivity()) {
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

	private List<InitialStop> filterIntermodalVehicleRequirements(List<InitialStop> stops, Attributes routingAttributes,
			Direction type) {
		// The estimator writes these attributes only on candidate branches where a
		// private vehicle must be retrieved or placed at a particular PT stop.
		String requiredMode = getRequiredMode(routingAttributes, type);
		String requiredStopId = getRequiredStopId(routingAttributes, type);
		Set<String> forbiddenModes = getForbiddenModes(routingAttributes, type);

		if (requiredMode == null && requiredStopId == null && forbiddenModes.isEmpty()) {
			return stops;
		}

		return stops.stream().filter(stop -> matchesRequiredStop(stop, requiredStopId))
				.filter(stop -> matchesRequiredMode(stop, requiredMode))
				.filter(stop -> !usesAnyMode(stop, forbiddenModes)).collect(Collectors.toList());
	}

	private String getRequiredMode(Attributes routingAttributes, Direction type) {
		if (routingAttributes == null) {
			return null;
		}

		String attribute = type == Direction.ACCESS ? IntermodalVehicleRoutingAttributes.REQUIRED_ACCESS_MODE
				: IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_MODE;
		Object value = routingAttributes.getAttribute(attribute);
		return value == null ? null : value.toString();
	}

	private String getRequiredStopId(Attributes routingAttributes, Direction type) {
		if (routingAttributes == null) {
			return null;
		}

		String attribute = type == Direction.ACCESS ? IntermodalVehicleRoutingAttributes.REQUIRED_ACCESS_STOP_ID
				: IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_STOP_ID;
		Object value = routingAttributes.getAttribute(attribute);
		return value == null ? null : value.toString();
	}

	private Set<String> getForbiddenModes(Attributes routingAttributes, Direction type) {
		if (routingAttributes == null) {
			return Collections.emptySet();
		}

		String attribute = type == Direction.ACCESS ? IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE
				: IntermodalVehicleRoutingAttributes.FORBIDDEN_EGRESS_MODE;
		Object value = routingAttributes.getAttribute(attribute);
		return parseModes(value);
	}

	private boolean matchesRequiredStop(InitialStop stop, String requiredStopId) {
		return requiredStopId == null || stop.stop.getId().toString().equals(requiredStopId);
	}

	private boolean matchesRequiredMode(InitialStop stop, String requiredMode) {
		return requiredMode == null || usesMode(stop, requiredMode);
	}

	private boolean usesAnyMode(InitialStop stop, Set<String> modes) {
		for (String mode : modes) {
			if (usesMode(stop, mode)) {
				return true;
			}
		}

		return false;
	}

	private boolean usesMode(InitialStop stop, String mode) {
		if (mode == null) {
			return false;
		}

		// Pure walk access/egress candidates have stop.mode set; intermodal
		// candidates created from routed access/egress legs keep their mode in the
		// leg list instead.
		if (mode.equals(stop.mode)) {
			return true;
		}

		if (stop.planElements == null) {
			return false;
		}

		for (PlanElement element : stop.planElements) {
			if (element instanceof Leg leg && mode.equals(leg.getMode())) {
				return true;
			}
		}

		return false;
	}

	private boolean isRestrictedActivity(Optional<Activity> activity) {
		return activity.map(Activity::getType).filter(config.getVehicleRestrictedActivityType()::equals).isPresent();
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
		Set<String> restrictedModes = config.getRestrictedIntermodalAccessEgressModes();
		if (restrictedModes.contains(stop.mode)) {
			return true;
		}

		if (stop.planElements == null) {
			return false;
		}

		for (PlanElement element : stop.planElements) {
			if (element instanceof Leg leg && restrictedModes.contains(leg.getMode())) {
				return true;
			}
		}

		return false;
	}

	static private Set<String> parseModes(Object value) {
		if (value == null || value.toString().isBlank()) {
			return Collections.emptySet();
		}

		return Arrays.stream(value.toString().split(",")) //
				.map(String::trim) //
				.filter(mode -> !mode.isEmpty()) //
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
