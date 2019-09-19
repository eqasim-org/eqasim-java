package org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.components.transit.routing.EnrichedTransitRoute;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.DAPtVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class DAPtPredictor extends CachedVariablePredictor<DAPtVariables> {
	public final PtPredictor delegate;
	private final TransitSchedule schedule;

	@Inject
	public DAPtPredictor(PtPredictor delegate, TransitSchedule schedule) {
		this.delegate = delegate;
		this.schedule = schedule;
	}

	@Override
	protected DAPtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		PtVariables delegateVariables = delegate.predictVariables(person, trip, elements);

		double railTravelTime_min = 0.0;
		double busTravelTime_min = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().equals(TransportMode.pt)) {
					EnrichedTransitRoute route = (EnrichedTransitRoute) leg.getRoute();
					TransitRoute transitRoute = schedule.getTransitLines().get(route.getTransitLineId()).getRoutes()
							.get(route.getTransitRouteId());

					if (transitRoute.getTransportMode().equals("rail")) {
						railTravelTime_min += route.getInVehicleTime() / 60.0;
					} else {
						busTravelTime_min += route.getInVehicleTime() / 60.0;
					}
				}
			}
		}

		Double headwayRaw = (Double) trip.getOriginActivity().getAttributes().getAttribute("headway");
		double headway_min = headwayRaw == null ? 0.0 : headwayRaw;

		return new DAPtVariables(delegateVariables, railTravelTime_min, busTravelTime_min, headway_min);
	}
}
