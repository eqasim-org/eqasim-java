package org.eqasim.switzerland.zurich.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.switzerland.ch.ovgk.OVGK;
import org.eqasim.switzerland.ch.ovgk.OVGKCalculator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichPtVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;


public class ZurichPtPredictor extends CachedVariablePredictor<ZurichPtVariables> {
	public final PtPredictor delegate;
	private final TransitSchedule schedule;
	private final OVGKCalculator ovgkCalculator;

	@Inject
	public ZurichPtPredictor(PtPredictor delegate, TransitSchedule schedule, OVGKCalculator ovgkCalculator) {
		this.delegate = delegate;
		this.schedule = schedule;
		this.ovgkCalculator = ovgkCalculator;
	}

	@Override
	protected ZurichPtVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		PtVariables delegateVariables = delegate.predictVariables(person, trip, elements);

		double railTravelTime_min = 0.0;
		double busTravelTime_min = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().equals(TransportMode.pt)) {
					TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
					TransitRoute transitRoute = schedule.getTransitLines().get(route.getLineId()).getRoutes()
							.get(route.getRouteId());

					if (transitRoute.getTransportMode().equals("rail")) {
						railTravelTime_min += route.getTravelTime().seconds() / 60.0;
					} else {
						busTravelTime_min += route.getTravelTime().seconds() / 60.0;
					}
				}
			}
		}

		Double headwayRaw = (Double) trip.getOriginActivity().getAttributes().getAttribute("headway_min");
		double headway_min = headwayRaw == null ? 0.0 : headwayRaw;

		OVGK originOvgk = ovgkCalculator.calculateOVGK(trip.getOriginActivity().getCoord());
		OVGK destinationOvgk = ovgkCalculator.calculateOVGK(trip.getDestinationActivity().getCoord());
		OVGK worstOvgk = originOvgk.ordinal() > destinationOvgk.ordinal() ? originOvgk : destinationOvgk;

		return new ZurichPtVariables(delegateVariables, railTravelTime_min, busTravelTime_min, headway_min, worstOvgk);
	}
}