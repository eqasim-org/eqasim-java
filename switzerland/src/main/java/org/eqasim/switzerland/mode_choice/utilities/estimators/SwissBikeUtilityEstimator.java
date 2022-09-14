package org.eqasim.switzerland.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissBikePredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissTripPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissBikeVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SwissBikeUtilityEstimator implements UtilityEstimator {
	private final SwissModeParameters swissModeParameters;
	private final SwissPersonPredictor swissPersonPredictor;
	private final SwissTripPredictor swissTripPredictor;
	private final SwissBikePredictor swissBikePredictor;


	@Inject
	public SwissBikeUtilityEstimator(SwissModeParameters swissModeParameters, SwissPersonPredictor swissPersonPredictor,
			SwissBikePredictor swissBikePredictor, SwissTripPredictor swissTripPredictor) {

		this.swissModeParameters = swissModeParameters;
		this.swissTripPredictor = swissTripPredictor;
		this.swissPersonPredictor = swissPersonPredictor;
		this.swissBikePredictor = swissBikePredictor;

	}
	
	protected double estimateConstantUtility() {
		return swissModeParameters.bike.alpha_u;
	}

	protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
		return (personVariables.statedPreferenceRegion == 3) ? swissModeParameters.swissBike.betaStatedPreferenceRegion3_u : 0.0;
	}

	protected double estimateAgeUtility(SwissPersonVariables personVariables) {
		return swissModeParameters.swissBike.betaAge * personVariables.age;
	}

	protected double estimateFemaleUtility(SwissPersonVariables personVariables) {
		if (personVariables.isFemale){
			return swissModeParameters.swissBike.betaIsFemale;}
		else {
			return 0.0;
		}
	}

	protected double estimateWorkTripUtility(SwissTripVariables tripVariables) {
		if (tripVariables.isWorkTrip){
			return swissModeParameters.swissBike.betaIsWorkTrip;}
		else {
			return 0.0;
		}
	}
	protected double estimateTravelTimeUtility(SwissBikeVariables bikeVariables) {
		return swissModeParameters.swissBike.betaTravelTime_hour * bikeVariables.travelTime_min/60;
	}

	protected double estimateRoadConditionsUtility(SwissBikeVariables bikeVariables) {
		return (swissModeParameters.swissBike.betaPropS1L1 * bikeVariables.propS1L1 +
				swissModeParameters.swissBike.betaPropS2L1 * bikeVariables.propS2L1 +
				swissModeParameters.swissBike.betaPropS3L1 * bikeVariables.propS3L1 +
				swissModeParameters.swissBike.betaPropS4L1 * bikeVariables.propS4L1 +
				swissModeParameters.swissBike.betaPropS1L2 * bikeVariables.propS1L2 +
				swissModeParameters.swissBike.betaPropS2L2 * bikeVariables.propS2L2 +
				swissModeParameters.swissBike.betaPropS3L2 * bikeVariables.propS3L2 +
				swissModeParameters.swissBike.betaPropS4L2 * bikeVariables.propS4L2)*
				EstimatorUtils.interaction(bikeVariables.routedDistance, swissModeParameters.swissBike.referenceRoutedDistance_km, swissModeParameters.swissBike.lambdaRCD);
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables personVariables = swissPersonPredictor.predictVariables(person, trip, elements);
		SwissBikeVariables bikeVariables = swissBikePredictor.predictVariables(person, trip, elements);
		SwissTripVariables tripVariables = swissTripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;
		utility += estimateConstantUtility();
		utility += estimateRegionalUtility(personVariables);

		utility += estimateAgeUtility(personVariables);
		utility += estimateFemaleUtility(personVariables);
		utility += estimateWorkTripUtility(tripVariables);
		utility += estimateTravelTimeUtility(bikeVariables);
		utility += estimateRoadConditionsUtility(bikeVariables);

		return utility;
	}
}
