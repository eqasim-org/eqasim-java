package org.sutlab.seville.mode_choice.costs;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.*;
import org.sutlab.seville.mode_choice.parameters.SevilleCostParameters;
import org.sutlab.seville.mode_choice.utilities.predictors.SevillePersonPredictor;
import org.sutlab.seville.mode_choice.utilities.variables.SevillePersonVariables;

import java.util.List;


public class SevillePtCostModel implements CostModel {
	private final TransitSchedule schedule;

	private final PersonPredictor generalPersonPredictor;
	private final SevillePersonPredictor personPredictor;
	private final SevilleCostParameters costParameters;

	@Inject
	public SevillePtCostModel(SevilleCostParameters costParameters, TransitSchedule schedule,
                              SevillePersonPredictor personPredictor, PersonPredictor generalPersonPredictor) {
		this.schedule = schedule;
		this.personPredictor = personPredictor;
		this.generalPersonPredictor = generalPersonPredictor;
		this.costParameters = costParameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		PersonVariables generalPersonVariables = generalPersonPredictor.predictVariables(person, trip, elements);
		SevillePersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		if (generalPersonVariables.age_a < 5) {
			return 0.0;
		}

		if (personVariables.hasSubscription) {
			return 0.0;
		}

		//bus prices
		//city buses for tussam are free for young people and elderly https://www.tussam.es/es/titulos-de-viajes
		for (Leg leg : TripStructureUtils.getLegs(elements)) {
			if (leg.getRoute() instanceof TransitPassengerRoute route) {
				TransitLine transitLine = schedule.getTransitLines().get(route.getLineId());
				TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());

				if (transitRoute.getTransportMode().equals("tram")
						|| transitRoute.getTransportMode().equals("bus")
						||transitRoute.getTransportMode().equals("Tram")
						|| transitRoute.getTransportMode().equals("Bus")) {
					if (generalPersonVariables.age_a <= 14 || generalPersonVariables.age_a >= 65 ) {
						return 0.0;
					}
					return costParameters.busCost_EUR;
				}
				if (transitRoute.getTransportMode().equals("subway")
						|| transitRoute.getTransportMode().equals("Subway")){
					return costParameters.metroCost_EUR;
				}
			}
		}

		return costParameters.ptCost_EUR;


	}

}