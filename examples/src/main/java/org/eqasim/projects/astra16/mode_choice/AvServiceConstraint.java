package org.eqasim.projects.astra16.mode_choice;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraintFactory;

public class AvServiceConstraint extends AbstractTripConstraint {
	static public final String NAME = "AVServiceConstraint";

	private final double minimumDistance_km;

	private AvServiceConstraint(double minimumDistance_km) {
		this.minimumDistance_km = minimumDistance_km;
	}

	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		if (mode.equals(AVModule.AV_MODE)) {
			double directDistance_km = 1e-3 * CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
					trip.getDestinationActivity().getCoord());
			return directDistance_km > minimumDistance_km;
		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		private final double minimumDistance_km;

		public Factory(double minimumDistance_km) {
			this.minimumDistance_km = minimumDistance_km;
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new AvServiceConstraint(minimumDistance_km);
		}
	}
}
