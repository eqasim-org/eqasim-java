package org.eqasim.ile_de_france.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class IDFPtVariables implements BaseVariables {
	public final boolean isOnlyBus;
	public final boolean hasOnlySubwayAndBus;
	public final boolean isWithinParis;

	public IDFPtVariables(boolean isOnlyBus, boolean hasOnlySubwayAndBus, boolean isWithinParis) {
		this.isOnlyBus = isOnlyBus;
		this.hasOnlySubwayAndBus = hasOnlySubwayAndBus;
		this.isWithinParis = isWithinParis;
	}
}
