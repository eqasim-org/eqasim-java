package org.eqasim.simulation.mode_choice.utilities.variables;

import org.matsim.api.core.v01.Coord;

public class PersonVariables {
	public final Coord homeLocation;

	public final boolean hasGeneralSubscription;
	public final boolean hasHalbtaxSubscription;
	public final boolean hasRegionalSubscription;

	public final int age_a;
	public final int statedPreferenceRegion;

	public PersonVariables(Coord homeLocation, boolean hasGeneralSubscription, boolean hasHalbtaxSubscription,
			boolean hasRegionalSubscription, int age_a, int statedPreferenceRegion) {
		this.homeLocation = homeLocation;

		this.hasGeneralSubscription = hasGeneralSubscription;
		this.hasHalbtaxSubscription = hasHalbtaxSubscription;
		this.hasRegionalSubscription = hasRegionalSubscription;

		this.age_a = age_a;
		this.statedPreferenceRegion = statedPreferenceRegion;
	}
}
