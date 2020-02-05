package org.eqasim.projects.dynamic_av.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class ProjectTripVariables implements BaseVariables {
	public final boolean isWork;

	public ProjectTripVariables(boolean isWork) {
		this.isWork = isWork;
	}
}
