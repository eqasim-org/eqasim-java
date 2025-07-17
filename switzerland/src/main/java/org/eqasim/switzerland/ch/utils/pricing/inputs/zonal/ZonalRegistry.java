package org.eqasim.switzerland.ch.utils.pricing.inputs.zonal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ZonalRegistry {
    private final Map<String, Collection<Zone>> zonesByStopId         = new HashMap<>();
	private final Map<String, Authority> authoritiesById            = new HashMap<>();
	private final Map<Authority, Collection<Zone>> zonesByAuthority = new HashMap<>();

	public ZonalRegistry(Collection<Authority> authorities, Collection<Zone> zones) {
		for (Authority authority : authorities) {
			authoritiesById.put(authority.getId(), authority);
			zonesByAuthority.put(authority, new HashSet<>());
		}

		for (Zone zone : zones) {
			zonesByAuthority.get(zone.getAuthority()).add(zone);

			for (String stopId : zone.getStopIds()) {
				if (!zonesByStopId.containsKey(stopId)) {
					zonesByStopId.put(stopId, new HashSet<>());
				}

				zonesByStopId.get(stopId).add(zone);
			}
		}
	}

	public Authority getAuthority(String authorityId) {
		return authoritiesById.get(authorityId);
	}

	public Zone getZone(Authority authority, String zoneId) {
		for (Zone zone : zonesByAuthority.get(authority)) {
			if (zone.getZoneId() == zoneId) {
				return zone;
			}
		}

		throw new IllegalStateException();
	}

	public Collection<Zone> getZones(String stopId) {
		if (zonesByStopId.containsKey(stopId)) {
			return zonesByStopId.get(stopId);
		} else {
			return Collections.emptyList();
		}
	}

	public Collection<Zone> getZones(Authority authority) {
		return zonesByAuthority.get(authority);
	}
}
