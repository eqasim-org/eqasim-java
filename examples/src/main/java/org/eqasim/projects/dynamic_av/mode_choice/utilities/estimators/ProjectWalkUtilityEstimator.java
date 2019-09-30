package org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.ProjectModeParameters;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectPersonPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectTripPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectWalkPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectPersonVariables;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectTripVariables;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectWalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class ProjectWalkUtilityEstimator extends WalkUtilityEstimator {
	private final ProjectModeParameters parameters;
	private final ProjectWalkPredictor predictor;
	private final ProjectPersonPredictor personPredictor;
	private final ProjectTripPredictor tripPredictor;

	@Inject
	public ProjectWalkUtilityEstimator(ProjectModeParameters parameters, ProjectWalkPredictor predictor,
			ProjectPersonPredictor personPredictor, ProjectTripPredictor tripPredictor) {
		super(parameters, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateTravelTimeUtility(ProjectWalkVariables variables) {
		return super.estimateTravelTimeUtility(variables) //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateAgeUtility(ProjectPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.projectWalk.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(ProjectTripVariables variables) {
		return variables.purpose.equals("work") ? parameters.projectWalk.betaWork : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		ProjectWalkVariables variables = predictor.predictVariables(person, trip, elements);
		ProjectPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		ProjectTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);

		if (variables.travelTime_min > 30.0) {
			utility -= 100.0;
		}

		return utility;
	}
}
