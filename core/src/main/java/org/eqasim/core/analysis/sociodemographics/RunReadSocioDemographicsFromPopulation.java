package org.eqasim.core.analysis.sociodemographics;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

import java.io.IOException;

public class RunReadSocioDemographicsFromPopulation {
    private static final Logger log = Logger.getLogger( RunReadSocioDemographicsFromPopulation.class );

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "output-path") //
                .build();

        new RunReadSocioDemographicsFromPopulation().run(cmd.getOptionStrict("input-path"), cmd.getOptionStrict("output-path"));
    }

    private void run(String inputPath, String outputPath) throws IOException {

        log.info("Reading in the population ...");
        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        PopulationUtils.readPopulation(population, inputPath);

        log.info("Extracting sociodemographic information ...");
        SocioDemographics socioDemographics = new SocioDemographicsReader().read(population);

        log.info("Writing sociodemographic information to csv ...");
        new SocioDemographicsWriter(socioDemographics).write(outputPath);

        log.info("Done.");
    }

}

