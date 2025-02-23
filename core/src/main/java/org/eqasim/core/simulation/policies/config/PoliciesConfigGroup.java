package org.eqasim.core.simulation.policies.config;

import org.eqasim.core.simulation.policies.impl.city_tax.CityTaxConfigGroup;
import org.eqasim.core.simulation.policies.impl.city_tax.CityTaxPolicyFactory;
import org.eqasim.core.simulation.policies.impl.discount.DiscountConfigGroup;
import org.eqasim.core.simulation.policies.impl.discount.DiscountPolicyFactory;
import org.eqasim.core.simulation.policies.impl.limited_traffic_zone.LimitedTrafficZoneConfigGroup;
import org.eqasim.core.simulation.policies.impl.limited_traffic_zone.LimitedTrafficZonePolicyFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class PoliciesConfigGroup extends ReflectiveConfigGroup {
	static public final String CONFIG_NAME = "eqasim:policies";

	public PoliciesConfigGroup() {
		super(CONFIG_NAME);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		switch (type) {
		case CityTaxPolicyFactory.POLICY_NAME:
			return new CityTaxConfigGroup();
		case LimitedTrafficZonePolicyFactory.POLICY_NAME:
			return new LimitedTrafficZoneConfigGroup();
		case DiscountPolicyFactory.POLICY_NAME:
			return new DiscountConfigGroup();
		default:
			throw new IllegalStateException();
		}
	}

	static public PoliciesConfigGroup get(Config config) {
		return (PoliciesConfigGroup) config.getModules().get(CONFIG_NAME);
	}
}
