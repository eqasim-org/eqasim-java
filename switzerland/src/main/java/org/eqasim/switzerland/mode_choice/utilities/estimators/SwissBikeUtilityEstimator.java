package org.eqasim.switzerland.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissBikePredictor;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissBikeVariables;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import java.lang.Math;

import com.google.inject.Inject;

public class SwissBikeUtilityEstimator extends BikeUtilityEstimator {
	private final SwissModeParameters swissModeParameters;
	private final SwissPersonPredictor swissPersonPredictor;
	private final SwissBikePredictor swissBikePredictor;


	@Inject
	public SwissBikeUtilityEstimator(SwissModeParameters swissModeParameters, SwissPersonPredictor swissPersonPredictor,
			SwissBikePredictor swissBikePredictor) {
		super(swissModeParameters, swissPersonPredictor.delegate, swissBikePredictor.delegate);
		this.swissModeParameters = swissModeParameters;
		this.swissPersonPredictor = swissPersonPredictor;
		this.swissBikePredictor = swissBikePredictor;

	}

	protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
		return (personVariables.statedPreferenceRegion == 3) ? swissModeParameters.swissBike.betaStatedPreferenceRegion3_u : 0.0;
	}

	//new
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

	protected double estimateWorkTripUtility(SwissBikeVariables bikeVariables) {
		if (bikeVariables.isWorkTrip){
			return swissModeParameters.swissBike.betaIsWorkTrip;}
		else {
			return 0.0;
		}
	}
	protected double estimateTravelTimeUtility(SwissBikeVariables bikeVariables) {
		return swissModeParameters.swissBike.betaTravelTime_hour * bikeVariables.travelTime_hour;
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
				Math.pow((bikeVariables.routedDistance/swissModeParameters.swissBike.referenceDist),(swissModeParameters.swissBike.lambdaRCD));
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables personVariables = swissPersonPredictor.predictVariables(person, trip, elements);
		SwissBikeVariables bikeVariables = swissBikePredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += super.estimateUtility(person, trip, elements);
		utility += estimateRegionalUtility(personVariables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateFemaleUtility(personVariables);
		utility += estimateWorkTripUtility(bikeVariables);
		utility += estimateTravelTimeUtility(bikeVariables);
		utility += estimateRoadConditionsUtility(bikeVariables);

		return utility;
	}
}
