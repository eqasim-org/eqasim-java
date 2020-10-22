package org.eqasim.projects.astra16.mode_choice.estimators;

import java.util.List;

import org.eqasim.automated_vehicles.mode_choice.utilities.estimators.AvUtilityEstimator;
import org.eqasim.automated_vehicles.mode_choice.utilities.variables.AvVariables;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.projects.astra16.mode_choice.AstraAvModeParameters;
import org.eqasim.projects.astra16.mode_choice.AstraModeParameters;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraAvPredictor;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraPersonPredictor;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraTripPredictor;
import org.eqasim.projects.astra16.mode_choice.variables.AstraPersonVariables;
import org.eqasim.projects.astra16.mode_choice.variables.AstraTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class AstraAvUtilityEstimator extends AvUtilityEstimator {
	static public final String NAME = "AstraAvEstimator";

	private final AstraModeParameters generalParameters;
	private final AstraAvModeParameters avParameters;
	private final AstraPersonPredictor personPredictor;
	private final AstraTripPredictor tripPredictor;
	private final AstraAvPredictor predictor;

	@Inject
	public AstraAvUtilityEstimator(AstraModeParameters generalParameters, AstraAvModeParameters avParameters,
			AstraAvPredictor predictor, AstraPersonPredictor personPredictor, AstraTripPredictor tripPredictor) {
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

	protected double estimateMonetaryCostUtility(AvVariables variables, AstraPersonVariables personVariables) {
		return super.estimateMonetaryCostUtility(variables) //
				* EstimatorUtils.interaction(personVariables.householdIncome_MU,
						generalParameters.referenceHouseholdIncome_MU, generalParameters.lambdaCostHouseholdIncome);
	}

	protected double estimateAgeUtility(AstraPersonVariables variables) {
		return variables.age_a >= 60 ? avParameters.project.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(AstraTripVariables variables) {
		return variables.isWork ? avParameters.project.betaWork : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		AvVariables variables = predictor.predict(person, trip, elements);
		AstraPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		AstraTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

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
