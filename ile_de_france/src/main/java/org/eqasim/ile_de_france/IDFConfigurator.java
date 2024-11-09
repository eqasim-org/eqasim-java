package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.ile_de_france.policies.PoliciesConfigGroup;

public class IDFConfigurator extends EqasimConfigurator {
	public IDFConfigurator() {
		super();

		registerConfigGroup(new PoliciesConfigGroup(), true);
	}
}
