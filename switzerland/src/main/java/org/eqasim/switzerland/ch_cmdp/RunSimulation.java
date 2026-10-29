package org.eqasim.switzerland.ch_cmdp;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import org.eqasim.switzerland.ch_cmdp.StrategyWeightDecay.StrategyWeightDecayModule;
import org.eqasim.switzerland.ch_cmdp.memory.ScenarioAttributeCleaner;
import org.eqasim.switzerland.ch_cmdp.runtime.SimulationRuntime;
import org.eqasim.switzerland.ch_cmdp.runtime.SimulationRuntimeConfigGroup;
import org.eqasim.switzerland.ch_cmdp.routing.pt_cache.PtRouteCacheModule;
import org.eqasim.switzerland.ch_cmdp.routing.MissingTripRoutingModule;
import org.eqasim.switzerland.ch_cmdp.dsim.DsimIntegration;
import org.eqasim.switzerland.ch_cmdp.dsim.DsimJvmSupport;
import org.eqasim.core.components.routing.speedy.MemoryEfficientRoutingModule;
import org.eqasim.core.components.travel_time.FloatTravelTimeModule;
import org.eqasim.switzerland.ch_cmdp.tolls.TollsModule;
import org.eqasim.switzerland.ch_cmdp.utils.link_stats.DetailedLinkStatisticsModule;
import org.eqasim.switzerland.ch_cmdp.transit_dwell.SampledTransitDwellConfigGroup;
import org.eqasim.switzerland.ch_cmdp.transit_dwell.SampledTransitDwellQSimModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;


/**
 * Runs the Swiss Simulation with QSim or DSim.
 *
 * <p>Required: {@code --config-path PATH}. Relative scenario paths are resolved against that config.
 * MATSim parameters can be overridden with {@code --config:GROUP.PARAMETER VALUE}.
 *
 * <p>Engine selection: {@code controller.mobsim=dsim} in the config selects DSim directly.
 * Alternatively, {@code --use-dsim true} temporarily switches a QSim config to DSim.
 * The final output config retains the original configured engine. DSim uses timetable-based PT
 * without road buses and teleports bicycles unless network bicycle simulation is requested.
 *
 * <p>Optional arguments (boolean options take {@code true} or {@code false}):
 * <ul>
 *   <li>{@code --dsim-simulate-bikes}: default {@code false}. Simulate capacity-free bikes on an
 *       isolated network layer in DSim; the temporary layer is removed for final scenario export.</li>
 *   <li>{@code --teleport-bike}: default is obtained from the config. Teleport bikes at 4 m/s with distance factor 1.4, including with QSim.
 *       Conflicts with {@code --dsim-simulate-bikes true}; teleportation leaves the network intact.</li>
 *   <li>{@code --lighten-scenario}: default {@code true}. Remove unused attributes during the run;
 *       archive and restore removed person, household and link attributes before final export.</li>
 *   <li>{@code --memory-efficient-routing}: default {@code true}. Use optimized SpeedyALT routing.</li>
 *   <li>{@code --float-travel-times}: default {@code true}. Share compact float travel-time storage.</li>
 *   <li>{@code --cache-pt-routes}: default {@code false}. Enable the PT itinerary cache;
 *       its budget and time tolerance are configured in the PT cache config module.</li>
 *   <li>{@code --mode-parameter:NAME VALUE} and {@code --cost-parameter:NAME VALUE}:
 *       override mode-choice and cost coefficients.</li>
 *   <li>{@code --samplingRateForPT VALUE}: multiply transit vehicle PCU equivalents by this factor.</li>
 *   <li>{@code --preventwaitingtoentertraffic y}: enable QSim flow-capacity easing for link entry
 *       (PCU threshold 1.0). DSim does not implement this feature.</li>
 * </ul>
 *
 * <p>Preprocessing adaptations are restored at normal shutdown before final scenario writers.
 * Initial preparation fills only missing or incompatible trip routes, including network bike
 * routes when enabled. DSim first clears every bike route to prepare it for the chosen bike behavior;
 * other complete trips are preserved. Teleported bikes require no network search.
 * Final config export restores the original engine settings while preserving simulation updates.
 * {@code run_config.xml} records the runtime settings, including temporary adaptations.
 */
