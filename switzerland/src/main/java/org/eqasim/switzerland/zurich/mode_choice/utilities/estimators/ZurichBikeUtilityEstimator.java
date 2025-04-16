package org.eqasim.switzerland.zurich.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.switzerland.ch.mode_choice.utilities.estimators.SwissBikeUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.parameters.ZurichModeParameters;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichBikePredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichPersonPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichTripPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichBikeVariables;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichPersonVariables;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class ZurichBikeUtilityEstimator extends SwissBikeUtilityEstimator {
	static public final String NAME = "ZurichBikeEstimator";

	private final ZurichModeParameters parameters;
	private final ZurichBikePredictor predictor;
	private final ZurichPersonPredictor personPredictor;
	private final ZurichTripPredictor tripPredictor;

	@Inject
	public ZurichBikeUtilityEstimator(ZurichModeParameters parameters, ZurichBikePredictor predictor,
			ZurichPersonPredictor personPredictor, ZurichTripPredictor tripPredictor) {
		super(parameters, personPredictor.delegate, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateTravelTimeUtility(ZurichBikeVariables variables) {
		return super.estimateTravelTimeUtility(variables) //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateAgeUtility(ZurichPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.ZurichBike.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(ZurichTripVariables variables) {
		return variables.isWork ? parameters.ZurichBike.betaWork : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		ZurichBikeVariables variables = predictor.predictVariables(person, trip, elements);
		ZurichPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		ZurichTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);

		return utility;
	}
}