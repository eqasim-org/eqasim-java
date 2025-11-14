package org.eqasim.switzerland.ch.utils.pricing.inputs;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Zone {
    private final String zoneId;
	private final Authority authority;
	private final Collection<String> stopIds;

	public Zone(Authority authority, String zoneId, Collection<String> stopIds) {
		this.zoneId = zoneId;
		this.authority = authority;
		this.stopIds = stopIds;
	}

	public Authority getAuthority() {
		return authority;
	}

	public String getZoneId() {
		return zoneId;
	}

	public Collection<String> getStopIds() {
		return stopIds;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Zone) {
			Zone otherZone = (Zone) other;
			return otherZone.zoneId == zoneId && otherZone.authority.equals(authority);
		}

		return false;
	}

	@Override
	public String toString() {
		return String.format("%s:%s", authority.toString(), zoneId);
	}

	static public class Builder {
		private final Authority authority;
		private final String zoneId;
		private final Set<String> stopIds = new HashSet<>();

		public Builder(Authority authority, String zoneId) {
			this.authority = authority;
			this.zoneId    = zoneId;
		}

		public void addStopId(String stopId) {
			if (!stopIds.contains(stopId)) {
				stopIds.add(stopId);
			}
		}

		public Zone build() {
			return new Zone(authority, zoneId, stopIds);
		}
	}
    
}
