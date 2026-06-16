package org.eqasim.core.components.network_calibration.demand_calibration;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.population.PersonUtils;

import java.util.List;

public class Tools {

    static public Coord getHomeLocation(Person person) {
        Double homeX = (Double) person.getAttributes().getAttribute("home_x");
        Double homeY = (Double) person.getAttributes().getAttribute("home_y");

        if (homeX == null || homeY == null) {
            homeX = (Double) person.getAttributes().getAttribute("home_coordinate_x");
            homeY = (Double) person.getAttributes().getAttribute("home_coordinate_y");
        }

        if (homeX == null || homeY == null) {
            homeX = 0.0;
            homeY = 0.0;

            for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                if (element instanceof Activity) {
                    Activity activity = (Activity) element;

                    if (activity.getType().equalsIgnoreCase("home") || activity.getType().equalsIgnoreCase("h")) {
                        homeX = activity.getCoord().getX();
                        homeY = activity.getCoord().getY();
                        break;
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
        return asc instanceof Double ? (double) asc : 0.0;
    }

    static public void setCarASC(Person person, double value) {
        person.getAttributes().putAttribute("carASC", value);
    }

    static public void setCarASCIfDoesntExist(Person person, double value) {
        double personAsc = getCarASC(person);
        if (Math.abs(personAsc)<1e-3) {
            setCarASC(person, value);
        }
    }

    static public void incrementCarASC(Person person, double delta) {
        double currentAsc = getCarASC(person);
        setCarASC(person, currentAsc + delta);
    }

    static public void incrementCarASC(Person person, double delta, double maxAsc) {
        double currentAsc = getCarASC(person);
        double clippedAsc = Math.max(Math.min(maxAsc, currentAsc+delta),-maxAsc);
        setCarASC(person, clippedAsc);
    }

    static public boolean isCarAvailable(Person person){
        Object hasLicense = PersonUtils.getLicense(person);
        Object hasCar = PersonUtils.getCarAvail(person);

        if (hasLicense == null || hasCar == null){
            return true; // cross-border or freight traffic
        }

        return !("no".equals(hasLicense) || "never".equals(hasCar));
    }

    static public boolean isInSubPopulation(Person person){
        Boolean isCrossBorder = (Boolean) person.getAttributes().getAttribute("isCrossBorder");
        Boolean isFreight = (Boolean) person.getAttributes().getAttribute("isFreight");
        return ((isCrossBorder != null && isCrossBorder) || (isFreight != null && isFreight));
    }

    static public boolean hasOutsideActivity(Person person, TripListConverter tripListConverter) {
        List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(person.getSelectedPlan());
        for (DiscreteModeChoiceTrip trip : trips) {
            String originActivity = trip.getOriginActivity().getType();
            String destinationActivity = trip.getDestinationActivity().getType();
            if (originActivity.equals("outside") || destinationActivity.equals("outside")) {
                return true;
            }
        }
        return false;
    }
}
