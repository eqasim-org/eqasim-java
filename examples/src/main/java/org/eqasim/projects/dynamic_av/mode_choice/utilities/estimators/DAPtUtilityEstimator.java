package org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.DAModeParameters;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.DAPersonPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.DAPtPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.DATripPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.DAPersonVariables;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.DAPtVariables;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.DATripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class DAPtUtilityEstimator extends PtUtilityEstimator {
	private final DAModeParameters parameters;
	private final DAPtPredictor predictor;
	private final DAPersonPredictor personPredictor;
	private final DATripPredictor tripPredictor;

	@Inject
	public DAPtUtilityEstimator(DAModeParameters parameters, DAPtPredictor predictor, DAPersonPredictor personPredictor,
			DATripPredictor tripPredictor) {
		super(parameters, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateInVehicleTimeUtility(DAPtVariables variables) {
		double utility = 0.0;

		utility += parameters.daPt.betaRailTravelTime //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance) //
				* variables.railTravelTime_min;

		if (variables.railTravelTime_min > 0.0 && variables.busTravelTime_min > 0.0) {
			// This is a feeder case
			utility += parameters.daPt.betaFeederTravelTime //
					* variables.busTravelTime_min;
		} else {
			// This is not a feeder case
			utility += parameters.daPt.betaBusTravelTime //
					* EstimatorUtils.interaction(variables.euclideanDistance_km,
							parameters.referenceEuclideanDistance_km, parameters.lambdaTravelTimeEuclideanDistance) //
					* variables.busTravelTime_min;
		}

		return utility;
	}

	protected double estimateMonetaryCostUtility(DAPtVariables variables, DAPersonVariables personVariables) {
		return super.estimateMonetaryCostUtility(variables) //
				* EstimatorUtils.interaction(personVariables.householdIncome_MU, parameters.referenceHouseholdIncome_MU,
						parameters.lambdaCostHouseholdIncome);
	}

	protected double estimateAgeUtility(DAPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.daWalk.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(DATripVariables variables) {
		return variables.purpose.equals("work") ? parameters.daWalk.betaWork : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		DAPtVariables variables = predictor.predictVariables(person, trip, elements);
		DAPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		DATripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateInVehicleTimeUtility(variables);
		utility += estimateWaitingTimeUtility(variables);
		utility += estimateLineSwitchUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);

		return utility;
	}
}
