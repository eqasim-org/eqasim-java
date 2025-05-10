package org.eqasim.core.tools.sampling;

import java.util.Random;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class RunDownsampling {
    static public void main(String[] args) throws ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "sampling-rate") //
                .allowOptions("prefix", "output-path", EqasimConfigurator.CONFIGURATOR) //
                .build();

        // Boostrap
        EqasimConfigurator configurator = EqasimConfigurator.getInstance(cmd);

        // Load configuration
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
        configurator.updateConfig(config);

        // Prepare scenario
        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.adjustScenario(scenario);
        ScenarioUtils.loadScenario(scenario);

        // Prepare sampling
        double samplingRate = Double.parseDouble(cmd.getOptionStrict("sampling-rate"));

        int seed = cmd.getOption("seed").map(Integer::parseInt).orElse(0);
        Random random = new Random(seed);

        // Process scenario
        new ScenarioSampler(samplingRate, random).process(scenario);

    }
}
