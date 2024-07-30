package org.eqasim.core.simulation.modes.feeder_drt.mode_choice;

import com.google.inject.Provider;
import com.google.inject.Provides;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.epsilon.EpsilonAdapter;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.modes.feeder_drt.mode_choice.constraints.FeederDrtConstraint;
import org.eqasim.core.simulation.modes.feeder_drt.config.MultiModeFeederDrtConfigGroup;
import org.eqasim.core.simulation.modes.feeder_drt.mode_choice.utilities.estimator.DefaultFeederDrtUtilityEstimator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EqasimFeederDrtModeChoiceModule extends AbstractEqasimExtension {

    public static final String FEEDER_DRT_ESTIMATOR_NAME = "DefaultFeederDrtUtilityEstimator";

    @Override
    protected void installEqasimExtension() {
        bindUtilityEstimator(FEEDER_DRT_ESTIMATOR_NAME).to(DefaultFeederDrtUtilityEstimator.class);
        bindTripConstraintFactory(FeederDrtConstraint.NAME).to(FeederDrtConstraint.Factory.class).asEagerSingleton();
    }

    @Provides
    public DefaultFeederDrtUtilityEstimator provideDefaultFeederDrtUtilityEstimator(EqasimConfigGroup eqasimConfigGroup, MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup, Map<String, Provider<UtilityEstimator>> utilityEstimatorProviders) {
        Map<String, UtilityEstimator> ptEstimators = multiModeFeederDrtConfigGroup.getModalElements().stream().collect(Collectors.toMap(cfg -> cfg.mode, cfg -> utilityEstimatorProviders.get(eqasimConfigGroup.getEstimators().get(cfg.ptModeName)).get()));
        Map<String, UtilityEstimator> drtEstimators = multiModeFeederDrtConfigGroup.getModalElements().stream().collect(Collectors.toMap(cfg -> cfg.mode, cfg -> utilityEstimatorProviders.get(eqasimConfigGroup.getEstimators().get(cfg.accessEgressModeName)).get()));
        // When we use the Epsilon adapter, we do not want to sum the pseudo-random errors of each sub-mode but rather only use one pseudo-error specific to the current mode
        for(Map<String, UtilityEstimator> map: List.of(ptEstimators, drtEstimators)) {
            for(String mode: map.keySet()) {
                if(map.get(mode) instanceof EpsilonAdapter epsilonAdapter) {
                    map.put(mode, epsilonAdapter.getDelegate());
                }
            }
        }
        return new DefaultFeederDrtUtilityEstimator(ptEstimators, drtEstimators);
    }

    @Provides
    public FeederDrtConstraint.Factory provideFeederDrtConstraintFactory(MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup) {
        Map<String, String> basePtModes = multiModeFeederDrtConfigGroup.getModalElements().stream().collect(Collectors.toMap(cfg -> cfg.mode, cfg -> cfg.ptModeName));
        Map<String, String> baseDrtModes = multiModeFeederDrtConfigGroup.getModalElements().stream().collect(Collectors.toMap(cfg -> cfg.mode, cfg -> cfg.accessEgressModeName));
        return new FeederDrtConstraint.Factory(basePtModes, baseDrtModes);
    }
}
