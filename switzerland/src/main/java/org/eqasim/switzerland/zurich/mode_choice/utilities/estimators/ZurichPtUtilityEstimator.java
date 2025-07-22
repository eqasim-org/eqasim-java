package org.eqasim.switzerland.zurich.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.parameters.ZurichModeParameters;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichPersonPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichPtPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichTripPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichPersonVariables;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichPtVariables;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class ZurichPtUtilityEstimator extends PtUtilityEstimator {
	static public final String NAME = "ZurichPtEstimator";

	private final ZurichModeParameters parameters;
	private final ZurichPtPredictor predictor;
	private final ZurichPersonPredictor personPredictor;
	private final ZurichTripPredictor tripPredictor;

	@Inject
	public ZurichPtUtilityEstimator(ZurichModeParameters parameters, ZurichPtPredictor predictor,
			ZurichPersonPredictor personPredictor, ZurichTripPredictor tripPredictor) {
		super(parameters, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateInVehicleTimeUtility(ZurichPtVariables variables) {
		double utility = 0.0;

		utility += parameters.ZurichPt.betaRailTravelTime_u_min //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance) //
				* variables.railTravelTime_min;

		if (variables.railTravelTime_min > 0.0 && variables.busTravelTime_min > 0.0) {
			// This is a feeder case
			utility += parameters.ZurichPt.betaFeederTravelTime_u_min //
					* variables.busTravelTime_min;
		} else {
			// This is not a feeder case
			utility += parameters.ZurichPt.betaBusTravelTime_u_min //
					* EstimatorUtils.interaction(variables.euclideanDistance_km,
							parameters.referenceEuclideanDistance_km, parameters.lambdaTravelTimeEuclideanDistance) //
					* variables.busTravelTime_min;
		}

		return utility;
	}

	protected double estimateMonetaryCostUtility(ZurichPtVariables variables, ZurichPersonVariables personVariables) {
		return super.estimateMonetaryCostUtility(variables) //
				* EstimatorUtils.interaction(personVariables.householdIncome_MU, parameters.referenceHouseholdIncome_MU,
						parameters.lambdaCostHouseholdIncome);
	}

	protected double estimateAgeUtility(ZurichPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.ZurichWalk.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(ZurichTripVariables variables) {
		return variables.isWork ? parameters.ZurichWalk.betaWork : 0.0;
	}

	protected double estimateHeadwayUtility(ZurichPtVariables variables) {
		if (parameters.ZurichPt.betaHeadway_u_min != 0.0 && variables.headway_min == 0.0) {
			throw new IllegalStateException("Non-zero beta for headway, but no headway is given.");
		}

		return parameters.ZurichPt.betaHeadway_u_min * variables.headway_min;
	}

	protected double estimateOvgkUtility(ZurichPtVariables variables) {
		switch (variables.ovgk) {
		case A:
			return 0.0;
		case B:
			return parameters.ZurichPt.betaOvgkB_u;
		case C:
			return parameters.ZurichPt.betaOvgkC_u;
		case D:
			return parameters.ZurichPt.betaOvgkD_u;
		case None:
			return parameters.ZurichPt.betaOvgkNone_u;
		default:
			throw new RuntimeException();
		}
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		ZurichPtVariables variables = predictor.predictVariables(person, trip, elements);
		ZurichPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		ZurichTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateInVehicleTimeUtility(variables);
		utility += estimateWaitingTimeUtility(variables);
		utility += estimateLineSwitchUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);
		utility += estimateHeadwayUtility(variables);
		utility += estimateOvgkUtility(variables);

		return utility;
	}
}