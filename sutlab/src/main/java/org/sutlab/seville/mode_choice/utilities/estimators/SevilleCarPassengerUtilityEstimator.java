package org.sutlab.seville.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.sutlab.seville.mode_choice.parameters.SevilleModeParameters;
import org.sutlab.seville.mode_choice.utilities.predictors.SevilleCarPassengerPredictor;
import org.sutlab.seville.mode_choice.utilities.predictors.SevillePersonPredictor;
import org.sutlab.seville.mode_choice.utilities.variables.SevilleCarPassengerVariables;
import org.sutlab.seville.mode_choice.utilities.variables.SevillePersonVariables;

import java.util.List;

public class SevilleCarPassengerUtilityEstimator implements UtilityEstimator {
	private final SevilleModeParameters parameters;
	private final SevilleCarPassengerPredictor predictor;
	private final SevillePersonPredictor personPredictor;

	@Inject
	public SevilleCarPassengerUtilityEstimator(SevilleModeParameters parameters, SevilleCarPassengerPredictor predictor,
                                               SevillePersonPredictor personPredictor) {
		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
	}

	protected double estimateConstantUtility() {
		return parameters.carPassenger.alpha_u;
	}

	protected double estimateTravelTimeUtility(SevilleCarPassengerVariables variables) {
		return parameters.carPassenger.betaInVehicleTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateAccessEgressTimeUtility(SevilleCarPassengerVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateDrivingPermit(SevillePersonVariables variables) {
		return variables.hasDrivingPermit ? parameters.sevilleCarPassenger.betaDrivingPermit_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SevilleCarPassengerVariables variables = predictor.predictVariables(person, trip, elements);
		SevillePersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateDrivingPermit(personVariables);


		return utility;
	}

}