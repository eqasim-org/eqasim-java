package org.sutlab.hannover.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.sutlab.hannover.mode_choice.parameters.HannoverModeParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class HannoverBicycleUtilityEstimator extends BikeUtilityEstimator {
	private final HannoverModeParameters parameters;

	@Inject
	public HannoverBicycleUtilityEstimator(HannoverModeParameters parameters, PersonPredictor personPredictor,
			BikePredictor predictor) {
		super(parameters, personPredictor, predictor);
		this.parameters = parameters;
	}

	protected double estimateAccessEgressTimeUtility(CarVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = 0.0;

		utility += super.estimateUtility(person, trip, elements);

		return utility;
	}

}