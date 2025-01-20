package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.policies.config.PoliciesConfigGroup;

public class IDFConfigurator extends EqasimConfigurator {
	public IDFConfigurator() {
		super();

		registerConfigGroup(new PoliciesConfigGroup(), true);
	}
}
