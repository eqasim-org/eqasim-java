package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class IDFPassengerUtilityEstimator implements UtilityEstimator {
	private final IDFModeParameters parameters;
	private final IDFPersonPredictor personPredictor;
	private final CarPredictor carPredictor;

	@Inject
	public IDFPassengerUtilityEstimator(IDFModeParameters parameters, IDFPersonPredictor personPredictor,
			CarPredictor carPredictor) {
		this.parameters = parameters;
		this.personPredictor = personPredictor;
		this.carPredictor = carPredictor;
	}

	protected double estimateUrbanUtility(IDFSpatialVariables variables) {
		double utility = 0.0;

		if (variables.hasUrbanOrigin && variables.hasUrbanDestination) {
			utility += parameters.idfCar.betaInsideUrbanArea_u;
		}

		if (variables.hasUrbanOrigin || variables.hasUrbanDestination) {
			utility += parameters.idfCar.betaCrossingUrbanArea_u;
		}

		return utility;
	}

	protected double estimateConstantUtility() {
		return parameters.idfPassenger.alpha_u;
	}

	protected double estimateTravelTimeUtility(CarVariables variables) {
		return parameters.idfPassenger.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateLicenseUtility(IDFPersonVariables variables) {
		if (variables.hasLicense) {
			return parameters.idfPassenger.betaLicense_u;
		} else {
			return 0.0;
		}
	}

	protected double estimateHouseholdCarAvailabilityUtility(IDFPersonVariables variables) {
		if (variables.householdCarAvailability) {
			return parameters.idfPassenger.betaHouseholdCarAvailability_u;
		} else {
			return 0.0;
		}
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		CarVariables carVariables = carPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(carVariables);
		utility += estimateLicenseUtility(personVariables);
		utility += estimateHouseholdCarAvailabilityUtility(personVariables);

		return utility;
	}
}
