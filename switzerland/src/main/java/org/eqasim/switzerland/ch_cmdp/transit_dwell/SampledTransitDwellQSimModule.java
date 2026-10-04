package org.eqasim.switzerland.ch_cmdp.transit_dwell;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;

/** Install with Controler.addOverridingQSimModule, after configuring the transit engine. */
public final class SampledTransitDwellQSimModule extends AbstractQSimModule {
    @Override
    protected void configureQSim() {
        SampledTransitDwellConfigGroup config = ConfigUtils.addOrGetModule(
                getConfig(), SampledTransitDwellConfigGroup.class);
        if (config.isEnabled() && getConfig().transit().isUseTransit()
                && getConfig().transit().isUsingTransitInMobsim()) {
            double sampleSize = EqasimConfigGroup.get(getConfig()).getSampleSize();
            // An explicit QSim binding also reaches the SBB engine's injected factory.
            bind(TransitStopHandlerFactory.class).toInstance(new SampledTransitStopHandlerFactory(sampleSize));
        }
    }
}
