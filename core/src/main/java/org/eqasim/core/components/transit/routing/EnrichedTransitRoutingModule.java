package org.eqasim.core.components.transit.routing;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * TODO: Same as for DefaultEnrichedTransitRouter: This is very focused on "pt"
 * legs, but other modes may be possible.
 */
public class EnrichedTransitRoutingModule implements RoutingModule {
	final private EnrichedTransitRouter transitRouter;
	final private TransitSchedule transitSchedule;
	final private RoutingModule walkRouter;

	@Inject
	public EnrichedTransitRoutingModule(EnrichedTransitRouter transitRouter, TransitSchedule transitSchedule,
			@Named("walk") RoutingModule walkRouter) {
		this.transitRouter = transitRouter;
		this.transitSchedule = transitSchedule;
		this.walkRouter = walkRouter;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		List<Leg> legs = transitRouter.calculateRoute(fromFacility, toFacility, departureTime, person);

		if (legs == null) {
			return walkRouter.calcRoute(fromFacility, toFacility, departureTime, person);
		}

		List<PlanElement> trip = new LinkedList<>();

		for (int i = 0; i < legs.size() - 1; i++) {
			Facility currentFacility = null;
			trip.add(legs.get(i));

			if (legs.get(i).getMode().equals(TransportMode.pt)) {
				EnrichedTransitRoute route = (EnrichedTransitRoute) legs.get(i).getRoute();

				currentFacility = transitSchedule.getTransitLines().get(route.getTransitLineId()).getRoutes()
						.get(route.getTransitRouteId()).getStops().get(route.getEgressStopIndex()).getStopFacility();
			} else {
				EnrichedTransitRoute route = (EnrichedTransitRoute) legs.get(i + 1).getRoute();

				currentFacility = transitSchedule.getTransitLines().get(route.getTransitLineId()).getRoutes()
						.get(route.getTransitRouteId()).getStops().get(route.getAccessStopIndex()).getStopFacility();
			}

			Activity activity = PopulationUtils.createActivityFromCoordAndLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE,
					currentFacility.getCoord(), currentFacility.getLinkId());

			activity.setMaximumDuration(0.0);
			trip.add(activity);
		}

		trip.add(legs.get(legs.size() - 1));

		return trip;
	}
}
