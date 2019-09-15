package org.eqasim.switzerland.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarEstimator;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SwissCarEstimator implements UtilityEstimator {
	private final CarEstimator delegate;
	private final SwissModeParameters parameters;
	private final SwissPersonPredictor swissPersonPredictor;

	@Inject
	public SwissCarEstimator(CarEstimator delegate, SwissPersonPredictor swissPersonPredictor,
			SwissModeParameters parameters) {
		this.delegate = delegate;
		this.parameters = parameters;
		this.swissPersonPredictor = swissPersonPredictor;
	}

	protected double predictRegionUtility(SwissPersonVariables variables) {
		double utility = 0.0;

		if (variables.statedPreferenceRegion == 1) {
			utility += parameters.swissCar.betaStatedPreferenceRegion1_u;
		} else if (variables.statedPreferenceRegion == 3) {
			utility += parameters.swissCar.betaStatedPreferenceRegion3_u;
		}

		return utility;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = delegate.estimateUtility(person, trip, elements);

		SwissPersonVariables variables = swissPersonPredictor.predictVariables(person);
		utility += predictRegionUtility(variables);

		return utility;
	}
}
