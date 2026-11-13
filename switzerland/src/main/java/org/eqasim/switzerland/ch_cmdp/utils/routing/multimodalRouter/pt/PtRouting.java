package org.eqasim.switzerland.ch_cmdp.utils.routing.multimodalRouter.pt;

import java.util.List;
import java.util.function.Function;
import org.eqasim.core.components.headway.HeadwayCalculator;
import org.eqasim.core.components.raptor.*;
import org.eqasim.switzerland.ch_cmdp.rustRouting.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.*;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.MatsimVehicleReader;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.*;

/** Owns shared timetable data and creates independent worker adapters for the selected PT backend. */
public final class PtRouting implements AutoCloseable {
    private final Scenario scenario;
    private final Config config;
    private final double headwayInterval;
    private final SwissRailRaptorData raptorData;
    private final RaptorParameters parameters;
    private final RustRoutingRuntime rust;

    public PtRouting(Config config, Network network, boolean useRust, double headwayInterval) {
        this.config = config;
        this.headwayInterval = headwayInterval;
        scenario = ScenarioUtils.createScenario(config);
        ((org.matsim.core.scenario.MutableScenario) scenario).setNetwork(network);
        var log = org.apache.logging.log4j.LogManager.getLogger(PtRouting.class);
        log.info("Loading PT schedule: {} (backend={})", config.transit().getTransitScheduleFile(), useRust ? "Rust" : "RAPTOR");
        new TransitScheduleReader(scenario).readURL(ConfigGroup.getInputFileURL(config.getContext(), config.transit().getTransitScheduleFile()));
        if (config.transit().getVehiclesFile() != null)
            new MatsimVehicleReader(scenario.getTransitVehicles()).readURL(ConfigGroup.getInputFileURL(config.getContext(), config.transit().getVehiclesFile()));
        parameters = !useRust || headwayInterval > 0
                ? config.getModules().containsKey(EqasimRaptorConfigGroup.GROUP_NAME)
                    ? EqasimRaptorUtils.createParameters(config, ConfigUtils.addOrGetModule(config, EqasimRaptorConfigGroup.class), schedule(), true)
                    : RaptorUtils.createParameters(config) : null;
        raptorData = parameters == null ? null : SwissRailRaptorData.create(schedule(), scenario.getTransitVehicles(),
                RaptorUtils.createStaticConfig(config), network, new OccupancyData());
        if (useRust) {
            var settings = ConfigUtils.addOrGetModule(config, RustRoutingConfigGroup.class);
            settings.enabled = true;
            rust = new RustRoutingRuntime(scenario, settings);
            try { rust.awaitReady(); }
            catch (RuntimeException | Error e) { rust.close(); throw e; }
        } else rust = null;
        log.info("PT timetable ready: backend={}, lines={}, stops={}, headway window={} s", useRust ? "Rust" : "RAPTOR",
                schedule().getTransitLines().size(), schedule().getFacilities().size(), headwayInterval);
    }

    public TransitSchedule schedule() { return scenario.getTransitSchedule(); }
    public record Worker(Function<RoutingRequest, List<? extends PlanElement>> route, HeadwayCalculator headway) {}

    @SuppressWarnings("null")
    public Worker createWorker() {
        SwissRailRaptor raptor = null;
        if (raptorData != null) {
            var builder = new SwissRailRaptor.Builder(raptorData, config).with(person -> parameters);
            var settings = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
            if (!settings.getModeToModeTransferPenaltyParameterSets().isEmpty()) builder.with(new ModeSpecificTransferCostCalculator());
            if (settings.isUseRangeQuery()) builder.with(new ConfigurableRaptorRouteSelector());
            raptor = builder.build();
        }
        Function<RoutingRequest, List<? extends PlanElement>> route = rust == null ? raptor::calcRoute : rust.createTransitRouter()::calcRoute;
        // Headway retains the batch tool's range-search definition, independently of the itinerary backend.
        return new Worker(route, headwayInterval > 0 ? new HeadwayCalculator(raptor, headwayInterval, headwayInterval) : null);
    }
    @Override public void close() { if (rust != null) rust.close(); }
}
