package org.eqasim.examples.auckland_av;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import ch.ethz.idsc.amodeus.data.ReferenceFrame;

public class AucklandReferenceFrame implements ReferenceFrame {
	@Override
	public CoordinateTransformation coords_toWGS84() {
		return new GeotoolsTransformation("EPSG:2193", "EPSG:4326");
	}

	@Override
	public CoordinateTransformation coords_fromWGS84() {
		return new GeotoolsTransformation("EPSG:4326", "EPSG:2193");
	}
}
