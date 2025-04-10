package org.eqasim.core.tools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.HouseholdsWriterV10;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class AddNumberOfVehiclesToHouseholds {

    public static final String SEPARATOR = ";";


    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {

        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("input-households-path", "output-households-path", "attributes-path")
                .build();

        String inputPath = commandLine.getOptionStrict("input-households-path");
        String outputPath = commandLine.getOptionStrict("output-households-path");
        String attributesPath = commandLine.getOptionStrict("attributes-path");

        String householdIdColumn = "household_id";

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(attributesPath)));

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        HouseholdsReaderV10 householdsReaderV10 = new HouseholdsReaderV10(scenario.getHouseholds());
        householdsReaderV10.readFile(inputPath);
        Households households = scenario.getHouseholds();

        String line;
        List<String> header = null;
        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(SEPARATOR));

            if (header == null) {
                header = row;
            } else {
                String householdId = row.get(header.indexOf(householdIdColumn));
                int numberOfCars = Integer.parseInt(row.get(header.indexOf("number_of_vehicles")));
                Household household = households.getHouseholds().get(Id.create(householdId, Household.class));
                if(household == null) {
                    continue;
                }
                household.getAttributes().putAttribute("number_of_vehicles", numberOfCars);
            }
        }
        reader.close();

        new HouseholdsWriterV10(households).writeFile(outputPath);
    }
}
