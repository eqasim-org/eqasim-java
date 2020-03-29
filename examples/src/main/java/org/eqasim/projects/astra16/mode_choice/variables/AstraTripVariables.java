package org.eqasim.projects.astra16.mode_choice.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class AstraTripVariables implements BaseVariables {
	public final boolean isWork;
	public final boolean isCity;

	public AstraTripVariables(boolean isWork, boolean isCity) {
		this.isWork = isWork;
		this.isCity = isCity;
	}
}
