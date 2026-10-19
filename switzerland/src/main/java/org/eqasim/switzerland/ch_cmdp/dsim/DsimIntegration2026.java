package org.eqasim.switzerland.ch_cmdp.dsim;

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.dsim.EqasimDsimModule;
import org.eqasim.switzerland.ch_cmdp.transit_dwell.SampledTransitDwellConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/** Integration for the MATSim 2026.0 release used by both engines. */
public final class DsimIntegration2026 implements DsimIntegration {
    private static final Logger log = LogManager.getLogger(DsimIntegration2026.class);
    private final boolean simulateBikes;
    private boolean convertedDefaultVehicleSource;

    public DsimIntegration2026() { this(false); }

    public DsimIntegration2026(boolean simulateBikes) {
        this.simulateBikes = simulateBikes;
    }

    @Override public void adaptConfig(Config config) {
        // These extensions require live QSim state. Silently dropping them would invalidate the model.
        for (String group : Set.of("multiModeDrt", "dvrp", "eqasim:vdf", "eqasim:vdf_engine",
                "multiModeFeederDrt", "transitWithAbstractAccess")) {
            if (config.getModules().containsKey(group)) {
                throw new IllegalArgumentException("--use-dsim does not support " + group + " in this runner");
            }
        }
        var qsim = config.qsim();
        if (simulateBikes && qsim.getVehiclesSource() == QSimConfigGroup.VehiclesSource.defaultVehicle) {
            // A shared default type cannot make only bicycles capacity-free. Separate mode types
            // preserve the default vehicle behavior while giving bike a zero-PCU type.
            qsim.setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
            convertedDefaultVehicleSource = true;
        }
        if (!(qsim.getFlowCapFactor() > 0) || !Double.isFinite(qsim.getFlowCapFactor())
                || !(qsim.getStorageCapFactor() > 0) || !Double.isFinite(qsim.getStorageCapFactor())) {
            throw new IllegalArgumentException("DSim requires finite, positive flow and storage capacity factors");
        }
        if (qsim.getVehicleBehavior() == QSimConfigGroup.VehicleBehavior.wait) {
            throw new IllegalArgumentException("DSim does not support vehicleBehavior=wait; choose teleport or exception");
        }
        if (qsim.getPcuThresholdForFlowCapacityEasing() != 0.0) {
            log.warn("DSim does not implement QSim flow-capacity easing/preventwaitingtoentertraffic; "
                    + "using DSim's normal waiting-to-link behavior");
            qsim.setPcuThresholdForFlowCapacityEasing(0.0);
        }
        if (qsim.getTimeStepSize() != 1.0 || qsim.isUseLanes()) {
            throw new IllegalArgumentException("DSim requires one-second steps and does not support QSim lanes");
        }
        if (config.network().isTimeVariantNetwork()) {
            throw new IllegalArgumentException("DSim's cached network does not support time-variant network changes");
        }
        var modes = new LinkedHashSet<>(qsim.getMainModes());
        if (simulateBikes) modes.add(TransportMode.bike);
        else modes.remove(TransportMode.bike);
        modes.remove(TransportMode.pt);
        qsim.setMainModes(modes);
        var routingModes = new LinkedHashSet<>(config.routing().getNetworkModes());
        if (simulateBikes) routingModes.add(TransportMode.bike);
        else routingModes.remove(TransportMode.bike);
        config.routing().setNetworkModes(routingModes);
        // Adding the first explicit mode to a default config otherwise clears walk and all defaults.
        var teleported = config.routing().getTeleportedModeParams();
        teleported.remove(TransportMode.bike);
        config.routing().clearTeleportedModeParams();
        teleported.values().forEach(config.routing()::addTeleportedModeParams);
        if (!simulateBikes) {
            var bike = config.routing().getOrCreateModeRoutingParams(TransportMode.bike);
            // Same values as ch_cmdp.scenario.RunAdaptConfig's non-network bicycle mode.
            bike.setTeleportedModeSpeed(4.0);
            bike.setBeelineDistanceFactor(1.4);
        } else if (config.routing().getAccessEgressType()
                == org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType.none) {
            // Activities remain on the road layer. Access/egress stages move the agent to and
            // from the disconnected bicycle layer before/after its network leg.
            log.warn("DSim network bicycles require access/egress stages; changing routing.accessEgressType "
                    + "from none to accessEgressModeToLink");
            config.routing().setAccessEgressType(
                    org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
        }
        var measured = new LinkedHashSet<>(config.travelTimeCalculator().getAnalyzedModes());
        if (!simulateBikes) measured.remove(TransportMode.bike);
        config.travelTimeCalculator().setAnalyzedModes(measured);

        var dsim = config.dsim();
        dsim.setNetworkModes(modes);
        if (dsim.getThreads() <= 0) dsim.setThreads(qsim.getNumberOfThreads());
        dsim.setStartTime(qsim.getStartTime().orElse(0.0));
        dsim.setEndTime(qsim.getEndTime().orElse(30 * 3600.0));
        dsim.setStuckTime(qsim.getStuckTime());
        dsim.setVehicleBehavior(qsim.getVehicleBehavior());
        var dynamics = qsim.getLinkDynamics();
        if (dynamics == QSimConfigGroup.LinkDynamics.SeepageQ) {
            dynamics = QSimConfigGroup.LinkDynamics.FIFO;
        }
        qsim.setLinkDynamics(dynamics);
        dsim.setLinkDynamics(dynamics);
        var traffic = qsim.getTrafficDynamics();
        if (traffic == QSimConfigGroup.TrafficDynamics.withHoles) {
            log.warn("DSim: replacing unsupported withHoles with kinematicWaves; traffic behavior differs");
            traffic = QSimConfigGroup.TrafficDynamics.kinematicWaves;
        }
        qsim.setTrafficDynamics(traffic);
        dsim.setTrafficDynamics(traffic);
        if (qsim.isRemoveStuckVehicles()) {
            log.warn("DSim pushes stuck vehicles downstream instead of removing them");
            qsim.setRemoveStuckVehicles(false);
        }
        config.controller().setWriteSnapshotsInterval(0);
        // 2026.0 DSim does not produce the partial-score file required by the standard
        // experienced-plan shutdown writer. Scoring and normal plan output remain active.
        if (config.scoring().isWriteExperiencedPlans()) {
            log.warn("DSim 2026.0: disabling experienced-plan file output (partial-score export is unsupported)");
            config.scoring().setWriteExperiencedPlans(false);
        }
        // Keep useTransit=true: Raptor still loads and routes the full timetable.
        config.transit().setUsingTransitInMobsim(false);
        EqasimConfigGroup.get(config).setUseScheduleBasedTransport(true);
        ConfigUtils.addOrGetModule(config, SampledTransitDwellConfigGroup.class).setEnabled(false);
        config.controller().setMobsim("dsim");
        if (simulateBikes) {
            log.warn("DSim enabled: {} workers, network modes {}; bicycles use an isolated bike-only network layer",
                    dsim.getThreads(), modes);
        } else {
            log.warn("DSim enabled: {} workers, network modes {}; bikes teleported at 4 m/s, factor 1.4",
                    dsim.getThreads(), modes);
        }
        log.warn("DSim PT uses timetable departures/arrivals with unlimited capacity. No road buses, "
                + "congestion feedback to PT, SBB vehicle simulation, or sampled dwell times.");
        log.info("DSim road intersection delays remain enabled through the Eqasim link adapter; "
                + "flow/storage factors remain {} / {}", qsim.getFlowCapFactor(), qsim.getStorageCapFactor());
    }

    @Override public void adaptScenario(Scenario scenario) {
        if (simulateBikes) {
            prepareCapacityFreeBikeVehicles(scenario);
            DsimBikeNetworkAdapter.adapt(scenario.getNetwork());
        }
        long reset = 0;
        for (var person : scenario.getPopulation().getPersons().values()) {
            for (var plan : person.getPlans()) {
                for (var element : plan.getPlanElements()) {
                    if (element instanceof Leg leg && TransportMode.bike.equals(leg.getMode())) {
                        // PrepareForSim recalculates all affected plans, including saved output plans.
                        leg.setRoute(null);
                        leg.setTravelTimeUndefined();
                        reset++;
                    }
                }
            }
        }
        log.info("DSim: invalidated {} bicycle legs so they are rerouted for the selected DSim bicycle behavior", reset);
    }

    private void prepareCapacityFreeBikeVehicles(Scenario scenario) {
        var vehicles = scenario.getVehicles();
        if (convertedDefaultVehicleSource) {
            var modes = new LinkedHashSet<>(scenario.getConfig().qsim().getMainModes());
            modes.addAll(scenario.getConfig().routing().getNetworkModes());
            for (String mode : modes) {
                var id = org.matsim.api.core.v01.Id.create(mode, VehicleType.class);
                if (!vehicles.getVehicleTypes().containsKey(id)) vehicles.addModeVehicleType(mode);
            }
        }

        var bikeTypes = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<VehicleType, Boolean>());
        for (VehicleType type : vehicles.getVehicleTypes().values()) {
            if ((type.hasNetworkMode() && TransportMode.bike.equals(type.getNetworkMode()))
                    || TransportMode.bike.equals(type.getId().toString())) {
                bikeTypes.add(type);
            }
        }
        if (scenario.getConfig().qsim().getVehiclesSource() == QSimConfigGroup.VehiclesSource.fromVehiclesData) {
            for (var person : scenario.getPopulation().getPersons().values()) {
                if (VehicleUtils.hasVehicleId(person, TransportMode.bike)) {
                    var bikeId = VehicleUtils.getVehicleId(person, TransportMode.bike);
                    var vehicle = vehicles.getVehicles().get(bikeId);
                    if (vehicle != null) bikeTypes.add(vehicle.getType());
                }
            }
        }
        if (bikeTypes.isEmpty()) {
            throw new IllegalArgumentException("--" + DsimIntegration.SIMULATE_BIKES_OPTION
                    + " requires a bicycle vehicle type or person-to-bike vehicle mapping");
        }
        bikeTypes.forEach(type -> type.setPcuEquivalents(0.0));
        log.warn("Configured {} bicycle vehicle type(s) with PCU 0.0; bicycles consume no DSim flow or storage capacity",
                bikeTypes.size());
    }

    @Override public void configureController(Controler controller, EqasimConfigurator configurator) {
        // Controller-level routing, calibration, delay calculation, costs and scoring remain installed.
        // QSim-only engines and SBB component configuration must not enter DSim's partition injectors.
        configurator.getModules(controller.getConfig()).forEach(controller::addOverridingModule);
        controller.addOverridingModule(new EqasimDsimModule());
        controller.addOverridingQSimModule(EqasimDsimModule.engineModule());
    }
}
