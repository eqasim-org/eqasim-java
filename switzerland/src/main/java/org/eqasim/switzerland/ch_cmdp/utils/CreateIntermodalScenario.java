package org.eqasim.switzerland.ch_cmdp.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class CreateIntermodalScenario {
    private static final String DEFAULT_PERSON_ATTRIBUTE = "bikesharingAccess";
    private static final String DEFAULT_STOP_FILTER_ATTRIBUTE = "bikeAccess";
    private static final String DEFAULT_STOP_CATEGORY_ATTRIBUTE = "stopCategory";
    private static final Set<Integer> DEFAULT_STOP_CATEGORIES = Set.of(1,2,3,4,5);
    private static final long DEFAULT_RANDOM_SEED = 0L;
    private static final String USAGE = """
            Usage:
              CreateIntermodalScenario \\
                --population-path population.xml.gz \\
                --transit-schedule-path transit_schedule.xml.gz \\
                --output-population-path population_intermodal.xml.gz \\
                --output-transit-schedule-path transit_schedule_intermodal.xml.gz \\
                --bikesharing-person-percentage 20 \\
                [--person-attribute bikesharingAccess] \\
                [--stop-filter-attribute bikeAccess] \\
                [--stop-category-attribute stopCategory] \\
                [--stop-categories 1,2,3] \\
                [--random-seed 0]
            """;

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        if (args.length == 0 || hasHelpOption(args)) {
            System.out.println(USAGE);
            return;
        }

        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("population-path", "transit-schedule-path", "output-population-path",
                        "output-transit-schedule-path", "bikesharing-person-percentage")
                .allowOptions("person-attribute", "stop-filter-attribute", "stop-category-attribute",
                        "stop-categories", "stop-category", "random-seed")
                .build();

        double personShare = parsePercentage(cmd.getOptionStrict("bikesharing-person-percentage"));
        String personAttribute = cmd.getOption("person-attribute").orElse(DEFAULT_PERSON_ATTRIBUTE);
        String stopFilterAttribute = cmd.getOption("stop-filter-attribute").orElse(DEFAULT_STOP_FILTER_ATTRIBUTE);
        String stopCategoryAttribute = cmd.getOption("stop-category-attribute").orElse(DEFAULT_STOP_CATEGORY_ATTRIBUTE);
        Set<Integer> stopCategories = cmd.getOption("stop-categories").map(CreateIntermodalScenario::parseStopCategories)
                .orElseGet(() -> cmd.getOption("stop-category").map(CreateIntermodalScenario::parseStopCategories)
                        .orElse(DEFAULT_STOP_CATEGORIES));
        long randomSeed = cmd.getOption("random-seed").map(Long::parseLong).orElse(DEFAULT_RANDOM_SEED);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(cmd.getOptionStrict("population-path"));
        new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("transit-schedule-path"));

        addPersonFilterAttribute(scenario, personAttribute, personShare, randomSeed);
        addStopFilterAttribute(scenario, stopFilterAttribute, stopCategoryAttribute, stopCategories);

        new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-population-path"));
        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(cmd.getOptionStrict("output-transit-schedule-path"));
    }

    private static void addPersonFilterAttribute(Scenario scenario, String attribute, double share, long randomSeed) {
        Random random = new Random(randomSeed);
        for (Person person : scenario.getPopulation().getPersons().values()) {
            person.getAttributes().putAttribute(attribute, random.nextDouble() < share);
        }
    }

    private static void addStopFilterAttribute(Scenario scenario, String filterAttribute, String categoryAttribute,
            Set<Integer> targetCategories) {
        for (TransitStopFacility stop : scenario.getTransitSchedule().getFacilities().values()) {
            stop.getAttributes().putAttribute(filterAttribute,
                    isTargetCategory(stop.getAttributes().getAttribute(categoryAttribute), targetCategories));
        }
    }

    private static boolean isTargetCategory(Object value, Set<Integer> targetCategories) {
        if (value instanceof Number) {
            return targetCategories.contains(((Number) value).intValue());
        }
        if (value == null) {
            return false;
        }

        try {
            return targetCategories.contains(Integer.parseInt(value.toString()));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Set<Integer> parseStopCategories(String value) {
        Set<Integer> categories = new HashSet<>();
        for (String category : value.split(",")) {
            category = category.trim();
            if (!category.isEmpty()) {
                categories.add(Integer.parseInt(category));
            }
        }
        return categories;
    }

    private static double parsePercentage(String value) throws CommandLine.ConfigurationException {
        double percentage = Double.parseDouble(value);
        if (percentage < 0.0 || percentage > 100.0) {
            throw new CommandLine.ConfigurationException("bikesharing-person-percentage must be between 0 and 100");
        }
        return percentage / 100.0;
    }

    private static boolean hasHelpOption(String[] args) {
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("--help")) {
                return true;
            }
        }
        return false;
    }
}
