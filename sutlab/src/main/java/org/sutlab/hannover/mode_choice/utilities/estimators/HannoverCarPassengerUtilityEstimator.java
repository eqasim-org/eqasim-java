package org.sutlab.hannover.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.sutlab.hannover.mode_choice.parameters.HannoverModeParameters;
import org.sutlab.hannover.mode_choice.utilities.predictors.HannoverCarPassengerPredictor;
import org.sutlab.hannover.mode_choice.utilities.predictors.HannoverPersonPredictor;
import org.sutlab.hannover.mode_choice.utilities.variables.HannoverCarPassengerVariables;
import org.sutlab.hannover.mode_choice.utilities.variables.HannoverPersonVariables;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class HannoverCarPassengerUtilityEstimator implements UtilityEstimator {
	private final HannoverModeParameters parameters;
	private final HannoverCarPassengerPredictor predictor;
	private final HannoverPersonPredictor personPredictor;

	@Inject
	public HannoverCarPassengerUtilityEstimator(HannoverModeParameters parameters, HannoverCarPassengerPredictor predictor,
			HannoverPersonPredictor personPredictor) {
		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
	}

	protected double estimateConstantUtility() {
		return parameters.carPassenger.alpha_u;
	}

	protected double estimateTravelTimeUtility(HannoverCarPassengerVariables variables) {
		return parameters.carPassenger.betaInVehicleTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateAccessEgressTimeUtility(HannoverCarPassengerVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateDrivingPermit(HannoverPersonVariables variables) {
		return variables.hasDrivingPermit ? parameters.carPassenger.betaDrivingPermit_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		HannoverCarPassengerVariables variables = predictor.predictVariables(person, trip, elements);
		HannoverPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateDrivingPermit(personVariables);

		if (isParis(trip)) {
			utility += parameters.hannover.carPassenger_u;
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