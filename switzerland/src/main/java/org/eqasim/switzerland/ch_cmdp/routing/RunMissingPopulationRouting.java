package org.eqasim.switzerland.ch_cmdp.routing;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.eqasim.core.components.routing.speedy.MemoryEfficientRoutingModule;
import org.eqasim.core.components.travel_time.FloatTravelTimeModule;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.eqasim.switzerland.ch_cmdp.SwitzerlandConfigurator;
import org.eqasim.switzerland.ch_cmdp.runtime.SimulationRuntimeConfigGroup;
import org.eqasim.switzerland.ch_cmdp.transit_dwell.SampledTransitDwellConfigGroup;
import org.matsim.core.config.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretationModule;

/**
 * Fill missing/incompatible trip routes, preserving complete trips and all scenario attributes.
 * Required: --config-path PATH --output-path PLANS.xml.gz. Optional: --threads N, --modes bike,car.
 * Network/teleported modes follow config.routing; this tool performs no DSim network conversion.
 * No simulation is run. All stored plans are checked. Output is the repaired population only.
 */
public final class RunMissingPopulationRouting {
    public static void main(String[] args) throws Exception {
        var cmd = new CommandLine.Builder(args).requireOptions("config-path", "output-path")
                .allowOptions("threads", "modes", MemoryEfficientRoutingModule.OPTION, FloatTravelTimeModule.OPTION)
                .allowPrefixes("mode-parameter", "cost-parameter").build();
        var configurator = new SwitzerlandConfigurator(cmd);
        var config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
                new SampledTransitDwellConfigGroup(), new SimulationRuntimeConfigGroup());
        configurator.updateConfig(config); cmd.applyConfiguration(config);
        config.getModules().remove(EqasimTerminationConfigGroup.GROUP_NAME);
        config.replanning().clearStrategySettings();
        // Routing uses controller services only, independently of the selected simulation engine.
        config.controller().setMobsim("qsim");
        int threads = cmd.getOption("threads").map(Integer::parseInt).orElse(config.global().getNumberOfThreads());
        if (threads < 1) throw new IllegalArgumentException("threads must be positive");
        Set<String> modes = Arrays.stream(cmd.getOption("modes").orElse("").split(","))
                .map(String::trim).filter(mode -> !mode.isEmpty()).collect(Collectors.toSet());
        var scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario); ScenarioUtils.loadScenario(scenario);
        if (!MissingTripRouting.hasMissingRoutes(scenario, modes)) {
            new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-path"));
            return;
        }
        configurator.adjustScenario(scenario);
        var builder = new InjectorBuilder(scenario, configurator)
                .addOverridingModule(new TimeInterpretationModule())
                .addOverridingModule(new AbstractModule() {
                    @Override public void install() { bind(MissingTripRouting.Worker.class); }
                });
        if (MemoryEfficientRoutingModule.isEnabled(cmd)) builder.addOverridingModule(new MemoryEfficientRoutingModule());
        if (FloatTravelTimeModule.isEnabled(cmd)) builder.addOverridingModule(new FloatTravelTimeModule());
        var injector = builder.build();
        MissingTripRouting.run(scenario, threads, modes, injector.getProvider(MissingTripRouting.Worker.class));
        new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-path"));
    }
}
