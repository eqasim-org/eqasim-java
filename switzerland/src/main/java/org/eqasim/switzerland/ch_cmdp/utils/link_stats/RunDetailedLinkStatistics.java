package org.eqasim.switzerland.ch_cmdp.utils.link_stats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

/** Replays one iteration's events using the same statistics handler as RunSimulation. */
public class RunDetailedLinkStatistics {
    public static void main(String[] args) throws ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("events-path", "population-path", "transit-schedule-path")
                .allowOptions("output-path")
                .build();

        Path output = Path.of(cmd.getOption("output-path")
                .orElse("output_detailed_link_statistics.csv.gz")).toAbsolutePath();
        if (!output.toString().endsWith(".csv.gz")) {
            throw new IllegalArgumentException("--output-path must end with .csv.gz");
        }

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        // Use a separate streaming scenario: this reader replaces its population.
        // Retain only person IDs and the three classification attributes, never plans.
        var reader = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        reader.addAlgorithm(person -> {
            var slimPerson = scenario.getPopulation().getFactory().createPerson(person.getId());
            for (String attribute : new String[] { "isFreight", "isCrossBorder", "isExternalFR" }) {
                Object value = person.getAttributes().getAttribute(attribute);
                if (value != null) slimPerson.getAttributes().putAttribute(attribute, value);
            }
            scenario.getPopulation().addPerson(slimPerson);
        });
        reader.readFile(cmd.getOptionStrict("population-path"));
        new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("transit-schedule-path"));

        var handler = new DetailedLinkStatisticsHandler(scenario);
        var events = EventsUtils.createEventsManager();
        events.addHandler(handler);
        events.initProcessing();
        new MatsimEventsReader(events).readFile(cmd.getOptionStrict("events-path"));
        events.finishProcessing();

        Files.createDirectories(output.getParent());
        handler.write(output.toString());
        System.out.println("Detailed link statistics written to: " + output);
    }
}
