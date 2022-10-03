package org.eqasim.switzerland;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CheckPopulationBikeAvailability {
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
        populationReader.readFile(path + "zurich_population_10pct.xml.gz");

        int bikeNotAvail = 0;
        int totalPop = 0;
        // loop through all people in population file
        for (Person person : population.getPersons().values()) {
            totalPop+=1;
            if (person.getAttributes().getAttribute("bikeAvailability").equals("FOR_NONE")){
                bikeNotAvail+=1;
            }}

////        List<Double> distList = new ArrayList<>();
//        int bikeTripCount = 0;
//        for (Person person : population.getPersons().values()) {
//            List<? extends PlanElement> elements = person.getSelectedPlan().getPlanElements();
//            int size = elements.size();
//            String start_coord = (String) elements.get(0).getAttributes().getAttribute("coord").toString();
//            elements.get(0).getAttributes().getAsMap().
//            String end_coord = (String) elements.get(size-1).getAttributes().getAttribute("coord");
////            Double dist = CoordUtils.calcEuclideanDistance(start_coord,end_coord);
////            if (dist < 20000){
////                distList.add(dist);
////                bikeTripCount+=1;
////            }
//            }



//            if (age >= 41 && age <= 65) {
//                if (k < 6700) {
//                    person.getAttributes().putAttribute("bsMembership", true);
//                    k += 1;
//                } else {
//                    person.getAttributes().putAttribute("bsMembership", false);
//                }
//            }

//        new PopulationWriter(population).write("zurich_population_10pct_bsmembership.xml.gz");

        System.out.println("number of people with no bikes: "+ bikeNotAvail);
        System.out.println("number of people total: "+ totalPop);

//        System.out.println("number of trips that can use bike: "+ bikeTripCount);

    }
}
//        number of people with no bikes: 28590
//        number of people total: 123436