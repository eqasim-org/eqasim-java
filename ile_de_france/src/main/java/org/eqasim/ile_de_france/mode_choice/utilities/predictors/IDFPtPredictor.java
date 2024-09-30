package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPtVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

public class IDFPtPredictor extends CachedVariablePredictor<IDFPtVariables> {
	private final TransitSchedule schedule;

	@Inject
	public IDFPtPredictor(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	@Override
	public IDFPtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		int busCount = 0;
		int subwayCount = 0;
		int otherCount = 0;

		for (PlanElement element : elements) {
			if (element instanceof Leg leg) {
				if (leg instanceof TransitPassengerRoute route) {
					TransitLine transitLine = schedule.getTransitLines().get(route.getLineId());
					TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());
					String transportMode = transitRoute.getTransportMode();

					if (transportMode.equals("bus")) {
						busCount++;
					} else if (transportMode.equals("subway")) {
						subwayCount++;
					}
				}
			}
		}

		boolean isOnlyBus = busCount > 0 && subwayCount == 0 && otherCount == 0;
		boolean hasOnlySubwayAndBus = (busCount > 0 || subwayCount > 0) && otherCount == 0;

		return new IDFPtVariables(isOnlyBus, hasOnlySubwayAndBus);
	}
}