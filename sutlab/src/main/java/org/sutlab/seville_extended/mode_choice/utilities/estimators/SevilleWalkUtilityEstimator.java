package org.sutlab.seville_extended.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.sutlab.seville_extended.mode_choice.parameters.SevilleModeParameters;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevillePersonPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevilleTripPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevillePersonVariables;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevilleTripVariables;

import java.util.List;

public class SevilleWalkUtilityEstimator implements UtilityEstimator {
	private final SevilleModeParameters parameters;
	private final WalkPredictor predictor;
	private final SevillePersonPredictor personPredictor;
	private final SevilleTripPredictor tripPredictor;

	@Inject
	public SevilleWalkUtilityEstimator(SevilleModeParameters parameters, WalkPredictor predictor,
									   SevillePersonPredictor personPredictor, SevilleTripPredictor tripPredictor) {
		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateConstantUtility() {
		return parameters.walk.alpha_u;
	}

	// Weight of a minute changes with the crow-fly distance: (d / referenceDistance) ^ lambda
	protected double estimateTravelTimeUtility(WalkVariables variables, SevilleTripVariables tripVariables) {
		return parameters.walk.betaTravelTime_u_min //
				* EstimatorUtils.interaction(tripVariables.euclideanDistance_km,
						parameters.referenceEuclideanDistance_km, parameters.sevilleWalk.lambdaTravelTimeEuclideanDistance) //
				* variables.travelTime_min;
	}

	protected double estimatePurposeUtility(SevilleTripVariables variables) {
		double utility = 0.0;
		utility += variables.isWorkTrip ? parameters.sevilleWalk.betaWorkTrip_u : 0.0;
		utility += variables.isEducationTrip ? parameters.sevilleWalk.betaEducationTrip_u : 0.0;
		return utility;
	}

	protected double estimatePersonUtility(SevillePersonVariables variables) {
		double utility = 0.0;
		utility += variables.isFemale ? parameters.sevilleWalk.betaFemale_u : 0.0;
		utility += variables.isOver65 ? parameters.sevilleWalk.betaOver65_u : 0.0;
		return utility;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		WalkVariables variables = predictor.predictVariables(person, trip, elements);
		SevillePersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		SevilleTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables, tripVariables);
		utility += estimatePurposeUtility(tripVariables);
		utility += estimatePersonUtility(personVariables);

		return utility;
	}
}
