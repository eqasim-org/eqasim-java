package org.eqasim.automated_vehicles.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.automated_vehicles.mode_choice.utilities.variables.AvVariables;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.name.Named;

import ch.ethz.matsim.av.routing.AVRoute;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AvPredictor extends CachedVariablePredictor<AvVariables> {
	private CostModel costModel;

	public AvPredictor(@Named("av") CostModel costModel) {
		this.costModel = costModel;
	}

	@Override
	public AvVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		if (elements.size() > 1) {
			throw new IllegalStateException("We do not support multi-stage AV trips yet.");
		}

		Leg leg = (Leg) elements.get(0);
		AVRoute route = (AVRoute) leg.getRoute();

		double travelTime_min = leg.getTravelTime() / 60.0;
		double waitingTime_min = route.getWaitingTime();
		double cost_MU = costModel.calculateCost_MU(person, trip, elements);

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return checkNaN(new AvVariables(travelTime_min, cost_MU, euclideanDistance_km, waitingTime_min));
	}

	public AvVariables checkNaN(AvVariables variables) {
		if (Double.isNaN(variables.travelTime_min) || Double.isNaN(variables.cost_MU)
				|| Double.isNaN(variables.waitingTime_min)) {
			throw new IllegalStateException(
					"NaN values encountered in AVVariables. Is the AV extension set up properly?");
		}

		return variables;
	}
}
