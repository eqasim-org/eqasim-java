package org.eqasim.core.tools;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.base.Preconditions;

public class SplitPopulation {
    static public void main(String[] args) throws ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("population-path") //
                .allowOptions("chunks", "chunk-size", EqasimConfigurator.CONFIGURATOR) //
                .build();

        String populationPath = cmd.getOptionStrict("population-path");

        Preconditions.checkArgument(cmd.hasOption("chunks") ^ cmd.hasOption("chunk-size"),
                "Either the number of chunks or the chunk size must be given.");

        EqasimConfigurator configurator = EqasimConfigurator.getInstance(cmd);

        Config config = ConfigUtils.createConfig();
        configurator.updateConfig(config);

        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);

        new PopulationReader(scenario).readFile(populationPath);
        Population population = scenario.getPopulation();
        int total = population.getPersons().size();

        final int chunkSize;
        final int chunks;

        if (cmd.hasOption("chunk-size")) {
            chunkSize = Integer.parseInt(cmd.getOptionStrict("chunk-size"));
            chunks = total / chunkSize + 1;
        } else {
            chunks = Integer.parseInt(cmd.getOptionStrict("chunks"));
            chunkSize = total / chunks + 1;
        }

        Preconditions.checkState(chunkSize * chunkSize >= total);

        List<Person> persons = new LinkedList<>();
        persons.addAll(population.getPersons().values());

        for (int k = 0; k < chunks; k++) {
            int startIndex = k * chunkSize;
            int endIndex = Math.min((k + 1) * chunkSize, total);

            // clear
            Set<Id<Person>> remove = new HashSet<>(scenario.getPopulation().getPersons().keySet());
            remove.forEach(scenario.getPopulation()::removePerson);

            // fill up
            persons.subList(startIndex, endIndex).forEach(scenario.getPopulation()::addPerson);

            // write
            String chunkPath = createChunkPath(populationPath, k);
            new PopulationWriter(population).write(chunkPath);
        }
    }

    static public String createChunkPath(String path, int chunk) {
        String name = new File(path).getName().split("\\.")[0];
        return path.replace(name, name + String.format(".%06d", chunk));
    }
}
