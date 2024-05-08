package org.eqasim.core.simulation.modes.feeder_drt;

import com.google.inject.Inject;
import org.eqasim.core.simulation.modes.feeder_drt.analysis.FeederDrtAnalysisModule;
import org.eqasim.core.simulation.modes.feeder_drt.config.FeederDrtConfigGroup;
import org.eqasim.core.simulation.modes.feeder_drt.config.MultiModeFeederDrtConfigGroup;
import org.matsim.core.controler.AbstractModule;

public class MultiModeFeederDrtModule extends AbstractModule {
    @Inject
    MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup;

    @Override
    public void install() {
        for(FeederDrtConfigGroup feederDrtConfigGroup: this.multiModeFeederDrtConfigGroup.getModalElements()) {
            install(new FeederDrtModeModule(feederDrtConfigGroup));
        }
        if(multiModeFeederDrtConfigGroup.performAnalysis) {
            install(new FeederDrtAnalysisModule());
        }
    }
}