public class RunSimulation {
    private static final Logger LOG = LogManager.getLogger(RunSimulation.class);

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path") //
                .allowOptions(ScenarioAttributeCleaner.OPTION, MemoryEfficientRoutingModule.OPTION,
                        FloatTravelTimeModule.OPTION, DsimIntegration.OPTION, DsimIntegration.SIMULATE_BIKES_OPTION,
                        PtRouteCacheModule.OPTION, SimulationRuntime.TELEPORT_BIKE_OPTION) //
                .allowPrefixes("mode-parameter", "cost-parameter", "preventwaitingtoentertraffic", "samplingRateForPT") //
                .build();
        boolean lightenScenario = ScenarioAttributeCleaner.isEnabled(cmd);
        boolean efficientRouting = MemoryEfficientRoutingModule.isEnabled(cmd);
        boolean floatTravelTimes = FloatTravelTimeModule.isEnabled(cmd);

        SwitzerlandConfigurator configurator = new SwitzerlandConfigurator(cmd);
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), new SampledTransitDwellConfigGroup(),
                new SimulationRuntimeConfigGroup());
        configurator.updateConfig(config);
        configurator.configure(config);
        cmd.applyConfiguration(config);
        PtRouteCacheModule.applyCommand(cmd, config);
        DsimIntegration dsim = DsimIntegration.fromCommand(cmd, config);
        boolean teleportBike = cmd.hasOption(SimulationRuntime.TELEPORT_BIKE_OPTION) ? SimulationRuntime.teleportBike(cmd)
                : ConfigUtils.addOrGetModule(config, SimulationRuntimeConfigGroup.class).isTeleportBike();
        if (teleportBike && DsimIntegration.simulateBikes(cmd))
            throw new ConfigurationException("teleport-bike conflicts with --dsim-simulate-bikes true");
        if (dsim != null) {
            var exit = DsimJvmSupport.runWithModuleAccess(RunSimulation.class.getName(), args);
            if (exit.isPresent()) {
                System.exit(exit.getAsInt());
                return;
            }
        }

        if (cmd.getOption("preventwaitingtoentertraffic").filter("y"::equals).isPresent()) {
            config.qsim().setPcuThresholdForFlowCapacityEasing(1.0);
        }

        SimulationRuntime runtime = new SimulationRuntime(config, dsim, teleportBike);
        Scenario scenario = ScenarioUtils.createScenario(config);

        SwissRailRaptorConfigGroup raptor = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
        LOG.info("Configured PT transfer penalties: {}", raptor.getModeToModeTransferPenaltyParameterSets());

        configurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        configurator.adjustScenario(scenario);
        configurator.adjustPTpcu(scenario);
        runtime.preprocess(scenario, lightenScenario);
        Controler controller = new Controler(scenario);
        if (floatTravelTimes) controller.addOverridingModule(new FloatTravelTimeModule());
        if (dsim == null) configurator.configureController(controller);
        else dsim.configureController(controller, configurator);
        runtime.install(controller);
        controller.addOverridingModule(new MissingTripRoutingModule());
        if (efficientRouting) controller.addOverridingModule(new MemoryEfficientRoutingModule());
        controller.addOverridingModule(new PtRouteCacheModule());
        if (dsim == null) {
            controller.addOverridingModule(new PTPassengerCountsModule());
            controller.addOverridingModule(new PTLinkVolumesModule());
        }
        controller.addOverridingModule(new StrategyWeightDecayModule());
        controller.addOverridingModule(new TollsModule());
        controller.addOverridingModule(new DetailedLinkStatisticsModule());

        // Install QSim-specific transit engines; DSim provides its own scheduled PT engine.
        if (dsim == null) {
            controller.addOverridingModule(new SBBTransitModule());
            controller.configureQSimComponents(components -> {
                new SBBTransitEngineQSimModule().configure(components);
            });
            controller.addOverridingQSimModule(new SampledTransitDwellQSimModule());
        }
        // Record the actual runtime config; final output configs are restored by SimulationRuntime.
        ConfigUtils.writeConfig(config, "run_config.xml");
        controller.run();
    }
}
