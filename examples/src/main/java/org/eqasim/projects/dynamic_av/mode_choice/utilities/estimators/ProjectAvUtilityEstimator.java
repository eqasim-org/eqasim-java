package org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.automated_vehicles.mode_choice.utilities.estimators.AvUtilityEstimator;
import org.eqasim.automated_vehicles.mode_choice.utilities.predictors.AvPredictor;
import org.eqasim.automated_vehicles.mode_choice.utilities.variables.AvVariables;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.projects.dynamic_av.mode_choice.ProjectAvModeParameters;
import org.eqasim.projects.dynamic_av.mode_choice.ProjectModeParameters;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectPersonPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectTripPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectPersonVariables;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class ProjectAvUtilityEstimator extends AvUtilityEstimator {
	private final ProjectModeParameters generalParameters;
	private final ProjectAvModeParameters avParameters;
	private final ProjectPersonPredictor personPredictor;
	private final ProjectTripPredictor tripPredictor;
	private final AvPredictor predictor;

	@Inject
	public ProjectAvUtilityEstimator(ProjectModeParameters generalParameters, ProjectAvModeParameters avParameters,
			AvPredictor predictor, ProjectPersonPredictor personPredictor, ProjectTripPredictor tripPredictor) {
		super(generalParameters, avParameters, predictor);
		this.generalParameters = generalParameters;
		this.avParameters = avParameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateTravelTimeUtility(AvVariables variables) {
		return super.estimateTravelTimeUtility(variables) //
				* EstimatorUtils.interaction(variables.euclideanDistance_km,
						generalParameters.referenceEuclideanDistance_km,
						generalParameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateMonetaryCostUtility(AvVariables variables, ProjectPersonVariables personVariables) {
		return super.estimateMonetaryCostUtility(variables) //
				* EstimatorUtils.interaction(personVariables.householdIncome_MU,
						generalParameters.referenceHouseholdIncome_MU, generalParameters.lambdaCostHouseholdIncome);
	}

	protected double estimateAgeUtility(ProjectPersonVariables variables) {
		return variables.age_a >= 60 ? avParameters.project.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(ProjectTripVariables variables) {
		return variables.purpose.equals("work") ? avParameters.project.betaWork : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		AvVariables variables = predictor.predict(person, trip, elements);
		ProjectPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		ProjectTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateWaitingTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables, personVariables);
		utility += estimateAccessEgressTimeUtility(variables);

		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);

		return utility;
	}
}
