package org.eqasim.projects.astra16.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.projects.astra16.AstraModeParameters;
import org.eqasim.projects.astra16.mode_choice.utilities.predictors.AstraPersonPredictor;
import org.eqasim.projects.astra16.mode_choice.utilities.predictors.AstraTripPredictor;
import org.eqasim.projects.astra16.mode_choice.utilities.variables.AstraPersonVariables;
import org.eqasim.projects.astra16.mode_choice.utilities.variables.AstraTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AstraCarUtilityEstimator extends CarUtilityEstimator {
	static public final String NAME = "AstraCarEstimator";

	private final AstraModeParameters parameters;
	private final AstraPersonPredictor personPredictor;
	private final AstraTripPredictor tripPredictor;
	private final CarPredictor predictor;

	@Inject
	public AstraCarUtilityEstimator(AstraModeParameters parameters, CarPredictor predictor,
			AstraPersonPredictor personPredictor, AstraTripPredictor tripPredictor) {
		super(parameters, predictor);

		this.parameters = parameters;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
		this.predictor = predictor;
	}

	protected double estimateTravelTimeUtility(CarVariables variables) {
		return super.estimateTravelTimeUtility(variables) //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateMonetaryCostUtility(CarVariables variables, AstraPersonVariables personVariables) {
		return super.estimateMonetaryCostUtility(variables) //
				* EstimatorUtils.interaction(personVariables.householdIncome_MU, parameters.referenceHouseholdIncome_MU,
						parameters.lambdaCostHouseholdIncome);
	}

	protected double estimateAgeUtility(AstraPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.astraCar.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(AstraTripVariables variables) {
		return variables.isWork ? parameters.astraCar.betaWork : 0.0;
	}

	protected double estimateCityUtility(AstraTripVariables variables) {
		return variables.isCity ? parameters.astraCar.betaCity : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarVariables variables = predictor.predictVariables(person, trip, elements);
		AstraPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		AstraTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables, personVariables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);
		utility += estimateCityUtility(tripVariables);

		return utility;
	}
}
