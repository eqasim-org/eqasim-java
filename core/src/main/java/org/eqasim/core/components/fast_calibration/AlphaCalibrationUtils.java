package org.eqasim.core.components.fast_calibration;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AlphaCalibrationUtils {
    public static Boolean isConsideredPerson(Person person) {
        Boolean isCrossBorder = (Boolean) person.getAttributes().getAttribute("isCrossBorder");
        Boolean isFreight = (Boolean) person.getAttributes().getAttribute("isFreight");
        return !((isCrossBorder != null && isCrossBorder) || (isFreight != null && isFreight));
    }

    public static Boolean isConsideredTrip(DiscreteModeChoiceTrip trip) {
        String originActivity = trip.getOriginActivity().getType();
        String destinationActivity = trip.getDestinationActivity().getType();
        if (originActivity.equals("outside") || destinationActivity.equals("outside")) {
            return false;
        }
        return true;
    }
}
