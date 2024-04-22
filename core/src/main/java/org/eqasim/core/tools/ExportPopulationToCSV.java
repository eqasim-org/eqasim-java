package org.eqasim.core.tools;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ExportPopulationToCSV {

    public static final String[] IGNORED_ATTRIBUTES = new String[]{"vehicles"};

    public static void exportPopulationToCSV(Population population, String filePath) {

        List<String> ignoredAttributes = List.of(IGNORED_ATTRIBUTES);

        List<String> attributes = population.getPersons().values().stream()
                .flatMap(p -> p.getAttributes().getAsMap().keySet().stream())
                .distinct()
                .filter(attribute -> !ignoredAttributes.contains(attribute))
                .collect(Collectors.toList());

        String[] header = new String[attributes.size()+1];
        header[0] = "person_id";
        for(int i=0; i<attributes.size(); i++) {
            header[i+1] = attributes.get(i);
        }

        try {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(String.join(";", header) + "\n");
            for(Person person: population.getPersons().values()) {
                String[] line = new String[attributes.size()+1];
                line[0] = person.getId().toString();
                for(int i=0; i<attributes.size(); i++) {
                    line[i+1] = String.valueOf(person.getAttributes().getAsMap().getOrDefault(attributes.get(i), null));
                }
                fileWriter.write(String.join(";", line) + "\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args).requireOptions("plans-path", "output-path").build();

        String plansPath = commandLine.getOptionStrict("plans-path");
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new PopulationReader(scenario).readFile(plansPath);

        exportPopulationToCSV(scenario.getPopulation(), commandLine.getOptionStrict("output-path"));
    }
}
