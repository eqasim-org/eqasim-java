package org.eqasim.core.simulation.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class CarEstimator implements UtilityEstimator {
	private final CarPredictor carPredictor;
	private final ModeParameters parameters;

	@Inject
	public CarEstimator(CarPredictor carPredictor, ModeParameters parameters) {
		this.parameters = parameters;
		this.carPredictor = carPredictor;
	}

	protected double predictConstantUtility() {
		return parameters.car.alpha_u;
	}

	protected double predictTravelTimeUtility(CarVariables variables) {
		return parameters.car.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double predictAccessEgressUtility(CarVariables variables) {
		return parameters.walk.betaTravelTime_u_min * variables.accessEgressTime_min;
	}

	protected double predictMonetaryCostUtility(CarVariables variables) {
		return parameters.betaCost_u_MU //
				* EstimatorUtilities.interaction(variables.euclideanDistance_km,
						parameters.referenceEuclideanDistance_km, parameters.lambdaCostCrowflyDistance) //
				* variables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = 0.0;

		CarVariables carVariables = carPredictor.predictVariables(person, trip, elements);

		utility += predictConstantUtility();
		utility += predictTravelTimeUtility(carVariables);
		utility += predictAccessEgressUtility(carVariables);
		utility += predictMonetaryCostUtility(carVariables);

		return utility;
	}
}
