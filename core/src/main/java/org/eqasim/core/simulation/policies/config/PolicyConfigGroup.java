package org.eqasim.core.simulation.policies.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public abstract class PolicyConfigGroup extends ReflectiveConfigGroup {
	protected PolicyConfigGroup(String name) {
		super(name);
	}

	@Parameter
	@Comment("Name of the policy for identification and analysis purposes")
	public String policyName;

	@Parameter
	@Comment("Defines whether the policy is active or not (can easily be switched via command line)")
	public boolean active = true;

	@Parameter
	@Comment("Only applies to persons that have set the given attribute as true. Preceding the expression by ! inverses the logic.")
	public String personFilter;
}
