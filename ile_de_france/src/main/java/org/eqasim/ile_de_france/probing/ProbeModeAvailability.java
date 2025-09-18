package org.eqasim.ile_de_france.probing;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

public class ProbeModeAvailability implements ModeAvailability {
    static public final String ATTRIBUTE = "probe:modeAvailability";

    @Override
    public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
        String raw = (String) person.getAttributes().getAttribute(ATTRIBUTE);

        Set<String> modes = new HashSet<>();

        for (String mode : raw.split(",")) {
            modes.add(mode.strip());
        }

        return modes;
    }
}
