package org.eqasim.odyssee;

public class RoutingResult {
	public final String personId;
	public final String officeId;

	public final double carTravelTime;
	public final double carDistance;

	public final double ptTravelTime;
	public final double ptDistance;

	public RoutingResult(String personId, String officeId, double carTravelTime, double carDistance,
			double ptTravelTime, double ptDistance) {
		this.personId = personId;
		this.officeId = officeId;
		this.carTravelTime = carTravelTime;
		this.carDistance = carDistance;
		this.ptTravelTime = ptTravelTime;
		this.ptDistance = ptDistance;
	}
}
