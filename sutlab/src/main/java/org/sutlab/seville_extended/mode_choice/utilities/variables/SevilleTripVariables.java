package org.sutlab.seville_extended.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class SevilleTripVariables implements BaseVariables {
	public final double euclideanDistance_km;

	// Piecewise crow-fly distance above each knot, zero below it
	public final double distanceAbove0_km;
	public final double distanceAbove2_km;
	public final double distanceAbove5_km;
	public final double distanceAbove10_km;

	public final boolean isWorkTrip;
	public final boolean isEducationTrip;

	public final boolean isParkingZoneDestination;
	public final boolean isOutsideBoundary;

	public SevilleTripVariables(double euclideanDistance_km, boolean isWorkTrip, boolean isEducationTrip,
			boolean isParkingZoneDestination, boolean isOutsideBoundary) {
		this.euclideanDistance_km = euclideanDistance_km;

		this.distanceAbove0_km = Math.max(0.0, euclideanDistance_km);
		this.distanceAbove2_km = Math.max(0.0, euclideanDistance_km - 2.0);
		this.distanceAbove5_km = Math.max(0.0, euclideanDistance_km - 5.0);
		this.distanceAbove10_km = Math.max(0.0, euclideanDistance_km - 10.0);

		this.isWorkTrip = isWorkTrip;
		this.isEducationTrip = isEducationTrip;
		this.isParkingZoneDestination = isParkingZoneDestination;
		this.isOutsideBoundary = isOutsideBoundary;
	}
}
