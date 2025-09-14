package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.mode_availability;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.core.population.PersonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


public class SwissDetailedModeAvailability implements ModeAvailability {
    @Override
    public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
        Boolean isFreight = (Boolean) person.getAttributes().getAttribute("isFreight");

        if (isFreight != null && isFreight) {
            return Collections.singleton("truck");
        }

        Collection<String> modes = new HashSet<>();

        // Modes that are always available
        modes.add(TransportMode.walk);
        modes.add(TransportMode.pt);

        // Check car availability
        boolean carAvailability = true;

        if (PersonUtils.getLicense(person).equals("no")) {
            carAvailability = false;
        }

        if (PersonUtils.getCarAvail(person).equals("never")) {
            carAvailability = false;
        }

        if (carAvailability) {
            modes.add(TransportMode.car);
        }

        // Check bike availability
        boolean bikeAvailability = true;

        if (person.getAttributes().getAttribute("bikeAvailability").equals("FOR_NONE")) {
            bikeAvailability = false;
        }

        if (bikeAvailability) {
            modes.add(TransportMode.bike);
        }

        // Add special mode "outside" if applicable
        Boolean isOutside = (Boolean) person.getAttributes().getAttribute("outside");

        if (isOutside != null && isOutside) {
            modes.add("outside");
        }

        // Add special mode "car_passenger" if applicable
        boolean carPassengerAvailability = !"never".equals(PersonUtils.getCarAvail(person));
        Boolean isCarPassenger = (Boolean) person.getAttributes().getAttribute("isCarPassenger");
        if (isCarPassenger != null && isCarPassenger) {
            carPassengerAvailability = true;
        }

        if (carPassengerAvailability) {
            modes.add("car_passenger");
        }

        return modes;
    }
}