package org.eqasim.switzerland;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

public class ChangePopulationBikeAvailability {
    static public void main(String[] args) throws IOException {

        // create empty config
        Config config = ConfigUtils.createConfig();
        // create scenario from config
        Scenario scenario = ScenarioUtils.createScenario(config);
        // get population from scenario
        Population population = scenario.getPopulation();

        PopulationReader populationReader = new PopulationReader(scenario);
        // path to MATSim scenario
        String path = args[0];
        populationReader.readFile(path + "zurich_population_100pct.xml.gz");

        int bikeNotAvail = 0;
        int totalPop = 0;
        // loop through all people in population file
        for (Person person : population.getPersons().values()) {
            totalPop+=1;
            if (person.getAttributes().getAttribute("bikeAvailability").equals("FOR_NONE")){
                bikeNotAvail+=1;
                person.getAttributes().putAttribute("bikeAvailability","NEW_AVAIL");
            }}


        new PopulationWriter(population).write(path +"zurich_population_100pct_newavailbike.xml.gz");

        System.out.println("number of people with no bikes: "+ bikeNotAvail);
        System.out.println("number of people total: "+ totalPop);


    }
}

//    1pct: number of people with no bikes: 2857
//        number of people total: 12344

//        number of people with no bikes: 28590
//        number of people total: 123436