package org.eqasim.core.simulation.modes.feeder_drt;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.modes.feeder_drt.analysis.FeederDrtAnalysisModule;
import org.eqasim.core.simulation.modes.feeder_drt.config.FeederDrtConfigGroup;
import org.eqasim.core.simulation.modes.feeder_drt.config.MultiModeFeederDrtConfigGroup;

public class MultiModeFeederDrtModule extends AbstractEqasimExtension {
    @Inject
    MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup;

    @Override
    protected void installEqasimExtension() {
        for(FeederDrtConfigGroup feederDrtConfigGroup: this.multiModeFeederDrtConfigGroup.getModalElements()) {
            install(new FeederDrtModeModule(feederDrtConfigGroup));
        }
        if(multiModeFeederDrtConfigGroup.performAnalysis) {
            install(new FeederDrtAnalysisModule());
        }
    }
}
