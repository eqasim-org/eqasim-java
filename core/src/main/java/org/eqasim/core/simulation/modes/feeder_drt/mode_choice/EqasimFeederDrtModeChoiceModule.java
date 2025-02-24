package org.eqasim.core.simulation.modes.feeder_drt.mode_choice;

import java.util.Map;
import java.util.stream.Collectors;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.modes.feeder_drt.config.MultiModeFeederDrtConfigGroup;
import org.eqasim.core.simulation.modes.feeder_drt.mode_choice.constraints.FeederDrtConstraint;
import org.eqasim.core.simulation.modes.feeder_drt.mode_choice.utilities.estimator.DefaultFeederDrtUtilityEstimator;

import com.google.inject.Provider;
import com.google.inject.Provides;

public class EqasimFeederDrtModeChoiceModule extends AbstractEqasimExtension {

    public static final String FEEDER_DRT_ESTIMATOR_NAME = "DefaultFeederDrtUtilityEstimator";
    public static final String FEEDER_DRT_MODE_AVAILABILITY_WRAPPER_NAME = "feederDrt";

    @Override
    protected void installEqasimExtension() {
        bindUtilityEstimator(FEEDER_DRT_ESTIMATOR_NAME).to(DefaultFeederDrtUtilityEstimator.class);
        bindTripConstraintFactory(FeederDrtConstraint.NAME).to(FeederDrtConstraint.Factory.class).asEagerSingleton();
        bindModeAvailabilityWrapperFactory(FEEDER_DRT_MODE_AVAILABILITY_WRAPPER_NAME).toInstance(modeAvailability -> new FeederDrtModeAvailabilityWrapper(getConfig(), modeAvailability));
    }

    @Provides
    public DefaultFeederDrtUtilityEstimator provideDefaultFeederDrtUtilityEstimator(EqasimConfigGroup eqasimConfigGroup, MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup, Map<String, Provider<UtilityEstimator>> utilityEstimatorProviders) {
        Map<String, UtilityEstimator> ptEstimators = multiModeFeederDrtConfigGroup.getModalElements().stream().collect(Collectors.toMap(cfg -> cfg.mode, cfg -> utilityEstimatorProviders.get(eqasimConfigGroup.getEstimators().get(cfg.ptModeName)).get()));
        Map<String, UtilityEstimator> drtEstimators = multiModeFeederDrtConfigGroup.getModalElements().stream().collect(Collectors.toMap(cfg -> cfg.mode, cfg -> utilityEstimatorProviders.get(eqasimConfigGroup.getEstimators().get(cfg.accessEgressModeName)).get()));
        return new DefaultFeederDrtUtilityEstimator(ptEstimators, drtEstimators);
    }

    @Provides
    public FeederDrtConstraint.Factory provideFeederDrtConstraintFactory(MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup) {
        Map<String, String> basePtModes = multiModeFeederDrtConfigGroup.getModalElements().stream().collect(Collectors.toMap(cfg -> cfg.mode, cfg -> cfg.ptModeName));
        Map<String, String> baseDrtModes = multiModeFeederDrtConfigGroup.getModalElements().stream().collect(Collectors.toMap(cfg -> cfg.mode, cfg -> cfg.accessEgressModeName));
        return new FeederDrtConstraint.Factory(basePtModes, baseDrtModes);
    }
}
