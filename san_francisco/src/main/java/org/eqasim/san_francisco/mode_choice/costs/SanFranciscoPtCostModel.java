package org.eqasim.san_francisco.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.san_francisco.mode_choice.parameters.SanFranciscoCostParameters;
import org.eqasim.san_francisco.mode_choice.utilities.predictors.SanFranciscoPersonPredictor;
import org.eqasim.san_francisco.mode_choice.utilities.variables.SanFranciscoPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SanFranciscoPtCostModel implements CostModel {
	private final SanFranciscoPersonPredictor predictor;
	private final SanFranciscoCostParameters parameters;

	@Inject
	public SanFranciscoPtCostModel(SanFranciscoCostParameters parameters, SanFranciscoPersonPredictor predictor) {
		this.predictor = predictor;
		this.parameters = parameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SanFranciscoPersonVariables variables = predictor.predictVariables(person, trip, elements);

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
