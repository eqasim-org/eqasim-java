package org.eqasim.switzerland.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PopulationParkingInfoEnricher {
    public Population population;

    public PopulationParkingInfoEnricher(Population population) {
        this.population = population;
    }

    public void enrich(Map<Id<Person>,List<ParkingInfo>> parkingInfoMap) {

        // loop through persons for which we have parking info
        for (Id<Person> personId : parkingInfoMap.keySet()) {

            // initialize counter to -1, i.e. the first destination activity will be 0
            int activityCounter= -1;

            // loop through that persons plan
            for (PlanElement planElement : this.population.getPersons().get(personId).getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    if (!((Activity) planElement).getType().contains("interaction")) {
                        if (activityCounter >= 0) {
                            double parkingCost = parkingInfoMap.get(personId).get(activityCounter).parkingCost;
                            double populationDensity = parkingInfoMap.get(personId).get(activityCounter).populationDensity;

                            planElement.getAttributes().putAttribute("parkingCost", parkingCost);
                            planElement.getAttributes().putAttribute("populationDensity", populationDensity);
                        }

                        activityCounter++;
                    }
                }
            }
        }
    }

    public void enrich(String inputFile, String delimiter) throws IOException {
        Map<Id<Person>,List<ParkingInfo>> parkingInfoMap = new ParkingInfoReader().read(inputFile, delimiter);
        this.enrich(parkingInfoMap);
    }

}
