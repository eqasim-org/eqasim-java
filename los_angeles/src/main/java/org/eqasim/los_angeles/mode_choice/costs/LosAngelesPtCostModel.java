package org.eqasim.los_angeles.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.los_angeles.mode_choice.parameters.LosAngelesCostParameters;
import org.eqasim.los_angeles.mode_choice.utilities.predictors.LosAngelesPersonPredictor;
import org.eqasim.los_angeles.mode_choice.utilities.variables.LosAngelesPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class LosAngelesPtCostModel implements CostModel {
	private final LosAngelesPersonPredictor predictor;
	private final LosAngelesCostParameters parameters;

	@Inject
	public LosAngelesPtCostModel(LosAngelesCostParameters parameters, LosAngelesPersonPredictor predictor) {
		this.predictor = predictor;
		this.parameters = parameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		LosAngelesPersonVariables variables = predictor.predictVariables(person, trip, elements);

		if (variables.hasSubscription) {
			return 0.0;
		}
		double euclideanDistance_kfeet = PredictorUtils.calculateEuclideanDistance_km(trip);
		double euclideanDistance_km = euclideanDistance_kfeet / 3.28084;

		if (euclideanDistance_km < 20)
			return Math.ceil(euclideanDistance_km / 10.0) * parameters.ptCostPerTrip_USD_20km;
		else if (euclideanDistance_km < 40)
			return parameters.ptCostPerTrip_USD_20km * 2.0
					+ Math.ceil((euclideanDistance_km - 20) / 10.0) * parameters.ptCostPerTrip_USD_40km;
		else
			return parameters.ptCostPerTrip_USD_20km * 2.0 + parameters.ptCostPerTrip_USD_40km * 2.0
					+ Math.ceil((euclideanDistance_km - 40) / 10.0) * parameters.ptCostPerTrip_USD_40plus_km;
	}
}
