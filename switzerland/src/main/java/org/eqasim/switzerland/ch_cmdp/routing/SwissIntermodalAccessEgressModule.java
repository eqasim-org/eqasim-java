package org.eqasim.switzerland.ch_cmdp.routing;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.intermodal.SwissIntermodalPTPredictor;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.SwissHomeActivityRaptorStopFinder;

public class SwissIntermodalAccessEgressModule extends AbstractModule {
	@Override
	public void install() {
		SwissRailRaptorConfigGroup raptorConfig = ConfigUtils.addOrGetModule(getConfig(),
				SwissRailRaptorConfigGroup.class);
		if (raptorConfig.isUseIntermodalAccessEgress()) {
			// SwissRailRaptor uses this component to score each candidate access/egress
			// chain. Rebinding it here lets ch_cmdp add stochastic access-mode tastes
			// without changing the upstream router implementation.
			bind(RaptorIntermodalAccessEgress.class).to(SwissStochasticIntermodalAccessEgress.class);
			bind(RaptorStopFinder.class).to(SwissHomeActivityRaptorStopFinder.class);
			bind(PtPredictor.class).to(SwissIntermodalPTPredictor.class);
		}
	}
}
