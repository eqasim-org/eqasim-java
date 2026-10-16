package org.eqasim.core.components.travel_time;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelTime;

/** Install before mode-specific overrides (bike, VDF, etc.), so they retain precedence. */
public final class FloatTravelTimeModule extends AbstractModule {
    public static final String OPTION = "float-travel-times";

    public static boolean isEnabled(CommandLine command) throws CommandLine.ConfigurationException {
        String value = command.getOption(OPTION).orElse("true");
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new CommandLine.ConfigurationException("--" + OPTION + " must be true or false: " + value);
    }

    @Override public void install() {
        var settings = getConfig().travelTimeCalculator();
        if (settings.getSeparateModes() && settings.isCalculateLinkToLinkTravelTimes()) {
            throw new IllegalArgumentException("Separate modes with link-to-link travel times are unsupported by MATSim");
        }
        bind(Registry.class).in(Singleton.class);
        addControllerListenerBinding().to(Registry.class);
        if (settings.isCalculateLinkTravelTimes()) {
            for (String mode : getConfig().routing().getNetworkModes()) {
                if (!settings.getSeparateModes() || settings.getAnalyzedModes().contains(mode)) {
                    addTravelTimeBinding(mode).toProvider(new com.google.inject.Provider<TravelTime>() {
                        @Inject Registry registry;
                        @Override public TravelTime get() { return registry.get(mode).getLinkTravelTimes(); }
                    }).in(Singleton.class);
                }
            }
        }
        if (settings.isCalculateLinkToLinkTravelTimes()) {
            bind(LinkToLinkTravelTime.class).toProvider(new com.google.inject.Provider<LinkToLinkTravelTime>() {
                @Inject Registry registry;
                @Override public LinkToLinkTravelTime get() { return registry.get("").getLinkToLinkTravelTimes(); }
            }).in(Singleton.class);
        }
    }

    /** Collectors are lazy: overridden bike/VDF bindings never allocate an unused table. */
    @Singleton
    public static final class Registry implements AfterMobsimListener {
        private final Network network;
        private final Config config;
        private final EventsManager events;
        private final Map<String, FloatTravelTimeCalculator> collectors = new LinkedHashMap<>();

        @Inject public Registry(Network network, Config config, EventsManager events) {
            this.network = network;
            this.config = config;
            this.events = events;
        }

        public synchronized FloatTravelTimeCalculator get(String mode) {
            var settings = config.travelTimeCalculator();
            String key = settings.getSeparateModes() ? mode : "all";
            return collectors.computeIfAbsent(key, ignored -> {
                var collector = new FloatTravelTimeCalculator(network, settings,
                        settings.getSeparateModes() || settings.isFilterModes(),
                        settings.getSeparateModes() ? Set.of(mode) : settings.getAnalyzedModes(),
                        config.qsim().getEndTime().orElse(Double.POSITIVE_INFINITY));
                events.addHandler(collector);
                return collector;
            });
        }

        @Override public synchronized void notifyAfterMobsim(AfterMobsimEvent event) {
            collectors.forEach((mode, collector) -> {
                collector.prepareForRouting();
                org.apache.logging.log4j.LogManager.getLogger(FloatTravelTimeModule.class)
                        .info("Shared float travel times [{}]: {}", mode, collector.getStorageStatistics());
            });
        }

        public synchronized int size() { return collectors.size(); }
    }
}
