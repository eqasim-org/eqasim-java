package org.eqasim.ile_de_france.policies;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Home-Work distance for agents' map
 * 
 * @author akramelb
 */
public class HomeWorkDistanceModule extends AbstractModule {

    @Provides
    @Singleton
    Map<Person, Double> provideHomeWorkDistances(Population population) {
        Map<Person, Double> homeWorkDistances = new HashMap<>();

        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            Coord homeCoord = null;
            Coord workCoord = null;

            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Activity) {
                    Activity activity = (Activity) planElement;

                    if (activity.getType().equals("home") && homeCoord == null) {
                        homeCoord = activity.getCoord();
                    } else if (activity.getType().equals("work") && workCoord == null) {
                        workCoord = activity.getCoord();
                    }
                }
            }

            if (homeCoord != null && workCoord != null) {
                double distance = CoordUtils.calcEuclideanDistance(homeCoord, workCoord);
                homeWorkDistances.put(person, distance);
            }
            else {
                homeWorkDistances.put(person, 0.0);
            }
        }

        return homeWorkDistances;
    }



    @Override
    public void install() {
        // TODO Auto-generated method stub
    }

}
