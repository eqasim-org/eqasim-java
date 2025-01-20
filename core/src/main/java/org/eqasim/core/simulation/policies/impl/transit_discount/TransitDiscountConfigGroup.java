package org.eqasim.core.simulation.policies.impl.transit_discount;

import org.eqasim.core.simulation.policies.config.PolicyConfigGroup;

public class TransitDiscountConfigGroup extends PolicyConfigGroup {
	public TransitDiscountConfigGroup() {
		super(TransitDiscountPolicyFactory.POLICY_NAME);
	}

	@Parameter
	public double priceFactor = 1.0;
}
