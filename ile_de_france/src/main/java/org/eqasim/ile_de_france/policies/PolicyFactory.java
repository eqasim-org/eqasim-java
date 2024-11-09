package org.eqasim.ile_de_france.policies;

public interface PolicyFactory {
	Policy createPolicy(String name, PolicyPersonFilter personFilter);
}
