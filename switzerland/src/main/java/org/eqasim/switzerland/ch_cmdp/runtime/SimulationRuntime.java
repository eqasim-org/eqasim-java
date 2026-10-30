package org.eqasim.switzerland.ch_cmdp.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.*;
import org.eqasim.switzerland.ch_cmdp.dsim.DsimIntegration;
import org.eqasim.switzerland.ch_cmdp.dsim.DsimBikeNetworkRestoration;
import org.eqasim.switzerland.ch_cmdp.memory.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.*;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

/** Owns temporary engine adaptations. Final export preserves calibrated values, but undoes engine choices. */
public final class SimulationRuntime implements ShutdownListener {
    public static final String TELEPORT_BIKE_OPTION = "teleport-bike";
    private final Config config;
    private final DsimIntegration dsim;
    private final boolean teleportBike;
    private final Map<String, Map<String, String>> original = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> changed = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> applied = new LinkedHashMap<>();
    private final RoutingConfigGroup.TeleportedModeParams originalBike;
    private Map<String, String> appliedBike;
    private Config export;
    private record CapacityChange(double original, double applied) { }
    private Scenario scenario;
    private ScenarioAttributeArchive archive;
    private DsimBikeNetworkRestoration bikeLayer;
    private final Map<org.matsim.vehicles.VehicleType, CapacityChange> pcu = new IdentityHashMap<>();
    private boolean restored;

    public static boolean teleportBike(CommandLine cmd) throws CommandLine.ConfigurationException {
        String value = cmd.getOption(TELEPORT_BIKE_OPTION).orElse("false");
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new CommandLine.ConfigurationException("--teleport-bike must be true or false");
    }

    public SimulationRuntime(Config config, DsimIntegration dsim, boolean teleportBike) {
        this.config = config; this.dsim = dsim; this.teleportBike = teleportBike;
        config.getModules().forEach((name, group) -> original.put(name, new LinkedHashMap<>(group.getParams())));
        originalBike = copyBikeParameters(config);
        preprocessConfig();
    }

    /** Adapt engine settings before loading the scenario; remember only the fields that we change. */
    private void preprocessConfig() {
        if (dsim != null) dsim.adaptConfig(config);
        if (teleportBike) configureBikeTeleportation();
        // Record only settings actually changed by preparation; calibration may update unrelated parameters later.
        original.forEach((name, values) -> values.forEach((key, v) -> {
            // This is a one-way parser switch, not a simulation setting; explicit exported mode sets replace defaults.
            if (!(name.equals("routing") && key.equals("clearDefaultTeleportedModeParams"))
                    && !Objects.equals(v, config.getModules().get(name).getParams().get(key)))
                changed.computeIfAbsent(name, ignored -> new LinkedHashMap<>()).put(key, v);
        }));
        changed.forEach((name, values) -> applied.put(name, new LinkedHashMap<>(config.getModules().get(name).getParams())));
        var bike = config.routing().getTeleportedModeParams().get(TransportMode.bike);
        appliedBike = bike == null ? null : new LinkedHashMap<>(bike.getParams());
    }

    /** Teleport bikes without editing the network or disturbing other teleportation modes. */
    private void configureBikeTeleportation() {
        var modes = new LinkedHashSet<>(config.qsim().getMainModes()); modes.remove(TransportMode.bike);
        config.qsim().setMainModes(modes);
        modes = new LinkedHashSet<>(config.routing().getNetworkModes()); modes.remove(TransportMode.bike);
        config.routing().setNetworkModes(modes);
        modes = new LinkedHashSet<>(config.travelTimeCalculator().getAnalyzedModes()); modes.remove(TransportMode.bike);
        config.travelTimeCalculator().setAnalyzedModes(modes);
        var teleported = config.routing().getTeleportedModeParams();
        teleported.remove(TransportMode.bike);
        config.routing().clearTeleportedModeParams();
        teleported.values().forEach(config.routing()::addTeleportedModeParams);
        var params = config.routing().getOrCreateModeRoutingParams(TransportMode.bike);
        params.setTeleportedModeSpeed(4.0); params.setBeelineDistanceFactor(1.4);
    }

    /** Retain only the original bicycle routing parameters, leaving other modes outside our restoration scope. */
    private static RoutingConfigGroup.TeleportedModeParams copyBikeParameters(Config config) {
        var source = config.routing().getTeleportedModeParams().get(TransportMode.bike);
        if (source == null) return null;
        var copy = new RoutingConfigGroup.TeleportedModeParams();
        source.getParams().forEach(copy::addParam);
        return copy;
    }

    /** Run after normal scenario adjustments, and before attribute cleanup. */
    public void preprocess(Scenario scenario, boolean lighten) throws IOException {
        this.scenario = scenario;
        prepareDsimScenario();
        archive = lighten ? ScenarioAttributeCleaner.cleanAndArchive(scenario) : null;
    }

