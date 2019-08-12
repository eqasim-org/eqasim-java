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

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class BikeEstimator implements UtilityEstimator {
	protected final BikePredictor bikePredictor;
	protected final PersonPredictor personPredictor;
	protected final ModeParameters parameters;

	@Inject
	public BikeEstimator(PersonPredictor personPredictor, BikePredictor bikePredictor,
			ModeParameters parameters) {
		this.parameters = parameters;
		this.bikePredictor = bikePredictor;
		this.personPredictor = personPredictor;
	}

	protected double predictConstantUtility() {
		return parameters.bike.alpha_u;
	}

	protected double predictTravelTimeUtility(BikeVariables variables) {
		return parameters.bike.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double predictAgeUtility(PersonVariables variables) {
		return parameters.bike.betaAgeOver18_u_a * Math.max(0.0, variables.age_a - 18);
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = 0.0;

		PersonVariables personVariables = personPredictor.predictVariables(person);
		BikeVariables bikeVariables = bikePredictor.predictVariables(elements);

		utility += predictConstantUtility();
		utility += predictTravelTimeUtility(bikeVariables);
		utility += predictAgeUtility(personVariables);

		return utility;
	}
}
