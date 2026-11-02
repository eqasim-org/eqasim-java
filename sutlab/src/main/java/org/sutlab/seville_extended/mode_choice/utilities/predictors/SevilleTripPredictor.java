package org.sutlab.seville_extended.mode_choice.utilities.predictors;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.sutlab.seville_extended.mode_choice.SevilleZones;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevilleTripVariables;

import java.util.List;

public class SevilleTripPredictor extends CachedVariablePredictor<SevilleTripVariables> {
	private final SevilleZones zones;

	@Inject
	public SevilleTripPredictor(SevilleZones zones) {
		this.zones = zones;
	}

	@Override
	protected SevilleTripVariables predict(Person person, DiscreteModeChoiceTrip trip,
										   List<? extends PlanElement> elements) {
		Activity originActivity = trip.getOriginActivity();
		Activity destinationActivity = trip.getDestinationActivity();

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		// A trip with one end at home takes the activity at its other end, a trip with neither end at home has none
		String purpose = null;

		if (originActivity.getType().equals("home")) {
			purpose = destinationActivity.getType();
		} else if (destinationActivity.getType().equals("home")) {
			purpose = originActivity.getType();
		}

		boolean isWorkTrip = "work".equals(purpose);
		boolean isEducationTrip = "education".equals(purpose);

		// Destination in the parking zone and the person does not live there
		boolean isParkingZoneDestination = false;

		if (zones.isInsideParkingZone(destinationActivity.getCoord())) {
			Coord homeCoord = SevillePredictorUtils.getHomeCoord(person);
			isParkingZoneDestination = homeCoord == null || !zones.isInsideParkingZone(homeCoord);
		}

		// Origin or destination outside the municipality of Sevilla
		boolean isOutsideBoundary = zones.isOutsideMunicipality(originActivity.getCoord())
				|| zones.isOutsideMunicipality(destinationActivity.getCoord());

		return new SevilleTripVariables(euclideanDistance_km, isWorkTrip, isEducationTrip, isParkingZoneDestination,
				isOutsideBoundary);
	}
}
