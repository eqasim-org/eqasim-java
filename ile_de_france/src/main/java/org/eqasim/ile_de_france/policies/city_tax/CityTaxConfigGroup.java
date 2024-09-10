package org.eqasim.ile_de_france.policies.city_tax;

import org.eqasim.ile_de_france.policies.PolicyConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.Parameter;

public class CityTaxConfigGroup extends PolicyConfigGroup {
	public CityTaxConfigGroup() {
		super(CityTaxPolicyFactory.POLICY_NAME);
	}

	@Parameter
	public double tax_EUR = 0.0;

	@Parameter
	public String perimetersPath;
}
