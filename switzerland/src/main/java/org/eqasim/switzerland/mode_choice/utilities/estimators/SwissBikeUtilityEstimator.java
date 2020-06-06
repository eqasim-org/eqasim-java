package org.eqasim.switzerland.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SwissBikeUtilityEstimator extends BikeUtilityEstimator {
	private final SwissModeParameters parameters;
	private final SwissPersonPredictor personPredictor;

	@Inject
	public SwissBikeUtilityEstimator(SwissModeParameters parameters, SwissPersonPredictor personPredictor,
			BikePredictor bikePredictor) {
		super(parameters, personPredictor.delegate, bikePredictor);

		this.parameters = parameters;
		this.personPredictor = personPredictor;
	}

	protected double estimateRegionalUtility(SwissPersonVariables variables) {
		return (variables.statedPreferenceRegion == 3) ? parameters.swissBike.betaStatedPreferenceRegion3_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables variables = personPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += super.estimateUtility(person, trip, elements);
		utility += estimateRegionalUtility(variables);

		return utility;
	}
}
