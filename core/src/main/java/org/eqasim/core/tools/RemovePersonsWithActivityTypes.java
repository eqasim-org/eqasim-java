package org.eqasim.core.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RemovePersonsWithActivityTypes {

    private static final Logger logger = LogManager.getLogger(RemovePersonsWithActivityTypes.class);

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args).requireOptions("input-path", "output-path", "activity-types").build();

        Set<String> activityTypes = Arrays.stream(commandLine.getOptionStrict("activity-types").split(",")).map(String::trim).collect(Collectors.toSet());

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(commandLine.getOptionStrict("input-path"));

        logger.info("Old population size: " + scenario.getPopulation().getPersons().size());
        scenario.getPopulation().getPersons().values().stream()
                .filter(p -> p.getSelectedPlan().getPlanElements().stream()
                        .filter(e -> e instanceof Activity)
                        .map(e -> ((Activity) e).getType())
                        .anyMatch(activityTypes::contains))
                .map(Identifiable::getId)
                .collect(Collectors.toSet())
                .forEach(scenario.getPopulation()::removePerson);
        logger.info("new population size: " + scenario.getPopulation().getPersons().size());
        new PopulationWriter(scenario.getPopulation()).write(commandLine.getOptionStrict("output-path"));
    }
}
