package org.eqasim.sao_paulo.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloCostParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SaoPauloPtCostModel implements CostModel {
	private final SaoPauloPersonPredictor predictor;
	private final SaoPauloCostParameters parameters;

	@Inject
	public SaoPauloPtCostModel(SaoPauloCostParameters parameters, SaoPauloPersonPredictor predictor) {
		this.predictor = predictor;
		this.parameters = parameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SaoPauloPersonVariables variables = predictor.predictVariables(person, trip, elements);

		if (variables.hasSubscription) {
			return 0.0;
		}

		return parameters.ptCostPerTrip_BRL;
	}
}
