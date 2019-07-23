package org.eqasim.core.location_assignment.assignment.relaxation;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public interface RelaxedLocationResult {
	List<Vector2D> getRelaxedLocations();

	boolean isConverged();
	
	int getIterations();
}
