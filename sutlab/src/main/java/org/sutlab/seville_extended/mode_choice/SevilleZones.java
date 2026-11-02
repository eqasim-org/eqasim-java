package org.sutlab.seville_extended.mode_choice;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.matsim.api.core.v01.Coord;

import java.util.Optional;

// Spatial zones used in the mode choice model. A zone is empty when the model does not need it.
public class SevilleZones {
	private final Optional<ScenarioExtent> municipality;
	private final Optional<ScenarioExtent> parkingZone;

	public SevilleZones(Optional<ScenarioExtent> municipality, Optional<ScenarioExtent> parkingZone) {
		this.municipality = municipality;
		this.parkingZone = parkingZone;
	}

	public boolean isOutsideMunicipality(Coord coord) {
		return municipality.isPresent() && !municipality.get().isInside(coord);
	}

	public boolean isInsideParkingZone(Coord coord) {
		return parkingZone.isPresent() && parkingZone.get().isInside(coord);
	}
}
