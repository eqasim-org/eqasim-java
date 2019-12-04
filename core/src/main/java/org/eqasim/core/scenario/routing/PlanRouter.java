package org.eqasim.core.scenario.routing;

import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import com.google.inject.Inject;

public class PlanRouter {
	private final ActivityFacilities facilities;
	private final TripRouter tripRouter;
	private final StageActivityTypes stageActivityTypes;
	private final MainModeIdentifier mainModeIdentifier;

	@Inject
	public PlanRouter(ActivityFacilities facilities, TripRouter tripRouter) {
		this.tripRouter = tripRouter;
		this.facilities = facilities;

		this.stageActivityTypes = tripRouter.getStageActivityTypes();
		this.mainModeIdentifier = tripRouter.getMainModeIdentifier();
	}

	public void run(Plan plan, boolean replaceExistingRoutes, Set<String> modes) {
		List<Trip> trips = TripStructureUtils.getTrips(plan, stageActivityTypes);

		for (Trip trip : trips) {
			boolean legsExist = true;

			for (Leg leg : trip.getLegsOnly()) {
				if (leg.getRoute() == null) {
					legsExist = false;
				}
			}

			if (!legsExist || replaceExistingRoutes) {
				String mainMode = mainModeIdentifier.identifyMainMode(trip.getTripElements());

				if (modes.size() == 0 || modes.contains(mainMode)) {
					double departureTime = trip.getOriginActivity().getEndTime();

					if (Time.isUndefinedTime(departureTime)) {
						throw new IllegalStateException(
								"Found undefined activity end time for agent " + plan.getPerson().getId().toString());
					}

					Facility fromFacility = FacilitiesUtils.toFacility(trip.getOriginActivity(), facilities);
					Facility toFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), facilities);

					List<? extends PlanElement> newElements = tripRouter.calcRoute(mainMode, fromFacility, toFacility,
							departureTime, plan.getPerson());

					TripRouter.insertTrip(plan, trip.getOriginActivity(), newElements, trip.getDestinationActivity());
				}
			}
		}
	}
}
