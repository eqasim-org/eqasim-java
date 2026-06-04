package org.eqasim.core.components.network_calibration.demand_calibration;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

public class Tools {

    static public Coord getHomeLocation(Person person) {
        Double homeX = (Double) person.getAttributes().getAttribute("home_x");
        Double homeY = (Double) person.getAttributes().getAttribute("home_y");

        if (homeX == null || homeY == null) {
            homeX = 0.0;
            homeY = 0.0;

            for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                if (element instanceof Activity) {
                    Activity activity = (Activity) element;

                    if (activity.getType().equals("home")) {
                        homeX = activity.getCoord().getX();
                        homeY = activity.getCoord().getY();
                    }
                }
            }

            person.getAttributes().putAttribute("home_x", homeX);
            person.getAttributes().putAttribute("home_y", homeY);
        }

        return new Coord(homeX, homeY);
    }

    // ################## CAR ASCs ##################
    static public double getCarASC(Person person) {
        Object asc = person.getAttributes().getAttribute("carASC");
        return asc instanceof Double ? (Double) asc : 0.0;
    }

    static public void setCarASC(Person person, double value) {
        person.getAttributes().putAttribute("carASC", value);
    }

    static public void incrementCarASC(Person person, double delta) {
        double currentAsc = getCarASC(person);
        setCarASC(person, currentAsc + delta);
    }


}
