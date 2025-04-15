package org.eqasim.core.tools;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

public class FilterPopulationByTourLength {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args).requireOptions("input-path", "output-path", "max-length").build();

        String inputPath = commandLine.getOptionStrict("input-path");
        String outputPath = commandLine.getOptionStrict("output-path");
        int maxLength = Integer.parseInt(commandLine.getOptionStrict("max-length"));

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new PopulationReader(scenario).readFile(inputPath);


        TourFinder tourFinder = new ActivityTourFinder(List.of("home"));
        TripListConverter tripListConverter = new TripListConverter();

        IdSet<Person> toRemove = new IdSet<>(Person.class);

        for(Person person: scenario.getPopulation().getPersons().values()) {
            List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(person.getSelectedPlan());
            for(List<DiscreteModeChoiceTrip> tour: tourFinder.findTours(trips)) {
                if(tour.size() > maxLength) {
                    toRemove.add(person.getId());
                    break;
                }
            }
        }

        int initialSize = scenario.getPopulation().getPersons().size();
        toRemove.forEach(scenario.getPopulation()::removePerson);

        new PopulationWriter(scenario.getPopulation()).write(outputPath);

        System.out.println(String.format("Removed %d out of %d persons", toRemove.size(), initialSize));

    }
}
