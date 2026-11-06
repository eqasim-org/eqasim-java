// Installs the native PT backend while leaving road routing, fares, and mode-choice workers intact.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

import com.google.inject.*;
import com.google.inject.name.Named;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.router.TransitRouter;

public final class RustRoutingModule extends AbstractModule {
    public static final String OPTION = "useRustRouter";
    private final RustRoutingRuntime runtime;

    public RustRoutingModule(RustRoutingRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Start optional installation and preprocessing; close the returned runtime after the
     * controller.
     */
    public static RustRoutingRuntime prepare(Scenario scenario, RustRoutingConfigGroup settings) {
        return settings.enabled ? new RustRoutingRuntime(scenario, settings) : null;
    }

    public static RustRoutingConfigGroup applyCommand(CommandLine command, Config config)
            throws CommandLine.ConfigurationException {
        var settings = ConfigUtils.addOrGetModule(config, RustRoutingConfigGroup.class);
        if (command.hasOption(OPTION)) {
            String value = command.getOptionStrict(OPTION);
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
                throw new CommandLine.ConfigurationException(
                        "--" + OPTION + " must be true or false");
            settings.enabled = Boolean.parseBoolean(value);
        }
        return settings;
    }

    @Override
    public void install() {
        bind(RustRoutingRuntime.class).toInstance(runtime);
        bind(RustPtRoutingModuleProvider.class);
        bind(TransitRouter.class).toProvider(TransitProvider.class);
        for (String mode : getConfig().transit().getTransitModes())
            addRoutingModuleBinding(mode).toProvider(RustPtRoutingModuleProvider.class);
        addControllerListenerBinding().toInstance(runtime);
        addEventHandlerBinding().toInstance(runtime);
    }

    public static void validate(Config config, RustRoutingConfigGroup settings) {
        var raptor = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
        if (!config.transit().isUseTransit())
            throw new IllegalArgumentException("Rust PT routing requires transit.useTransit=true");
        if (raptor.isUseCapacityConstraints()
                || raptor.isUseIntermodalAccessEgress()
                || raptor.isUseRangeQuery()
                || raptor.isUseModeMappingForPassengers()
                || config.network().isTimeVariantNetwork()
                || config.routing().getNetworkModes().contains("walk")
                || config.routing()
                                .getOrCreateModeRoutingParams("walk")
                                .getTeleportedModeFreespeedFactor()
                        != null)
            throw new IllegalArgumentException(
                    "Rust PT routing supports static single-query scheduled PT with teleported"
                        + " walking; capacity constraints, range/intermodal queries, passenger mode"
                        + " mappings and time-variant/network walking are unsupported");
        if (settings.coordinateWorkBudget < 0 || !Double.isFinite(settings.qualityWaitSeconds)
                || settings.qualityWaitSeconds < 0 || !Double.isFinite(settings.qualityJourneySeconds)
                || settings.qualityJourneySeconds < 0 || !Double.isFinite(settings.qualityRadiusMeters)
                || settings.qualityRadiusMeters < 0)
            throw new IllegalArgumentException("Invalid Rust coordinate work/quality policy");
        if (settings.localBoundSettlements < 0 || settings.maxBoardings < 1
                || settings.maxBoardings > 64
                || !Double.isFinite(settings.maxDurationSeconds)
                || settings.maxDurationSeconds <= 0)
            throw new IllegalArgumentException(
                    "Invalid Rust routing maxBoardings/maxDurationSeconds/localBoundSettlements");
        NativePtRouter.millis(settings.maxDurationSeconds);
        var walk = ch.sbb.matsim.routing.pt.raptor.RaptorUtils.createStaticConfig(config);
        if (!Double.isFinite(walk.getBeelineWalkSpeed()) || walk.getBeelineWalkSpeed() <= 0)
            throw new IllegalArgumentException(
                    "Rust routing requires a positive finite walking speed");
    }

    static final class TransitProvider implements Provider<TransitRouter> {
        @Inject RustRoutingRuntime runtime;

        @Override
        public TransitRouter get() {
            var adapter = new RustPtRoutingModule(runtime, null);
            return adapter::transitLegs;
        }
    }

    /** Provider remains unscoped: each MATSim routing worker owns its walk delegate. */
    public static final class RustPtRoutingModuleProvider implements Provider<RoutingModule> {
        @Inject RustRoutingRuntime runtime;

        @Inject
        @Named(TransportMode.walk)
        RoutingModule walk;

        @Override
        public RoutingModule get() {
            return new RustPtRoutingModule(runtime, walk);
        }
    }
}
