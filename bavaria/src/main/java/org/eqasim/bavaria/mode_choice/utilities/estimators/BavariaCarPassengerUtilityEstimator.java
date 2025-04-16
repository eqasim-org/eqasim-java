package org.eqasim.bavaria.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.bavaria.mode_choice.parameters.BavariaModeParameters;
import org.eqasim.bavaria.mode_choice.utilities.predictors.BavariaCarPassengerPredictor;
import org.eqasim.bavaria.mode_choice.utilities.predictors.BavariaPersonPredictor;
import org.eqasim.bavaria.mode_choice.utilities.variables.BavariaCarPassengerVariables;
import org.eqasim.bavaria.mode_choice.utilities.variables.BavariaPersonVariables;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class BavariaCarPassengerUtilityEstimator implements UtilityEstimator {
	private final BavariaModeParameters parameters;
	private final BavariaCarPassengerPredictor predictor;
	private final BavariaPersonPredictor personPredictor;

	@Inject
	public BavariaCarPassengerUtilityEstimator(BavariaModeParameters parameters, BavariaCarPassengerPredictor predictor,
			BavariaPersonPredictor personPredictor) {
		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
	}

	protected double estimateConstantUtility() {
		return parameters.carPassenger.alpha_u;
	}

	protected double estimateTravelTimeUtility(BavariaCarPassengerVariables variables) {
		return parameters.carPassenger.betaInVehicleTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateAccessEgressTimeUtility(BavariaCarPassengerVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateDrivingPermit(BavariaPersonVariables variables) {
		return variables.hasDrivingPermit ? parameters.carPassenger.betaDrivingPermit_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		BavariaCarPassengerVariables variables = predictor.predictVariables(person, trip, elements);
		BavariaPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateDrivingPermit(personVariables);

		if (isParis(trip)) {
			utility += parameters.munich.carPassenger_u;
		}

		return utility;
	}

	static private boolean isParis(DiscreteModeChoiceTrip trip) {
		return isParis(trip.getOriginActivity()) || isParis(trip.getDestinationActivity());
	}

	static private boolean isParis(Activity activity) {
		Boolean isParis = (Boolean) activity.getAttributes().getAttribute("isParis");
		return isParis != null && isParis;
	}
}