package org.eqasim.core.tools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.api.core.v01.Scenario;

import java.util.List;
import java.util.regex.Pattern;

public class FilterPopulationByActivityChainFormat {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("input-path", "output-path", "pattern")
                .allowOptions("separator")
                .build();

        String inputPath = commandLine.getOptionStrict("input-path");
        String outputPath = commandLine.getOptionStrict("output-path");
        String patternString = commandLine.getOptionStrict("pattern");

        String separator = commandLine.getOption("separator").orElse("-");


        Pattern pattern = Pattern.compile(patternString);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new PopulationReader(scenario).readFile(inputPath);

        int initialSize = scenario.getPopulation().getPersons().size();

        List<Id<Person>> toRemove = scenario.getPopulation().getPersons().values()
                .stream().filter(p -> !pattern.matcher(getActivityChainString(p, separator)).matches())
                .map(Person::getId)
                .toList();

        toRemove.forEach(scenario.getPopulation()::removePerson);
        new PopulationWriter(scenario.getPopulation()).write(outputPath);

        System.out.println(String.format("Removed %d out of %d persons", toRemove.size(), initialSize));
    }

    private static String getActivityChainString(Person person, String separator) {
        List<String> activityTypes = person.getSelectedPlan().getPlanElements().stream()
                .filter(planElement -> planElement instanceof Activity)
                .map(planElement -> (Activity) planElement)
                .map(Activity::getType)
                .filter(type -> !TripStructureUtils.isStageActivityType(type)).toList();
        String result = String.join(separator, activityTypes);
        return result;
    }
}
