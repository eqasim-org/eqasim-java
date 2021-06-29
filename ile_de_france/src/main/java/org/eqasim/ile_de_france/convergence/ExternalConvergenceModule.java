package org.eqasim.ile_de_france.convergence;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.TerminationCriterion;

public class ExternalConvergenceModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(ExternalConvergenceCriterion.class);
		bind(TerminationCriterion.class).to(ExternalConvergenceCriterion.class);
	}
}
