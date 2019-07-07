package org.eqasim.core.simulation.mode_choice;

import java.util.List;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;

import ch.ethz.matsim.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/**
 * Using this home finder, vehicle constraints will always assume that the
 * vehicle starts at the first activity of the tour and must be moved to the
 * last one.
 * 
 * @author sebhoerl
 */
public class UniversalHomeFinder implements HomeFinder {
	@Override
	public Id<? extends BasicLocation> getHomeLocationId(List<DiscreteModeChoiceTrip> trips) {
		return null;
	}
}
