package org.sutlab.seville_extended.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.sutlab.seville_extended.mode_choice.parameters.SevilleModeParameters;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevilleCarPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevillePersonPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevilleTripPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevilleCarVariables;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevillePersonVariables;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevilleTripVariables;

import java.util.List;

public class SevilleCarUtilityEstimator implements UtilityEstimator {
	private final SevilleModeParameters parameters;
	private final SevilleCarPredictor predictor;
	private final SevillePersonPredictor personPredictor;
	private final SevilleTripPredictor tripPredictor;

	@Inject
	public SevilleCarUtilityEstimator(SevilleModeParameters parameters, SevilleCarPredictor predictor,
									  SevillePersonPredictor personPredictor, SevilleTripPredictor tripPredictor) {
		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateConstantUtility() {
		return parameters.car.alpha_u;
	}

	protected double estimateTravelTimeUtility(SevilleCarVariables variables) {
		return parameters.car.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateAccessEgressTimeUtility(SevilleCarVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateMonetaryCostUtility(SevilleCarVariables variables) {
		return parameters.betaCost_u_MU * variables.cost_MU;
	}

	protected double estimateParkingZoneUtility(SevilleTripVariables variables) {
		return variables.isParkingZoneDestination ? parameters.sevilleCar.betaParkingZone_u : 0.0;
	}

	protected double estimateSpeedUtility(SevilleCarVariables variables) {
		return parameters.sevilleCar.betaSpeed_u_kmh * variables.speed_kmh;
	}

	protected double estimateDistanceUtility(SevilleTripVariables variables) {
		return parameters.sevilleCar.betaDistanceAbove0_u_km * variables.distanceAbove0_km
				+ parameters.sevilleCar.betaDistanceAbove2_u_km * variables.distanceAbove2_km
				+ parameters.sevilleCar.betaDistanceAbove5_u_km * variables.distanceAbove5_km
				+ parameters.sevilleCar.betaDistanceAbove10_u_km * variables.distanceAbove10_km;
	}

	protected double estimatePurposeUtility(SevilleTripVariables variables) {
		double utility = 0.0;
		utility += variables.isWorkTrip ? parameters.sevilleCar.betaWorkTrip_u : 0.0;
		utility += variables.isEducationTrip ? parameters.sevilleCar.betaEducationTrip_u : 0.0;
		return utility;
	}

	protected double estimateFemaleUtility(SevillePersonVariables variables) {
		return variables.isFemale ? parameters.sevilleCar.betaFemale_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SevilleCarVariables variables = predictor.predictVariables(person, trip, elements);
		SevillePersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		SevilleTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateParkingZoneUtility(tripVariables);
		utility += estimateSpeedUtility(variables);
		utility += estimateDistanceUtility(tripVariables);
		utility += estimatePurposeUtility(tripVariables);
		utility += estimateFemaleUtility(personVariables);

		return utility;
	}
}
