package org.eqasim.switzerland.zurich.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class ZurichTripVariables implements BaseVariables {
	public final boolean isWork;
	public final boolean isCity;

	public ZurichTripVariables(boolean isWork, boolean isCity) {
		this.isWork = isWork;
		this.isCity = isCity;
	}
}