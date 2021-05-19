package org.eqasim.examples.corsica_drt.mode_choice.utilities;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class DrtPredictor extends CachedVariablePredictor<DrtVariables> {
	private CostModel costModel;

	@Inject
	public DrtPredictor(@Named("drt") CostModel costModel) {
		this.costModel = costModel;
	}

	@Override
	public DrtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double travelTime_min = 0.0;
		double accessEgressTime_min = 0.0;
		double cost_MU = 0.0;
		double waitingTime_min = 0.0;

		for (Leg leg : TripStructureUtils.getLegs(elements)) {
			switch (leg.getMode()) {
			case TransportMode.walk:
				accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
				break;
			case "drt":
				DrtRoute route = (DrtRoute) leg.getRoute();

				// We use worst case here
				travelTime_min = route.getMaxTravelTime() / 60.0;
				waitingTime_min = route.getMaxWaitTime() / 60.0;

				cost_MU = costModel.calculateCost_MU(person, trip, elements);

				break;
			default:
				throw new IllegalStateException("Encountered unknown mode in DrtPredictor: " + leg.getMode());
			}
		}

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new DrtVariables(travelTime_min, cost_MU, euclideanDistance_km, waitingTime_min, accessEgressTime_min);
	}
}
