package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.UUID;

import com.google.inject.*;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorRoutingModuleProvider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.replanning.DiscreteModeChoiceStrategyProvider;

/** Install after SwitzerlandConfigurator's usual routing bindings. */
public final class PtRouteCacheModule extends AbstractModule {
    public static final String OPTION = "cache-pt-routes";

    public static void applyCommand(CommandLine command, Config config) throws CommandLine.ConfigurationException {
        var settings = ConfigUtils.addOrGetModule(config, PtRouteCacheConfigGroup.class);
        if (command.hasOption(OPTION)) {
            String value = command.getOptionStrict(OPTION);
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value))
                throw new CommandLine.ConfigurationException("--" + OPTION + " must be true or false");
            settings.enabled = Boolean.parseBoolean(value);
        }
    }

    @Override public void install() {
        var settings = ConfigUtils.addOrGetModule(getConfig(), PtRouteCacheConfigGroup.class);
        if (!settings.enabled || !getConfig().transit().isUseTransit()) return;
        validate(getConfig(), settings);
        bind(SwissRailRaptorRoutingModuleProvider.class);
        bind(PtRouteCache.class).toProvider(CacheProvider.class).in(Singleton.class);
        addControllerListenerBinding().to(PtRouteCache.class);
        addEventHandlerBinding().to(PtRouteCache.class);
        addRoutingModuleBinding("pt").toProvider(RouterProvider.class);
        if (getConfig().global().getNumberOfThreads() > 0 && getConfig().replanning().getStrategySettings().stream()
                .anyMatch(strategy -> DiscreteModeChoiceModule.STRATEGY_NAME.equals(strategy.getStrategyName()))) {
            bind(DiscreteModeChoiceStrategyProvider.class);
            addPlanStrategyBinding(DiscreteModeChoiceModule.STRATEGY_NAME).toProvider(PrefetchStrategyProvider.class);
        }
    }

    static void validate(Config config, PtRouteCacheConfigGroup settings) {
        var raptor = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
        if (settings.memoryMb < 0) throw new IllegalArgumentException("PT cache memoryMb must be nonnegative");
        if (raptor.isUseCapacityConstraints() || raptor.isUseIntermodalAccessEgress() || raptor.isUseRangeQuery()
                || config.network().isTimeVariantNetwork() || config.routing().getNetworkModes().contains("walk")
                || config.routing().getOrCreateModeRoutingParams("walk").getTeleportedModeFreespeedFactor() != null)
            throw new IllegalArgumentException("PT route caching requires static single-query timetable routing, "
                    + "without capacity constraints, intermodal access, time-variant networks or network walk routing");
    }

    static Path cacheDirectory(Config config, PtRouteCacheConfigGroup settings) {
        Path base;
        if (settings.directory == null || settings.directory.isBlank()) {
            base = Path.of(config.controller().getOutputDirectory()).resolve("pt_route_cache");
        } else {
            try { base = Path.of(ConfigGroup.getInputFileURL(config.getContext(), settings.directory).toURI()); }
            catch (URISyntaxException | IllegalArgumentException e) { throw new IllegalArgumentException("PT cache needs a local directory", e); }
        }
        return base.resolve("run-" + UUID.randomUUID());
    }

    static final class CacheProvider implements Provider<PtRouteCache> {
        @Inject Config config;
        @Override public PtRouteCache get() {
            var settings = ConfigUtils.addOrGetModule(config, PtRouteCacheConfigGroup.class);
            return new PtRouteCache(cacheDirectory(config, settings), settings.memoryMb);
        }
    }

    static final class RouterProvider implements Provider<RoutingModule> {
        @Inject SwissRailRaptorRoutingModuleProvider original;
        @Inject PtRouteCache cache;
        @Inject Scenario scenario;
        @Override public RoutingModule get() {
            return new CachingPtRoutingModule(original.get(), cache, scenario.getPopulation().getFactory());
        }
    }

    static final class PrefetchStrategyProvider implements Provider<PlanStrategy> {
        @Inject DiscreteModeChoiceStrategyProvider original;
        @Inject PtRouteCache cache;
        @Override public PlanStrategy get() { return new PtCachePrefetchingStrategy(original.get(), cache); }
    }
}
