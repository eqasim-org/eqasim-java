package org.eqasim.wayne_county.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.wayne_county.mode_choice.parameters.WayneCountyCostParameters;
import org.eqasim.wayne_county.mode_choice.utilities.predictors.WayneCountyPersonPredictor;
import org.eqasim.wayne_county.mode_choice.utilities.variables.WayneCountyPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class WayneCountyPtCostModel implements CostModel {
	private final WayneCountyPersonPredictor predictor;
	private final WayneCountyCostParameters parameters;

	@Inject
	public WayneCountyPtCostModel(WayneCountyCostParameters parameters, WayneCountyPersonPredictor predictor) {
		this.predictor = predictor;
		this.parameters = parameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		WayneCountyPersonVariables variables = predictor.predictVariables(person, trip, elements);

		/*if (variables.age >= 65) {
			return 0.50;
		}
		TODO:
		add age person variable in WayneCountyPersonVariables, WayneCountyPersonPredictor, WayneCountyPredictorUtils
		*/
		return 2;
	}
}
