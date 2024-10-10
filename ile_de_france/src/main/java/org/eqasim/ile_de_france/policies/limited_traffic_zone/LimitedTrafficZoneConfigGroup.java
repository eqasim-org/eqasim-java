package org.eqasim.ile_de_france.policies.limited_traffic_zone;

import org.eqasim.ile_de_france.policies.PolicyConfigGroup;

public class LimitedTrafficZoneConfigGroup extends PolicyConfigGroup {
	public LimitedTrafficZoneConfigGroup() {
		super(LimitedTrafficZonePolicyFactory.POLICY_NAME);
	}

	@Parameter
	@Comment("Path to a GeoPackage file containing polygons that cover (from and to node) the links that are part of the limited traffic zone")
	public String perimetersPath = "";

	@Parameter
	@Comment("Alternative: Path to a file containing one link id per line. Those links are tagged as being inside of the zone.")
	public String linkListPath = "";
}
