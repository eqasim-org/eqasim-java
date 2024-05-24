package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFParkingPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFParkingVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class IDFPassengerUtilityEstimator implements UtilityEstimator {
	private final IDFModeParameters parameters;

	private final IDFParkingPredictor parkingPredictor;
	private final CarPredictor carPredictor;
	private final IDFPersonPredictor personPredictor;

	@Inject
	public IDFPassengerUtilityEstimator(IDFModeParameters parameters, IDFParkingPredictor parkingPredictor,
			CarPredictor carPredictor, IDFPersonPredictor personPredictor) {
		this.parameters = parameters;
		this.parkingPredictor = parkingPredictor;
		this.carPredictor = carPredictor;
		this.personPredictor = personPredictor;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarVariables carVariables = carPredictor.predictVariables(person, trip, elements);
		IDFParkingVariables parkingVariables = parkingPredictor.predictVariables(person, trip, elements);
		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateInVehicleTimeUtility(carVariables);
		utility += estimateParkingPressureUtility(parkingVariables);
		utility += estimateDrivingPermitUtility(personVariables);

		return utility;
	}

	private double estimateConstantUtility() {
		return parameters.idfPassenger.alpha_u;
	}

	private double estimateInVehicleTimeUtility(CarVariables carVariables) {
		return parameters.idfPassenger.betaInVehicleTime_u_min
				* EstimatorUtils.interaction(carVariables.euclideanDistance_km,
						parameters.referenceEuclideanDistance_km, parameters.lambdaTravelTimeEuclideanDistance)
				* carVariables.travelTime_min;
	}

	private double estimateParkingPressureUtility(IDFParkingVariables parkingVariables) {
		return parameters.idfPassenger.betaParkingPressure_u * parkingVariables.parkingPressure;
	}

	private double estimateDrivingPermitUtility(IDFPersonVariables personVariables) {
		if (personVariables.hasDrivingPermit) {
			return parameters.idfPassenger.betaDrivingPermit_u;
		} else {
			return 0.0;
		}
	}
}
