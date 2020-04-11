package org.eqasim.examples.zurich_adpt.mode_choice.constraints;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zone;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zones;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraintFactory;

public class AdPTConstraint extends AbstractTripConstraint {
	public static final String ADPT_MODE = "adpt";
	private Zones zones;

	public AdPTConstraint(Zones zones) {
		this.zones = zones;
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {

		if (mode.equals(ADPT_MODE)) {
			boolean foundStart = false;
			boolean foundEnd = false;
			String startZone = null;
			String endZone = null;
			Coord startCoord = trip.getOriginActivity().getCoord();

			Coord endCoord = trip.getDestinationActivity().getCoord();

			Map<String, Zone> mapZones = this.zones.getZones();

			for (Zone zone : mapZones.values()) {

				if (zone.containsCoordinate(startCoord)) {
					foundStart = true;
					startZone = zone.code;
				}
				if (zone.containsCoordinate(endCoord)) {
					foundEnd = true;
					endZone = zone.code;
				}
				if (foundStart && foundEnd)
					break;
			}
			return ((foundStart && foundEnd) && (!startZone.equals(endZone)));

		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		private Zones zones;

		@Inject
		public Factory(Zones zones) {
			this.zones = zones;
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new AdPTConstraint(zones);
		}
	}
}
