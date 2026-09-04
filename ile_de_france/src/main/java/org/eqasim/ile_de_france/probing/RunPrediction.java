package org.eqasim.ile_de_france.probing;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunPrediction {
    public static void main(String[] args)
            throws CommandLine.ConfigurationException, InterruptedException, IOException, ExecutionException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path") //
                .build();

        // Loading the config
        EqasimConfigurator configurator = EqasimConfigurator.getInstance(cmd);

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
        configurator.updateConfig(config);
        cmd.applyConfiguration(config);

        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        configurator.adjustScenario(scenario);

        Controler controller = new Controler(scenario);
        configurator.configureController(controller);
        controller.getInjector().getInstance(PredictionWriter.class).run();
    }
}
