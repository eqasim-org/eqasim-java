package org.eqasim.wayne_county.mode_choice.utilities.estimators;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.wayne_county.mode_choice.parameters.WayneCountyModeParameters;
import org.eqasim.wayne_county.mode_choice.utilities.predictors.WayneCountyPersonPredictor;
import org.eqasim.wayne_county.mode_choice.utilities.variables.WayneCountyPersonVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;

import java.util.List;

public class WayneCountyDRTUtilityEstimator implements UtilityEstimator {
	private final WayneCountyModeParameters parameters;
	private final WayneCountyPersonPredictor predictor;
	//private final PtPredictor ptPredictor;

	@Inject
	public WayneCountyDRTUtilityEstimator(WayneCountyModeParameters parameters,
										  PersonPredictor personPredictor,
										  WayneCountyPersonPredictor predictor) {
		//super(parameters, ptPredictor);
		//this.ptPredictor = ptPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}



	//DRT parameters travel time
	double betaDrtAccessEgressTime_min = 0.0;
	double betaDrtInVehicleTime_min = 0.0;
	double betaDrtWaitingTime_min = 0.0;

	protected double estimateConstantUtility(WayneCountyPersonVariables variables) {
		switch (variables.hhlIncomeClass) {
			case 1:
				return parameters.wcDRT.alpha_low_income;
			case 2:
				return parameters.wcDRT.alpha_medium_income;
			case 3:
				return parameters.wcDRT.alpha_high_income;
			default:
				return 2;
		}
	}


	protected double estimateMonetaryCostUtility(WayneCountyPersonVariables variables) {
		double costBeta = 0;
		switch (variables.hhlIncomeClass) {
			case 1:
				costBeta = parameters.wcCost.beta_cost_low_income;
				break;
			case 2:
				costBeta = parameters.wcCost.beta_cost_medium_income;
				break;
			case 3:
				costBeta = parameters.wcCost.beta_cost_high_income;
				break;
		}
		return costBeta;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		// Get all information
		WayneCountyPersonVariables variables = predictor.predictVariables(person, trip, elements);
		double accessEgressTime_min = 0.0;
		double inVehicleTime_min = 0.0;

		double maximumWaitingTime_min = 0.0;
		double directDistance_km = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().equals("drt")) {
					DrtRoute route = (DrtRoute) leg.getRoute();

					// Both waiting time and travel time in the route is already the "worst case" provided by DRT
					inVehicleTime_min += route.getTravelTime() / 60.0;
					maximumWaitingTime_min += route.getMaxWaitTime() / 60.0;

					// This is the direct travel distance
					directDistance_km += route.getDistance() * 1e-3;
				} else {
					accessEgressTime_min += leg.getTravelTime() / 60.0;
				}
			}
		}

		// Calculate cost
		//ToDo: Move to CostParameters
		double pricePerKilometer_USD_km = 0.5;
		double cost_USD = directDistance_km * pricePerKilometer_USD_km;

		// Calculate utility

		double utility = 0.0;
		double ascDrt = estimateConstantUtility(variables);
		double B_Cost = estimateMonetaryCostUtility(variables);

		utility += ascDrt;
		utility += parameters.wcDRT.betaAccessEgressTime_u_min * accessEgressTime_min;
		utility += parameters.wcDRT.beta_time_min * inVehicleTime_min;
		utility += parameters.wcDRT.betaWaitingTime_u_min * maximumWaitingTime_min;
		utility +=B_Cost * cost_USD;

		return utility;

	}


}
