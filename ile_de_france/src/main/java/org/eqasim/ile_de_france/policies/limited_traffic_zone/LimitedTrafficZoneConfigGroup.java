package org.eqasim.ile_de_france.policies.limited_traffic_zone;

import org.eqasim.ile_de_france.policies.PolicyConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.Parameter;

public class LimitedTrafficZoneConfigGroup extends PolicyConfigGroup {
	public LimitedTrafficZoneConfigGroup() {
		super(LimitedTrafficZonePolicyFactory.POLICY_NAME);
	}

	@Parameter
	public String perimetersPath;
}
