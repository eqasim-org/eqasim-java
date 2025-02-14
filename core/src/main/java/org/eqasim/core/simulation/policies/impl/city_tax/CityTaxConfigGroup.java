package org.eqasim.core.simulation.policies.impl.city_tax;

import org.eqasim.core.simulation.policies.config.PolicyConfigGroup;

public class CityTaxConfigGroup extends PolicyConfigGroup {
	public CityTaxConfigGroup() {
		super(CityTaxPolicyFactory.POLICY_NAME);
	}

	@Parameter
	public double tax_EUR = 0.0;

	@Parameter
	public String perimetersPath;
}
