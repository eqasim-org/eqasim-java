package org.eqasim.core.simulation.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class BikeUtilityEstimator implements UtilityEstimator {
	private final ModeParameters parameters;
	private final BikePredictor bikePredictor;
	private final PersonPredictor personPredictor;

	@Inject
	public BikeUtilityEstimator(ModeParameters parameters, PersonPredictor personPredictor,
			BikePredictor bikePredictor) {
		this.parameters = parameters;
		this.bikePredictor = bikePredictor;
		this.personPredictor = personPredictor;
	}

	protected double estimateConstantUtility() {
		return parameters.bike.alpha_u;
	}

	protected double estimateTravelTimeUtility(BikeVariables variables) {
		return parameters.bike.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateAgeOver18Utility(PersonVariables variables) {
		return parameters.bike.betaAgeOver18_u_a * Math.max(0.0, variables.age_a - 18);
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		PersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		BikeVariables bikeVariables = bikePredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(bikeVariables);
		utility += estimateAgeOver18Utility(personVariables);

		return utility;
	}
}
