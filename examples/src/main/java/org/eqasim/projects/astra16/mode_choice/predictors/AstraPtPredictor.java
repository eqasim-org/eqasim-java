package org.eqasim.projects.astra16.mode_choice.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.projects.astra16.mode_choice.variables.AstraPtVariables;
import org.eqasim.switzerland.ovgk.OVGK;
import org.eqasim.switzerland.ovgk.OVGKCalculator;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

public class AstraPtPredictor extends CachedVariablePredictor<AstraPtVariables> {
	public final PtPredictor delegate;
	private final TransitSchedule schedule;
	private final OVGKCalculator ovgkCalculator;

	@Inject
	public AstraPtPredictor(PtPredictor delegate, TransitSchedule schedule, OVGKCalculator ovgkCalculator) {
		this.delegate = delegate;
		this.schedule = schedule;
		this.ovgkCalculator = ovgkCalculator;
	}

	@Override
	protected AstraPtVariables predict(Person person, DiscreteModeChoiceTrip trip,
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

					double waitingTime_s = route.getBoardingTime().seconds() - leg.getDepartureTime().seconds();
					double inVehicleTime_s = route.getTravelTime().seconds() - waitingTime_s;

					if (transitRoute.getTransportMode().equals("rail")) {
						railTravelTime_min += inVehicleTime_s / 60.0;
					} else {
						busTravelTime_min += inVehicleTime_s / 60.0;
					}
				}
			}
		}

		Double headwayRaw = (Double) trip.getOriginActivity().getAttributes().getAttribute("headway_min");
		double headway_min = headwayRaw == null ? 0.0 : headwayRaw;

		OVGK originOvgk = ovgkCalculator.calculateOVGK(trip.getOriginActivity().getCoord());
		OVGK destinatonOvgk = ovgkCalculator.calculateOVGK(trip.getDestinationActivity().getCoord());
		OVGK worstOvgk = OVGK.worst(originOvgk, destinatonOvgk);

		return new AstraPtVariables(delegateVariables, railTravelTime_min, busTravelTime_min, headway_min, worstOvgk);
	}
}
