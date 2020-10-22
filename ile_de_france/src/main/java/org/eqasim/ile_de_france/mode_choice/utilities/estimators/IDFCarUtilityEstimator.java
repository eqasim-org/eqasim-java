package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class IDFCarUtilityEstimator extends CarUtilityEstimator {
	private final IDFModeParameters parameters;
	private final IDFSpatialPredictor spatialPredictor;

	@Inject
	public IDFCarUtilityEstimator(IDFModeParameters parameters, IDFSpatialPredictor spatialPredictor,
			CarPredictor carPredictor) {
		super(parameters, carPredictor);

		this.parameters = parameters;
		this.spatialPredictor = spatialPredictor;
	}

	protected double estimateUrbanUtility(IDFSpatialVariables variables) {
		double utility = 0.0;

		if (variables.hasUrbanOrigin && variables.hasUrbanDestination) {
			utility += parameters.idfCar.betaInsideUrbanArea;
		}

		if (variables.hasUrbanOrigin || variables.hasUrbanDestination) {
			utility += parameters.idfCar.betaCrossingUrbanArea;
		}

		return utility;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		IDFSpatialVariables variables = spatialPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += super.estimateUtility(person, trip, elements);
		utility += estimateUrbanUtility(variables);

		return utility;
	}
}
