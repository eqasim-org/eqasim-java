package org.sutlab.seville_extended.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.sutlab.seville_extended.mode_choice.parameters.SevilleModeParameters;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevilleCarPassengerPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevillePersonPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevilleTripPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevilleCarPassengerVariables;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevillePersonVariables;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevilleTripVariables;

import java.util.List;

public class SevilleCarPassengerUtilityEstimator implements UtilityEstimator {
	private final SevilleModeParameters parameters;
	private final SevilleCarPassengerPredictor predictor;
	private final SevillePersonPredictor personPredictor;
	private final SevilleTripPredictor tripPredictor;

	@Inject
	public SevilleCarPassengerUtilityEstimator(SevilleModeParameters parameters, SevilleCarPassengerPredictor predictor,
											   SevillePersonPredictor personPredictor, SevilleTripPredictor tripPredictor) {
		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateConstantUtility() {
		return parameters.carPassenger.alpha_u;
	}

	protected double estimateTravelTimeUtility(SevilleCarPassengerVariables variables) {
		return parameters.carPassenger.betaInVehicleTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateAccessEgressTimeUtility(SevilleCarPassengerVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateDrivingPermit(SevillePersonVariables variables) {
		return variables.hasDrivingPermit ? parameters.sevilleCarPassenger.betaDrivingPermit_u : 0.0;
	}

	protected double estimateDistanceUtility(SevilleTripVariables variables) {
		return parameters.sevilleCarPassenger.betaDistanceAbove0_u_km * variables.distanceAbove0_km
				+ parameters.sevilleCarPassenger.betaDistanceAbove2_u_km * variables.distanceAbove2_km
				+ parameters.sevilleCarPassenger.betaDistanceAbove5_u_km * variables.distanceAbove5_km
				+ parameters.sevilleCarPassenger.betaDistanceAbove10_u_km * variables.distanceAbove10_km;
	}

	protected double estimatePurposeUtility(SevilleTripVariables variables) {
		double utility = 0.0;
		utility += variables.isWorkTrip ? parameters.sevilleCarPassenger.betaWorkTrip_u : 0.0;
		utility += variables.isEducationTrip ? parameters.sevilleCarPassenger.betaEducationTrip_u : 0.0;
		return utility;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SevilleCarPassengerVariables variables = predictor.predictVariables(person, trip, elements);
		SevillePersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		SevilleTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateDrivingPermit(personVariables);
		utility += estimateDistanceUtility(tripVariables);
		utility += estimatePurposeUtility(tripVariables);

		return utility;
	}

}
