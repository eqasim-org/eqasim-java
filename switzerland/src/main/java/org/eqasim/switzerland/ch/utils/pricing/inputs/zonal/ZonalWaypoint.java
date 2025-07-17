package org.eqasim.switzerland.ch.utils.pricing.inputs.zonal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ZonalWaypoint {
    private final Set<Zone> zones;

	public ZonalWaypoint(Collection<Zone> zones) {
		this.zones = new HashSet<>(zones);
	}

	public Set<Zone> getZones() {
		return zones;
	}

	@Override
	public String toString() {
		return String.format("Waypoint(%s)",
				String.join(", ", zones.stream().map(z -> z.toString()).collect(Collectors.toList())));
	}
}
