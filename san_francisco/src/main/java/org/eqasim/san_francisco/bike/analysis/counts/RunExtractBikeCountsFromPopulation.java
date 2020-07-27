package org.eqasim.san_francisco.bike.analysis.counts;

import org.apache.log4j.Logger;
import org.eqasim.san_francisco.bike.analysis.counts.items.CountItem;
import org.eqasim.san_francisco.bike.analysis.counts.readers.BikeCountReaderFromPopulation;
import org.eqasim.san_francisco.bike.analysis.counts.writers.BikeCountWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RunExtractBikeCountsFromPopulation {

    public static final Logger log = Logger.getLogger(RunExtractBikeCountsFromPopulation.class);

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("plans", "network", "output")
                .build();

        new RunExtractBikeCountsFromPopulation().run(cmd.getOptionStrict("plans"),
                cmd.getOptionStrict("network"),
                cmd.getOptionStrict("output"));
    }

    public void run(String plansPath, String networkPath, String outputPath) throws IOException {

        log.info("Loading population...");
        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        PopulationUtils.readPopulation(population, plansPath);

        log.info("Loading network...");
        log.info("Loading population...");
        Network network = NetworkUtils.readNetwork(networkPath);

        log.info("Extracting bike counts...");
        Map<Id<Link>, List<CountItem>> bikeCounts = new BikeCountReaderFromPopulation(13.67, 900)
                .read(population, network);

        log.info("Writing bike counts...");
        new BikeCountWriter(bikeCounts).write(outputPath);
    }
}
