package org.eqasim.quebec.mode_choice.utilities.estimators;

import java.util.List;


import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.quebec.mode_choice.parameters.QuebecModeParameters;
import org.eqasim.quebec.mode_choice.utilities.predictors.QuebecPersonPredictor;
//import org.eqasim.quebec.mode_choice.utilities.variables.QuebecPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class QuebecCarUtilityEstimator extends CarUtilityEstimator {
	private final QuebecModeParameters parameters;
//	private final QuebecPersonPredictor predictor;
	private final CarPredictor CarPredictor;

	@Inject
	public QuebecCarUtilityEstimator(QuebecModeParameters parameters, PersonPredictor personPredictor,
			CarPredictor CarPredictor, QuebecPersonPredictor predictor) {
		super(parameters, CarPredictor);
		this.CarPredictor = CarPredictor;
		this.parameters = parameters;
//		this.predictor = predictor;
	}

	

	@Override
	
	protected double estimateTravelTimeUtility(CarVariables variables) {
		return (parameters.car.betaTravelTime_u_min * (variables.travelTime_min)) ;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
//		QuebecPersonVariables variables = predictor.predictVariables(person, trip, elements); ????CarSubscriCarion???
		CarVariables variables_Car = CarPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateTravelTimeUtility(variables_Car)  ;


		return utility;
	}
}
