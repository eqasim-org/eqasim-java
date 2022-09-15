package org.eqasim.switzerland.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissCarPredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissTripPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissBikeVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissCarVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SwissCarUtilityEstimator implements UtilityEstimator {
	private final SwissModeParameters swissModeParameters;
	private final SwissPersonPredictor swissPersonPredictor;
	private final SwissTripPredictor swissTripPredictor;
	private final SwissCarPredictor swissCarPredictor;

	@Inject
	public SwissCarUtilityEstimator(SwissModeParameters swissModeParameters, SwissCarPredictor swissCarPredictor,
			SwissPersonPredictor swissPersonPredictor,SwissTripPredictor swissTripPredictor) {

		this.swissPersonPredictor = swissPersonPredictor;
		this.swissModeParameters = swissModeParameters;
		this.swissTripPredictor = swissTripPredictor;
		this.swissCarPredictor = swissCarPredictor;
	}

	protected double estimateConstantUtility() {
		return swissModeParameters.car.alpha_u;
	}

	protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
		if (personVariables.statedPreferenceRegion == 1) {
			return swissModeParameters.swissCar.betaStatedPreferenceRegion1_u;
		} else if (personVariables.statedPreferenceRegion == 3) {
			return swissModeParameters.swissCar.betaStatedPreferenceRegion3_u;
		} else {
			return 0.0;
		}
	}

	protected double estimateAgeUtility(SwissPersonVariables personVariables) {
		return swissModeParameters.swissCar.betaAge * personVariables.age;
	}

	protected double estimateFemaleUtility(SwissPersonVariables personVariables) {
		if (personVariables.isFemale){
			return swissModeParameters.swissCar.betaIsFemale;}
		else {
			return 0.0;
		}
	}

	protected double estimateWorkTripUtility(SwissTripVariables tripVariables) {
		if (tripVariables.isWorkTrip){
			return swissModeParameters.swissCar.betaIsWorkTrip;}
		else {
			return 0.0;
		}
	}

	protected double estimateTravelTimeUtility(SwissCarVariables carVariables) {
		return swissModeParameters.swissCar.betaTravelTime_hour * carVariables.travelTime_min/60;
	}

	protected double estimateMonetaryCostUtility(SwissCarVariables carVariables) {
		return swissModeParameters.betaCost_RD * EstimatorUtils.interaction(carVariables.routedDistance_km,
				swissModeParameters.referenceRoutedDistance_km, swissModeParameters.lambdaCostRoutedDistance) * carVariables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables personVariables = swissPersonPredictor.predictVariables(person, trip, elements);
		SwissTripVariables tripVariables = swissTripPredictor.predictVariables(person, trip, elements);
		SwissCarVariables carVariables = swissCarPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateRegionalUtility(personVariables);

		utility += estimateAgeUtility(personVariables);
		utility += estimateFemaleUtility(personVariables);
		utility += estimateWorkTripUtility(tripVariables);
		utility += estimateTravelTimeUtility(carVariables);
		utility += estimateMonetaryCostUtility(carVariables);

		return utility;
	}
}
