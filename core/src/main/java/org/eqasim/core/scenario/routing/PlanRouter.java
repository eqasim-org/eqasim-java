package org.eqasim.core.scenario.routing;

import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.google.inject.Inject;

public class PlanRouter {
	private final ActivityFacilities facilities;
	private final TripRouter tripRouter;

	@Inject
	public PlanRouter(ActivityFacilities facilities, TripRouter tripRouter) {
		this.tripRouter = tripRouter;
		this.facilities = facilities;
	}

	public void run(Plan plan, boolean replaceExistingRoutes, Set<String> modes) {
		List<Trip> trips = TripStructureUtils.getTrips(plan);

		for (Trip trip : trips) {
			boolean legsExist = true;

			for (Leg leg : trip.getLegsOnly()) {
				if (leg.getRoute() == null) {
					legsExist = false;
				}
			}

			if (!legsExist || replaceExistingRoutes) {
				String mainMode = TripStructureUtils.identifyMainMode(trip.getTripElements());

				if (modes.size() == 0 || modes.contains(mainMode)) {
					OptionalTime departureTime = trip.getOriginActivity().getEndTime();

					if (departureTime.isUndefined()) {
						throw new IllegalStateException(
								"Found undefined activity end time for agent " + plan.getPerson().getId().toString());
					}

					Facility fromFacility = FacilitiesUtils.toFacility(trip.getOriginActivity(), facilities);
					Facility toFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), facilities);

					List<? extends PlanElement> newElements = tripRouter.calcRoute(mainMode, fromFacility, toFacility,
							departureTime.seconds(), plan.getPerson(), trip.getTripAttributes());

					// Fix in case we have a transit trip that is only walk
					newElements = fixOnlyWalk(mainMode, fromFacility, toFacility, departureTime.seconds(),
							plan.getPerson(), newElements, trip.getTripAttributes());

					TripRouter.insertTrip(plan, trip.getOriginActivity(), newElements, trip.getDestinationActivity());
				}
			}
		}
	}

	private List<? extends PlanElement> fixOnlyWalk(String mainMode, Facility fromFacility, Facility toFacility,
			double departureTime, Person person, List<? extends PlanElement> elements, Attributes attributes) {
		// No need to fix if already walk
		if (mainMode.equals(TransportMode.walk)) {
			return elements;
		}

		// No need to fix if we have a non-walk leg
		for (Leg leg : TripStructureUtils.getLegs(elements)) {
			if (!leg.getMode().equals(TransportMode.walk)) {
				return elements;
			}
		}

		// We have only walk legs
		return tripRouter.calcRoute(TransportMode.walk, fromFacility, toFacility, departureTime, person, attributes);
	}
}
