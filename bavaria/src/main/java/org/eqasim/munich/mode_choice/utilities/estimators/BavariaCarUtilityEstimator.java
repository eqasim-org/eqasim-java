package org.eqasim.bavaria.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.bavaria.mode_choice.parameters.BavariaModeParameters;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class BavariaCarUtilityEstimator extends CarUtilityEstimator {
	private final BavariaModeParameters parameters;
	private final CarPredictor predictor;

	@Inject
	public BavariaCarUtilityEstimator(BavariaModeParameters parameters, CarPredictor predictor) {
		super(parameters, predictor);

		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateAccessEgressTimeUtility(CarVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);

		if (isParis(trip)) {
			utility += parameters.BavariaParis.car_u;
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
