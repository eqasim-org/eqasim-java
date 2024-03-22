package org.eqasim.core.simulation.modes.feeder_drt.mode_choice;

import org.eqasim.core.simulation.modes.feeder_drt.config.MultiModeFeederDrtConfigGroup;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FeederDrtModeAvailabilityWrapper implements ModeAvailability {
    private final Set<String> extraModes;
    private final ModeAvailability delegate;

    public FeederDrtModeAvailabilityWrapper(MultiModeDrtConfigGroup multiModeDrtConfigGroup, MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup, ModeAvailability delegate) {
        this.delegate = delegate;
        this.extraModes = multiModeFeederDrtConfigGroup.modes().collect(Collectors.toSet());
        Collection<String> coveredDrtModes = multiModeFeederDrtConfigGroup.getModalElements().stream().map(cfg -> cfg.accessEgressTransitStopModes).toList();
        this.extraModes.addAll(multiModeDrtConfigGroup.modes().filter(mode -> !coveredDrtModes.contains(mode)).toList());
    }

    @Override
    public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
        Collection<String> modes = this.delegate.getAvailableModes(person, trips);
        modes.addAll(this.extraModes);
        return modes;
    }
}
