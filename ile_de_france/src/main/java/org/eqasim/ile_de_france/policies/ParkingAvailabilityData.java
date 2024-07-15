package org.eqasim.ile_de_france.policies;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * Maps parking availability to links
 * 
 * @author akramelb
 */
public class ParkingAvailabilityData{
	public static final String ATTRIBUTE = "parkingAvailability";


	private final IdMap<Link, Boolean> parkingAvailability;

	ParkingAvailabilityData(IdMap<Link, Boolean> parkingAvailability) {
		this.parkingAvailability = parkingAvailability;
	}

	public boolean getParkingAvailability(Id<Link> linkId) {
		return parkingAvailability.computeIfAbsent(linkId, id -> true).booleanValue();
	}

	static public ParkingAvailabilityData loadFromAttributes(Network network) {
		IdMap<Link, Boolean> parkingAvailability = new IdMap<>(Link.class);

		for (Link link : network.getLinks().values()) {
			Boolean value = (Boolean) link.getAttributes().getAttribute(ATTRIBUTE);

			if (value != null) {
				parkingAvailability.put(link.getId(), value);
			}
			else {
				parkingAvailability.put(link.getId(), true);
			}
		}

		return new ParkingAvailabilityData(parkingAvailability);
	}
	
}
