package org.eqasim.core.simulation.policies.impl.discount;

import org.eqasim.core.simulation.policies.config.PolicyConfigGroup;

public class DiscountConfigGroup extends PolicyConfigGroup {
	public DiscountConfigGroup() {
		super(DiscountPolicyFactory.POLICY_NAME);
	}

	@Parameter
	public String mode = "pt";

	@Parameter
	public double priceFactor = 1.0;
}
