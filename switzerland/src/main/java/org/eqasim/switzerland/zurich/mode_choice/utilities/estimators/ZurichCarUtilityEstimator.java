package org.eqasim.switzerland.zurich.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.switzerland.zurich.mode_choice.parameters.ZurichModeParameters;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichPersonPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichTripPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichPersonVariables;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichTripVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class ZurichCarUtilityEstimator extends CarUtilityEstimator {
	static public final String NAME = "ZurichCarEstimator";

	private final ZurichModeParameters parameters;
	private final ZurichPersonPredictor personPredictor;
	private final ZurichTripPredictor tripPredictor;
	private final CarPredictor predictor;

	@Inject
	public ZurichCarUtilityEstimator(ZurichModeParameters parameters, CarPredictor predictor,
			ZurichPersonPredictor personPredictor, ZurichTripPredictor tripPredictor) {
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

	protected double estimateMonetaryCostUtility(CarVariables variables, ZurichPersonVariables personVariables) {
		return super.estimateMonetaryCostUtility(variables) //
				* EstimatorUtils.interaction(personVariables.householdIncome_MU, parameters.referenceHouseholdIncome_MU,
						parameters.lambdaCostHouseholdIncome);
	}

	protected double estimateAgeUtility(ZurichPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.ZurichCar.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(ZurichTripVariables variables) {
		return variables.isWork ? parameters.ZurichCar.betaWork : 0.0;
	}

	protected double estimateCityUtility(ZurichTripVariables variables) {
		return variables.isCity ? parameters.ZurichCar.betaCity : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarVariables variables = predictor.predictVariables(person, trip, elements);
		ZurichPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		ZurichTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables, personVariables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);
		utility += estimateCityUtility(tripVariables);

		Leg leg = (Leg) elements.get(0);
		leg.getAttributes().putAttribute("isNew", true);

		return utility;
	}
}