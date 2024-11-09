package org.eqasim.ile_de_france.policies.transit_discount;

import org.eqasim.ile_de_france.policies.PolicyConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.Parameter;

public class TransitDiscountConfigGroup extends PolicyConfigGroup {
	public TransitDiscountConfigGroup() {
		super(TransitDiscountPolicyFactory.POLICY_NAME);
	}

	@Parameter
	public double priceFactor = 1.0;
}
