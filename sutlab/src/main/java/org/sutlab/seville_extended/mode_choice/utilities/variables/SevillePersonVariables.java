package org.sutlab.seville_extended.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class SevillePersonVariables implements BaseVariables {
	public final boolean hasSubscription;
	public final boolean hasDrivingPermit;
	public final boolean isFemale;
	public final boolean isOver65;

	public SevillePersonVariables(boolean hasSubscription, boolean hasDrivingPermit, boolean isFemale,
			boolean isOver65) {
		this.hasSubscription = hasSubscription;
		this.hasDrivingPermit = hasDrivingPermit;
		this.isFemale = isFemale;
		this.isOver65 = isOver65;
	}
}
