package org.eqasim.projects.astra16.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class AstraTripVariables implements BaseVariables {
	public final boolean isWork;

	public AstraTripVariables(boolean isWork) {
		this.isWork = isWork;
	}
}
