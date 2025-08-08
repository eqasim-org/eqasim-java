package org.eqasim.core.tools;

import java.io.File;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class MergePopulation {
    static public void main(String[] args) throws ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("population-path") //
                .allowOptions("output-path", "eqasim-configurator") //
                .build();

        String populationPath = cmd.getOptionStrict("population-path");

        int chunks = 0;
        while (new File(SplitPopulation.createChunkPath(populationPath, chunks)).exists()) {
            chunks++;
        }

        EqasimConfigurator configurator = EqasimConfigurator.getInstance(cmd);

        Config config = ConfigUtils.createConfig();
        configurator.updateConfig(config);

        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);

        Population population = scenario.getPopulation();

        for (int k = 0; k < chunks; k++) {
            String chunkPath = SplitPopulation.createChunkPath(populationPath, k);
            new PopulationReader(scenario).readFile(chunkPath);
        }

        String outputPath = cmd.getOption("output-path").orElse(createMergePath(populationPath));
        new PopulationWriter(population).write(outputPath);
    }

    static public String createMergePath(String path) {
        String name = new File(path).getName().split("\\.")[0];
        return path.replace(name, name + ".merged");
    }
}
