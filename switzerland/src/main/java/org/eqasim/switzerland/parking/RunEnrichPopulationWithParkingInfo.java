package org.eqasim.switzerland.parking;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

public class RunEnrichPopulationWithParkingInfo {

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {

        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("input-population", "parking-info", "output-population")
                .allowOptions("delimiter")
                .build();

        // create population data container
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        // read original population file
        new PopulationReader(scenario).readFile(cmd.getOptionStrict("input-population"));
        Population population = scenario.getPopulation();

        // read in parking info and enrich population
        String delimiter = cmd.getOption("delimiter").orElse(",");
        new PopulationParkingInfoEnricher(population).enrich(cmd.getOptionStrict("parking-info"), delimiter);

        // write out enriched population
        new PopulationWriter(population).write(cmd.getOptionStrict("output-population"));

    }
}
