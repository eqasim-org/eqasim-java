package org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.ProjectModeParameters;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectPersonPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectPtPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectTripPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectPersonVariables;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectPtVariables;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class ProjectPtUtilityEstimator extends PtUtilityEstimator {
	private final ProjectModeParameters parameters;
	private final ProjectPtPredictor predictor;
	private final ProjectPersonPredictor personPredictor;
	private final ProjectTripPredictor tripPredictor;

	@Inject
	public ProjectPtUtilityEstimator(ProjectModeParameters parameters, ProjectPtPredictor predictor,
			ProjectPersonPredictor personPredictor, ProjectTripPredictor tripPredictor) {
		super(parameters, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateInVehicleTimeUtility(ProjectPtVariables variables) {
		double utility = 0.0;

		utility += parameters.projectPt.betaRailTravelTime_u_min //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance) //
				* variables.railTravelTime_min;

		if (variables.railTravelTime_min > 0.0 && variables.busTravelTime_min > 0.0) {
			// This is a feeder case
			utility += parameters.projectPt.betaFeederTravelTime_u_min //
					* variables.busTravelTime_min;
		} else {
			// This is not a feeder case
			utility += parameters.projectPt.betaBusTravelTime_u_min //
					* EstimatorUtils.interaction(variables.euclideanDistance_km,
							parameters.referenceEuclideanDistance_km, parameters.lambdaTravelTimeEuclideanDistance) //
					* variables.busTravelTime_min;
		}

		return utility;
	}

	protected double estimateMonetaryCostUtility(ProjectPtVariables variables, ProjectPersonVariables personVariables) {
		return super.estimateMonetaryCostUtility(variables) //
				* EstimatorUtils.interaction(personVariables.householdIncome_MU, parameters.referenceHouseholdIncome_MU,
						parameters.lambdaCostHouseholdIncome);
	}

	protected double estimateAgeUtility(ProjectPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.projectWalk.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(ProjectTripVariables variables) {
		return variables.purpose.equals("work") ? parameters.projectWalk.betaWork : 0.0;
	}

	protected double estimateHeadwayUtility(ProjectPtVariables variables) {
		if (parameters.projectPt.betaHeadway_u_min > 0.0 && variables.headway_min == 0.0) {
			throw new IllegalStateException("Non-zero beta for headway, but no headway is given.");
		}

		return parameters.projectPt.betaHeadway_u_min * variables.headway_min;
	}

	protected double estimateOvgkUtility(ProjectPtVariables variables) {
		switch (variables.ovgk) {
		case A:
			return 0.0;
		case B:
			return parameters.projectPt.betaOvgkB_u;
		case C:
			return parameters.projectPt.betaOvgkC_u;
		case D:
			return parameters.projectPt.betaOvgkD_u;
		case None:
			return parameters.projectPt.betaOvgkNone_u;
		default:
			throw new RuntimeException();
		}
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		ProjectPtVariables variables = predictor.predictVariables(person, trip, elements);
		ProjectPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		ProjectTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

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
