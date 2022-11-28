package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFParkingPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFParkingVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class IDFCarUtilityEstimator extends CarUtilityEstimator {
	private final IDFModeParameters parameters;

	private final CarPredictor carPredictor;
	private final IDFParkingPredictor parkingPredictor;

	@Inject
	public IDFCarUtilityEstimator(IDFModeParameters parameters, IDFParkingPredictor parkingPredictor,
			CarPredictor carPredictor) {
		super(parameters, carPredictor);

		this.parameters = parameters;
		this.carPredictor = carPredictor;
		this.parkingPredictor = parkingPredictor;
	}

	protected double estimateTravelTimeUtility(CarVariables variables) {
		return super.estimateTravelTimeUtility(variables) * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateParkingPressureUtility(IDFParkingVariables variables) {
		return parameters.idfCar.parkingPressure_u * variables.parkingPressure;
	}

	protected double estimateMonetaryCostUtility(CarVariables variables, IDFParkingVariables parkingVariables) {
		return super.estimateMonetaryCostUtility(variables)
				+ parameters.betaCost_u_MU
						* EstimatorUtils.interaction(variables.euclideanDistance_km,
								parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance)
						* parkingVariables.parkingCost_EUR;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarVariables carVariables = carPredictor.predictVariables(person, trip, elements);
		IDFParkingVariables parkingVariables = parkingPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(carVariables);
		utility += estimateAccessEgressTimeUtility(carVariables);
		utility += estimateMonetaryCostUtility(carVariables);
		utility += estimateParkingPressureUtility(parkingVariables);

		return utility;
	}
}
