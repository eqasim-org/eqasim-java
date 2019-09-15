package org.eqasim.switzerland.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeEstimator;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SwissBikeEstimator implements UtilityEstimator {
	private final SwissModeParameters parameters;
	private final BikeEstimator delegate;
	private final SwissPersonPredictor swissPersonPredictor;

	@Inject
	public SwissBikeEstimator(SwissModeParameters parameters, BikeEstimator delegate,
			SwissPersonPredictor swissPersonPredictor) {
		this.delegate = delegate;
		this.swissPersonPredictor = swissPersonPredictor;
		this.parameters = parameters;
	}

	protected double predictRegionUtility(SwissPersonVariables variables) {
		return variables.statedPreferenceRegion == 3 ? parameters.swissBike.betaStatedPreferenceRegion3_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = delegate.estimateUtility(person, trip, elements);

		SwissPersonVariables variables = swissPersonPredictor.predictVariables(person);
		utility += predictRegionUtility(variables);

		return utility;
	}
}
