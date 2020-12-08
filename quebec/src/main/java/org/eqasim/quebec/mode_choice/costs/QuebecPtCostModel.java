package org.eqasim.quebec.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.quebec.mode_choice.parameters.QuebecCostParameters;
import org.eqasim.quebec.mode_choice.utilities.predictors.QuebecPersonPredictor;
import org.eqasim.quebec.mode_choice.utilities.variables.QuebecPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class QuebecPtCostModel implements CostModel {
	private final QuebecPersonPredictor predictor;
	private final QuebecCostParameters parameters;

	@Inject
	public QuebecPtCostModel(QuebecCostParameters parameters, QuebecPersonPredictor predictor) {
		this.predictor = predictor;
		this.parameters = parameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		QuebecPersonVariables variables = predictor.predictVariables(person, trip, elements);

		if (variables.hasSubscription) {
			return 0.0;
		}
	
		else
			return parameters.ptCostPerTrip_CAD ; 
	}
}
