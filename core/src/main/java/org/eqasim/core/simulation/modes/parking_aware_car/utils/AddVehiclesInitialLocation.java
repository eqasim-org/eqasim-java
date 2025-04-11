package org.eqasim.core.simulation.modes.parking_aware_car.utils;


import com.google.inject.Injector;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.validation.VehiclesValidator;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.modes.parking_aware_car.routing.InitialParkingAssignment;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class AddVehiclesInitialLocation {
    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("config-path", "output-path")
                .allowOptions(EqasimConfigurator.CONFIGURATOR)
                .build();

        String configPath = commandLine.getOptionStrict("config-path");
        String outputPath = commandLine.getOptionStrict("output-path");

        EqasimConfigurator configurator = EqasimConfigurator.getInstance(commandLine);
        Config config = ConfigUtils.loadConfig(configPath);
        configurator.updateConfig(config);
        commandLine.applyConfiguration(config);

        VehiclesValidator.validate(config);

        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        configurator.adjustScenario(scenario);

        Injector injector = new InjectorBuilder(scenario, configurator).build(); //
        InitialParkingAssignment initialParkingAssignment = injector.getInstance(InitialParkingAssignment.class);
        initialParkingAssignment.assignInitialParkingForPopulation();

        new PopulationWriter(scenario.getPopulation()).write(outputPath);
    }

}
