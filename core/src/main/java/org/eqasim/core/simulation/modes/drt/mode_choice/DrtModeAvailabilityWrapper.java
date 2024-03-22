package org.eqasim.core.simulation.modes.drt.mode_choice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

import java.util.Collection;
import java.util.List;

public class DrtModeAvailabilityWrapper implements ModeAvailability {
    private final Collection<String> drtModes;
    private final ModeAvailability delegate;

    public DrtModeAvailabilityWrapper(MultiModeDrtConfigGroup multiModeDrtConfigGroup, ModeAvailability delegate) {
        this.drtModes = multiModeDrtConfigGroup.modes().toList();
        this.delegate = delegate;
    }

    @Override
    public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
        Collection<String> modes = this.delegate.getAvailableModes(person, trips);
        modes.addAll(this.drtModes);
        return modes;
    }
}
