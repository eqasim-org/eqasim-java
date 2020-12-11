package org.eqasim.odyssee;

public class RoutingTask {
	public final String personId;
	public final String officeId;
	public final double originX;
	public final double originY;
	public final double destinationX;
	public final double destinationY;

	public RoutingTask(String personId, String officeId, double originX, double originY, double destinationX,
			double destinationY) {
		this.personId = personId;
		this.officeId = officeId;
		this.originX = originX;
		this.originY = originY;
		this.destinationX = destinationX;
		this.destinationY = destinationY;
	}
}
