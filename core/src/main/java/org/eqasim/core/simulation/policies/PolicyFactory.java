package org.eqasim.core.simulation.policies;

public interface PolicyFactory {
	Policy createPolicy(String name, PolicyPersonFilter personFilter);
}
