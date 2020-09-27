package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Singleton;

@Singleton
public class IDFSpatialPredictor extends CachedVariablePredictor<IDFSpatialVariables> {
	@Override
	protected IDFSpatialVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		boolean hasUrbanOrigin = IDFPredictorUtils.isUrbanArea(trip.getOriginActivity());
		boolean hasUrbanDestination = IDFPredictorUtils.isUrbanArea(trip.getDestinationActivity());

		return new IDFSpatialVariables(hasUrbanOrigin, hasUrbanDestination);
	}
}
