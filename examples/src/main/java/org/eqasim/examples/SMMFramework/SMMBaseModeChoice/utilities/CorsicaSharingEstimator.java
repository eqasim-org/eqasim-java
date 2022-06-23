package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.parameters.CorsicaSharingParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class CorsicaSharingEstimator implements UtilityEstimator {
	private final CorsicaSharingParameters parameters;
	private final CorsicaSharingPredictor predictor;


	@Inject
	public CorsicaSharingEstimator(CorsicaSharingParameters parameters, CorsicaSharingPredictor predictor) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return parameters.bikeSharing.alpha_u;
	}

	protected double estimateTravelTimeUtility(CorsicaSharingVariables variables) {
		return parameters.bikeSharing.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateAccessTimeUtility(CorsicaSharingVariables variables) {
		return parameters.bikeSharing.betaAccess_Time * variables.accessTime_min;
	}

	protected double estimateMonetaryCostUtility(CorsicaSharingVariables variables) {
		return parameters.betaCost_u_MU * variables.cost_MU;
	}

	protected double estimateEgressTimeUtility(CorsicaSharingVariables variables) {
		return parameters.bikeSharing.betaEgress_Time* variables.egressTime_min;
	}
	protected double estimateDetourTimeUtility(CorsicaSharingVariables variables){
		return parameters.bikeSharing.betaDetour_Time*variables.detour_min;

	}
	protected double estimateParkingTimeUtility(CorsicaSharingVariables variables){
		return parameters.bikeSharing.betaParkingTime_u_min*variables.parkingTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CorsicaSharingVariables variables = predictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateEgressTimeUtility(variables);
		utility+= estimateParkingTimeUtility(variables);
		utility+= estimateDetourTimeUtility(variables);
		return utility;
	}
}
