package org.eqasim.ile_de_france.policies;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class ParkingAvailabilityData{
	public static final String ATTRIBUTE = "parkingAvailability";


	private final IdMap<Link, Boolean> parkingAvailability;

	ParkingAvailabilityData(IdMap<Link, Boolean> parkingAvailability) {
		this.parkingAvailability = parkingAvailability;
	}

	public boolean getParkingAvailability(Id<Link> linkId) {
		return parkingAvailability.computeIfAbsent(linkId, id -> false).booleanValue();
	}

	static public ParkingAvailabilityData loadFromAttributes(Network network) {
		IdMap<Link, Boolean> parkingAvailability = new IdMap<>(Link.class);

		for (Link link : network.getLinks().values()) {
			Boolean value = (Boolean) link.getAttributes().getAttribute(ATTRIBUTE);

			if (value != null) {
				parkingAvailability.put(link.getId(), value);
			}
		}

		return new ParkingAvailabilityData(parkingAvailability);
	}
	
}
