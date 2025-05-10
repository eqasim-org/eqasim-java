package org.eqasim.core.tools.sampling;

import java.io.File;
import java.util.Random;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.tools.sampling.ScenarioSampler.UpdateSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.vehicles.MatsimVehicleWriter;

import com.google.common.base.Preconditions;

public class RunDownsampling {
    static public void main(String[] args) throws ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "sampling-rate", "suffix") //
                .allowOptions("update", EqasimConfigurator.CONFIGURATOR) //
                .build();

        Preconditions.checkArgument(cmd.getOptionStrict("suffix").length() > 0, "Suffix must be given.");

        // Boostrap
        EqasimConfigurator configurator = EqasimConfigurator.getInstance(cmd);

        // Parse command line
        UpdateSet update = cmd.hasOption("update") ? new UpdateSet(cmd.getOptionStrict("update")) : new UpdateSet();

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
        new ScenarioSampler(update, samplingRate, random).process(scenario);

        // Prepare configuration
        String suffix = cmd.getOptionStrict("suffix");

        String populationPath = withSuffix(suffix, config.plans().getInputFile());
        new PopulationWriter(scenario.getPopulation()).write(writeTo(config, populationPath));
        config.plans().setInputFile(populationPath);

        if (update.households) {
            String householdsPath = withSuffix(suffix, config.households().getInputFile());
            new HouseholdsWriterV10(scenario.getHouseholds()).writeFile(writeTo(config, householdsPath));
            config.households().setInputFile(householdsPath);
        }

        if (update.vehicles) {
            String vehiclesPath = withSuffix(suffix, config.vehicles().getVehiclesFile());
            new MatsimVehicleWriter(scenario.getVehicles()).writeFile(writeTo(config, vehiclesPath));
            config.vehicles().setVehiclesFile(vehiclesPath);
        }

        if (update.facilities) {
            String facilitiesPath = withSuffix(suffix, config.facilities().getInputFile());
            new FacilitiesWriter(scenario.getActivityFacilities()).write(writeTo(config, facilitiesPath));
            config.facilities().setInputFile(facilitiesPath);
        }

        String configPath = withSuffix(suffix, cmd.getOptionStrict("config-path"));
        new ConfigWriter(config).write(configPath);
    }

    private static String withSuffix(String suffix, String path) {
        File directory = new File(path).getParentFile();

        String[] segments = new File(path).getName().split("\\.", 2);
        String filename = segments[0] + "_" + suffix + "." + segments[1];

        return new File(directory, filename).getPath();
    }

    private static String writeTo(Config config, String path) {
        return ConfigGroup.getInputFileURL(config.getContext(), path).getPath();
    }
}
