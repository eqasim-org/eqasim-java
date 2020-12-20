package org.eqasim.odyssee;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class RoutingResult {
	public final String personId;
	public final String officeId;

	public final double carTravelTime;
	public final double carDistance;

	public final double ptTravelTime;
	public final double ptDistance;

	public final Set<Id<Link>> linkIds = new HashSet<>();

	public RoutingResult(String personId, String officeId, double carTravelTime, double carDistance,
			double ptTravelTime, double ptDistance, Set<Id<Link>> linkIds) {
		this.personId = personId;
		this.officeId = officeId;
		this.carTravelTime = carTravelTime;
		this.carDistance = carDistance;
		this.ptTravelTime = ptTravelTime;
		this.ptDistance = ptDistance;
		this.linkIds.addAll(linkIds);
	}
}
