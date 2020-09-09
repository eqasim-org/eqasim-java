package org.eqasim.core.simulation.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarPtVariables;
//import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class CarPtUtilityEstimator implements UtilityEstimator {
	private final ModeParameters parameters;
	private final CarPtPredictor carPtPredictor;
	//private final PersonPredictor personPredictor;

	@Inject
	public CarPtUtilityEstimator(ModeParameters parameters, PersonPredictor personPredictor,
			CarPtPredictor carPtPredictor) {
		this.parameters = parameters;
		this.carPtPredictor = carPtPredictor;
		//this.personPredictor = personPredictor;
	}

	protected double estimateConstantUtility() {
		return parameters.car_pt.alpha_u;
	}

	protected double estimateTravelTimeUtility(CarPtVariables variables) {
		return parameters.car_pt.betaTravelTime_u_min * variables.travelTime_min;
	}

	//protected double estimateAgeOver18Utility(PersonVariables variables) {
	//	return parameters.bike.betaAgeOver18_u_a * Math.max(0.0, variables.age_a - 18);
	//}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		//PersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		CarPtVariables carPtVariables = carPtPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(carPtVariables);
		//utility += estimateAgeOver18Utility(personVariables);

		return utility;
	}
}
