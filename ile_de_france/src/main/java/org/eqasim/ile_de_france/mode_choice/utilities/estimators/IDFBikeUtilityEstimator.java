package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class IDFBikeUtilityEstimator extends BikeUtilityEstimator {
	private final IDFModeParameters parameters;
	private final IDFSpatialPredictor spatialPredictor;

	@Inject
	public IDFBikeUtilityEstimator(IDFModeParameters parameters, IDFSpatialPredictor spatialPredictor,
			PersonPredictor personPredictor, BikePredictor bikePredictor) {
		super(parameters, personPredictor, bikePredictor);

		this.parameters = parameters;
		this.spatialPredictor = spatialPredictor;
	}

	protected double estimateUrbanUtility(IDFSpatialVariables variables) {
		double utility = 0.0;

		if (variables.hasUrbanOrigin && variables.hasUrbanDestination) {
			utility += parameters.idfBike.betaInsideUrbanArea;
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
