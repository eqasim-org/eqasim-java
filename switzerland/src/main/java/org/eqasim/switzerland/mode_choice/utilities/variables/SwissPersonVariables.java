package org.eqasim.switzerland.mode_choice.utilities.variables;

import org.matsim.api.core.v01.Coord;

public class SwissPersonVariables {
	public final boolean hasGeneralSubscription;
	public final boolean hasHalbtaxSubscription;
	public final boolean hasRegionalSubscription;

	public final int statedPreferenceRegion;

	public final Coord homeLocation;

	public SwissPersonVariables(boolean hasGeneralSubscription, boolean hasHalbtaxSubscription,
			boolean hasRegionalSubscription, int statedPreferenceRegion, Coord homeLocation) {
		this.hasGeneralSubscription = hasGeneralSubscription;
		this.hasHalbtaxSubscription = hasHalbtaxSubscription;
		this.hasRegionalSubscription = hasRegionalSubscription;

		this.statedPreferenceRegion = statedPreferenceRegion;

		this.homeLocation = homeLocation;
	}
}
