package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class IDFSpatialPredictor extends CachedVariablePredictor<IDFSpatialVariables> {
	@Override
	protected IDFSpatialVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		String originMunicipalityId = IDFPredictorUtils.getMunicipalityId(trip.getOriginActivity());
		String destinationMunicipalityId = IDFPredictorUtils.getMunicipalityId(trip.getDestinationActivity());

		boolean isOriginInsideParis = isInsideParis(originMunicipalityId);
		boolean isDestinationInsideParis = isInsideParis(destinationMunicipalityId);

		return new IDFSpatialVariables( //
				isOriginInsideParis && isDestinationInsideParis, //
				isOriginInsideParis ^ isDestinationInsideParis, //
				originMunicipalityId, destinationMunicipalityId);
	}

	static private boolean isInsideParis(String municipalityId) {
		return municipalityId.startsWith("75");
	}
}
