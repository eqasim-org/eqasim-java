package org.eqasim.examples.zurich_carsharing.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.examples.zurich_carsharing.mode_choice.parameters.CarsharingModeParameters;
import org.eqasim.examples.zurich_carsharing.mode_choice.utilities.predictors.CarsharingPredictor;
import org.eqasim.examples.zurich_carsharing.mode_choice.utilities.variables.CarsharingVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class CarsharingEstimator implements UtilityEstimator {
	private final CarsharingPredictor predictor;
	private final CarsharingModeParameters carsharingParameters;
	private final ModeParameters generalParameters;

	@Inject
	public CarsharingEstimator( CarsharingPredictor predictor,
			CarsharingModeParameters carsharingParameters,
			ModeParameters generalParameters) {
		this.predictor = predictor;
		this.carsharingParameters = carsharingParameters;
		this.generalParameters = generalParameters;
	}
	
	protected double estimateMonetaryCostUtility(CarsharingVariables variables) {
		return generalParameters.betaCost_u_MU
				//* EstimatorUtils.interaction(variables.euclideanDistance_km,
				//		generalParameters.referenceEuclideanDistance_km, generalParameters.lambdaCostEuclideanDistance)
				* variables.cost_MU;
	}
	
	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarsharingVariables variables = this.predictor.predict(person, trip, elements);
		if (!variables.foundVehicle)
			return -100;
		double utility = 0.0;
		
		utility += carsharingParameters.alpha_u;
		utility += carsharingParameters.betaTravelTime_u_min * (variables.travelTime_min + 4.0);
		utility += estimateMonetaryCostUtility(variables);
		utility += carsharingParameters.betaAccessTime_u_min * (variables.accessEgressTime_min);
		
		return utility;
	}
}
