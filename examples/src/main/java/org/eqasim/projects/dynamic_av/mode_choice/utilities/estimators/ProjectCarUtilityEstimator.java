package org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.projects.dynamic_av.mode_choice.ProjectModeParameters;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectPersonPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectTripPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectPersonVariables;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class ProjectCarUtilityEstimator extends CarUtilityEstimator {
	private final ProjectModeParameters parameters;
	private final ProjectPersonPredictor personPredictor;
	private final ProjectTripPredictor tripPredictor;
	private final CarPredictor predictor;

	@Inject
	public ProjectCarUtilityEstimator(ProjectModeParameters parameters, CarPredictor predictor, ProjectPersonPredictor personPredictor,
			ProjectTripPredictor tripPredictor) {
		super(parameters, predictor);

		this.parameters = parameters;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
		this.predictor = predictor;
	}

	protected double estimateTravelTimeUtility(CarVariables variables) {
		return super.estimateTravelTimeUtility(variables) //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateMonetaryCostUtility(CarVariables variables, ProjectPersonVariables personVariables) {
		return super.estimateMonetaryCostUtility(variables) //
				* EstimatorUtils.interaction(personVariables.householdIncome_MU, parameters.referenceHouseholdIncome_MU,
						parameters.lambdaCostHouseholdIncome);
	}

	protected double estimateAgeUtility(ProjectPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.projectCar.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(ProjectTripVariables variables) {
		return variables.purpose.equals("work") ? parameters.projectCar.betaWork : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarVariables variables = predictor.predictVariables(person, trip, elements);
		ProjectPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		ProjectTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables, personVariables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);

		return utility;
	}
}
