package org.eqasim.core.simulation.mode_choice;

import java.util.List;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * Specific home finder for the vehicle tour constraint that first tries to find
 * the "home" activity location of the agent, and then falls back to the first
 * "leisure" location of the agent. If neither "home" nor "leisure" can be
 * found, the location of the first activity is selected.
 */
public class EqasimHomeFinder implements HomeFinder {
	@Override
	public Id<? extends BasicLocation> getHomeLocationId(List<DiscreteModeChoiceTrip> trips) {
		if (trips.size() == 0) {
			return null;
		}

		Id<? extends BasicLocation> leisureLocationId = null;

		for (DiscreteModeChoiceTrip trip : trips) {
			if (trip.getOriginActivity().getType().equals("home")) {
				return LocationUtils.getLocationId(trip.getOriginActivity());
			}

			if (trip.getDestinationActivity().getType().equals("home")) {
				return LocationUtils.getLocationId(trip.getDestinationActivity());
			}

			if (leisureLocationId == null && trip.getOriginActivity().getType().equals("leisure")) {
				leisureLocationId = LocationUtils.getLocationId(trip.getOriginActivity());
			}

			if (leisureLocationId == null && trip.getDestinationActivity().getType().equals("leisure")) {
				leisureLocationId = LocationUtils.getLocationId(trip.getDestinationActivity());
			}
		}

		if (leisureLocationId != null) {
			return leisureLocationId;
		}

		return LocationUtils.getLocationId(trips.get(0).getOriginActivity());
	}
}
