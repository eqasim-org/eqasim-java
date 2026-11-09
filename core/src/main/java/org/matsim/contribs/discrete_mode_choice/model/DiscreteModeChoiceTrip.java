package org.matsim.contribs.discrete_mode_choice.model;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * This class represents an agent's trip. It contains structural information
 * such as the origin and destination activity and the mode that has initially
 * been used to cover the trip.
 *
 * Local compatibility override for MATSim 2026.0-2025w33. Only the trip-key
 * hash calculation differs from that dependency; the public API and reference
 * equality remain unchanged. Recheck this
 * override when upgrading MATSim.
 *
 * @author sebhoerl
 */
public final class DiscreteModeChoiceTrip {
	private final Activity originActivity;
	private final Activity destinationActivity;
	private final String initialMode;
	private OptionalTime departureTime = OptionalTime.undefined();
	private final List<? extends PlanElement> initialElements;
	private final Attributes tripAttributes;

	private final int hashCode;
	private final int index;

	public DiscreteModeChoiceTrip(Activity originActivity, Activity destinationActivity, String initialMode,
			List<? extends PlanElement> initialElements, int personHash, int tripHash, int index, Attributes tripAttributes) {
		this.originActivity = originActivity;
		this.destinationActivity = destinationActivity;
		this.initialMode = initialMode;
		this.initialElements = initialElements;
		this.index = index;
		this.tripAttributes = tripAttributes;

		// Both inputs are ints: shifting by 32 is shifting by zero in Java.
		// The dependency's long-style folding therefore produced hash 12 for
		// every trip, making CachedTripEstimator lookups collision-heavy.
		// Mix the int inputs directly; retain the existing reference equality
		// so distinct trip objects never share an estimated result by accident.
		int hashCode = 12;
		hashCode = 37 * hashCode + personHash;
		hashCode = 37 * hashCode + tripHash;
		this.hashCode = hashCode;
	}

	public Activity getOriginActivity() {
		return originActivity;
	}

	public Activity getDestinationActivity() {
		return destinationActivity;
	}

	public double getDepartureTime() {
		return departureTime.seconds();
	}

	public void setDepartureTime(double departureTime) {
		this.departureTime = OptionalTime.defined(departureTime);
	}

	public String getInitialMode() {
		return initialMode;
	}

	public List<? extends PlanElement> getInitialElements() {
		return initialElements;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	public int getIndex() {
		return index;
	}

	public Attributes getTripAttributes() {
		return tripAttributes;
	}
}