    /** Snapshot only capacities and network data that DSim's bicycle conversion can change. */
    private void prepareDsimScenario() {
        if (dsim == null) return;
        boolean networkBikes = config.dsim().getNetworkModes().contains(TransportMode.bike);
        var before = new IdentityHashMap<org.matsim.vehicles.VehicleType, Double>();
        if (networkBikes) {
            bikeLayer = DsimBikeNetworkRestoration.capture(scenario.getNetwork());
            scenario.getVehicles().getVehicleTypes().values().forEach(type -> before.put(type, type.getPcuEquivalents()));
        }
        dsim.adaptScenario(scenario);
        if (networkBikes) scenario.getVehicles().getVehicleTypes().values().forEach(type -> {
            double old = before.getOrDefault(type, 1.0), now = type.getPcuEquivalents();
            if (old != now) pcu.put(type, new CapacityChange(old, now));
        });
    }

    /** Incompatible bicycle routes are prepared again by MATSim on the next run. */
    private void invalidateBikeRoutes() {
        scenario.getPopulation().getPersons().values().forEach(person -> person.getPlans().forEach(plan ->
                plan.getPlanElements().forEach(element -> {
                    if (element instanceof Leg leg && TransportMode.bike.equals(leg.getMode())) {
                        leg.setRoute(null); leg.setTravelTimeUndefined();
                    }
                })));
    }

    /** Install one final-export boundary shared by scenario cleanup and engine adaptations. */
    public void install(Controler controller) {
        controller.addOverridingModule(AttributeRestoringDumpDataAtEnd.module(this::postprocess,
                () -> writeExportConfig(controller), this));
    }

    /** Undo owned preprocessing changes before final scenario writers; simulation updates survive. */
    public void postprocess() {
        if (restored) return;
        restoreAttributes();
        restoreBikeNetwork();
        restoreVehicleCapacities();
        if (dsim != null || teleportBike) export = exportConfig();
        prepareBikeRoutesForExport();
        restored = true;
    }

    /** Current attributes win over archived values; absent stable persons/households/links are skipped. */
    private void restoreAttributes() { if (archive != null) archive.restore(); }

    /** Remove only a bicycle layer created by this run, remapping its surviving references. */
    private void restoreBikeNetwork() { if (bikeLayer != null) bikeLayer.restore(scenario); }

    /** Restore only PCUs changed by preparation and still equal to the value we installed. */
    private void restoreVehicleCapacities() {
        pcu.forEach((type, change) -> {
            if (Double.compare(type.getPcuEquivalents(), change.applied()) == 0)
                type.setPcuEquivalents(change.original());
        });
    }

    /** Let the next run prepare compatible routes when switching back between teleportation and network routing. */
    private void prepareBikeRoutesForExport() {
        Config target = export == null ? config : export;
        if (target.routing().getNetworkModes().contains(TransportMode.bike)
                != config.routing().getNetworkModes().contains(TransportMode.bike)) invalidateBikeRoutes();
    }

    /** The delegate must keep DSim's writer flags; publish the prepared baseline configs after it completes. */
    private void writeExportConfig(Controler controller) {
        if (export == null) return;
        var io = controller.getInjector().getInstance(OutputDirectoryHierarchy.class);
        new ConfigWriter(export).write(io.getOutputFilename(Controler.DefaultFiles.config,
                ControllerConfigGroup.CompressionType.none));
        new ConfigWriter(export, ConfigWriter.Verbosity.minimal).write(io.getOutputFilename(
                Controler.DefaultFiles.configReduced, ControllerConfigGroup.CompressionType.none));
    }

    /** Copy current configuration (including final calibration), then undo only temporary engine changes. */
    public Config exportConfig() {
        try {
            var file = Files.createTempFile("eqasim-export-config-", ".xml");
            try {
                new ConfigWriter(config).write(file.toString());
                Config exported = ConfigUtils.loadConfig(file.toString());
                exported.setContext(config.getContext());
                restoreConfigParameters(exported);
                restoreBikeParameters(exported);
                return exported;
            } finally { Files.deleteIfExists(file); }
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    /** Restore a scalar only if it still has our preprocessing value, preserving subsequent simulation edits. */
    private void restoreConfigParameters(Config exported) {
        changed.forEach((name, values) -> values.forEach((key, v) -> {
            if (Objects.equals(config.getModules().get(name).getParams().get(key), applied.get(name).get(key)))
                exported.getModules().get(name).addParam(key, v);
        }));
    }

    /** Restore only the bike parameter set; changes to walking and other modes remain untouched. */
    private void restoreBikeParameters(Config exported) {
        var current = config.routing().getTeleportedModeParams().get(TransportMode.bike);
        if (!Objects.equals(current == null ? null : current.getParams(), appliedBike)) return;
        var modes = exported.routing().getTeleportedModeParams();
        modes.remove(TransportMode.bike);
        exported.routing().clearTeleportedModeParams();
        modes.values().forEach(exported.routing()::addTeleportedModeParams);
        if (originalBike != null) exported.routing().addTeleportedModeParams(originalBike);
    }

    @Override public void notifyShutdown(ShutdownEvent event) {
        if (!event.isUnexpected()) postprocess(); // Also handles dumpDataAtEnd=false.
        if (archive != null) archive.notifyShutdown(event);
    }
}
