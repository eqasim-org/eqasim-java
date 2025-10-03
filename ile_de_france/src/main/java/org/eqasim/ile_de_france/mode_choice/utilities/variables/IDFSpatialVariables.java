package org.eqasim.ile_de_france.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class IDFSpatialVariables implements BaseVariables {
	public final boolean isInsideParisBoundary;
	public final boolean isCrossingParisBoundary;

	public final String originMunicipalityId;
	public final String destinationMunicipalityId;

	public IDFSpatialVariables(boolean isInsideParisBoundary, boolean isCrossingParisBoundary,
			String originMunicipalityId, String destinationMunicipalityId) {
		this.isInsideParisBoundary = isInsideParisBoundary;
		this.isCrossingParisBoundary = isCrossingParisBoundary;
		this.originMunicipalityId = originMunicipalityId;
		this.destinationMunicipalityId = destinationMunicipalityId;
	}
}
