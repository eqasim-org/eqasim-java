package org.eqasim.switzerland.mode_choice.utilities.variables;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.matsim.api.core.v01.Coord;

public class SwissPersonVariables extends PersonVariables {
	public final Coord homeLocation;

	public final boolean hasGeneralSubscription;
	public final boolean hasHalbtaxSubscription;
	public final boolean hasRegionalSubscription;

	public final int statedPreferenceRegion;

	public final boolean isFemale;
	public final int age;

	public SwissPersonVariables(PersonVariables delegate,Coord homeLocation, boolean hasGeneralSubscription,
			boolean hasHalbtaxSubscription, boolean hasRegionalSubscription, int statedPreferenceRegion,
								boolean isFemale, int age) {

		super(delegate.age_a);

		this.homeLocation = homeLocation;
		this.hasGeneralSubscription = hasGeneralSubscription;
		this.hasHalbtaxSubscription = hasHalbtaxSubscription;
		this.hasRegionalSubscription = hasRegionalSubscription;

		this.statedPreferenceRegion = statedPreferenceRegion;
		//new
		this.isFemale =isFemale;
		this.age=age;
	}
}
