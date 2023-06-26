package org.eqasim.examples.corsica_carpooling.mode_choice.utilities;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class CarpoolingPredictor extends CachedVariablePredictor<CarpoolingVariables> {
	private CostModel costModel;

	@Inject
	public CarpoolingPredictor(@Named("carpooling") CostModel costModel) {
		this.costModel = costModel;
	}

	@Override
	public CarpoolingVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		if (elements.size() > 1) {
			throw new IllegalStateException("We do not support multi-stage car trips yet.");
		}

		Leg leg = (Leg) elements.get(0);

		double travelTime_min = leg.getTravelTime().seconds() / 60.0;
		double cost_MU = costModel.calculateCost_MU(person, trip, elements);

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new CarpoolingVariables(travelTime_min, cost_MU, euclideanDistance_km);
	}
}
