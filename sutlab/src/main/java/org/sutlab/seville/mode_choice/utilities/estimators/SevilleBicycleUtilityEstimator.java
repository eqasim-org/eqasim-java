package org.sutlab.seville.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.sutlab.seville.mode_choice.parameters.SevilleModeParameters;

import java.util.List;

public class SevilleBicycleUtilityEstimator extends BikeUtilityEstimator {
	private final SevilleModeParameters parameters;

	@Inject
	public SevilleBicycleUtilityEstimator(SevilleModeParameters parameters, PersonPredictor personPredictor,
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