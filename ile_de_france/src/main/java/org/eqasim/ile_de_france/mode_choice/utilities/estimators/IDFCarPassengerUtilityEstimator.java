package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFCarPassengerPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFCarPassengerVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class IDFCarPassengerUtilityEstimator implements UtilityEstimator {
	private final IDFModeParameters parameters;
	private final IDFCarPassengerPredictor predictor;

	@Inject
	public IDFCarPassengerUtilityEstimator(IDFModeParameters parameters, IDFCarPassengerPredictor predictor) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return parameters.idfCarPassenger.alpha_u;
	}

	protected double estimateTravelTimeUtility(IDFCarPassengerVariables variables) {
		return parameters.idfCarPassenger.betaInVehicleTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateAccessEgressTimeUtility(IDFCarPassengerVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		IDFCarPassengerVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);

		return utility;
	}
}