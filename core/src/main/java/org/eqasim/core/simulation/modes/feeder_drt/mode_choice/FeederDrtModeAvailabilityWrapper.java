package org.eqasim.core.simulation.modes.feeder_drt.mode_choice;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.modes.feeder_drt.config.MultiModeFeederDrtConfigGroup;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.core.config.Config;

import java.util.*;
import java.util.stream.Collectors;

public class FeederDrtModeAvailabilityWrapper implements ModeAvailability {
    private final static Logger logger = LogManager.getLogger(FeederDrtModeAvailabilityWrapper.class);
    private final Set<String> extraModes;
    private final ModeAvailability delegate;

    public FeederDrtModeAvailabilityWrapper(Config config, ModeAvailability delegate) {
        this((MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME), (MultiModeFeederDrtConfigGroup) config.getModules().get(MultiModeFeederDrtConfigGroup.GROUP_NAME), delegate);
    }

    public FeederDrtModeAvailabilityWrapper(MultiModeDrtConfigGroup multiModeDrtConfigGroup, MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup, ModeAvailability delegate) {
        this.delegate = delegate;
        Collection<String> coveredDrtModes;
        if(multiModeFeederDrtConfigGroup == null) {
            this.extraModes = new HashSet<>();
            coveredDrtModes = Collections.emptyList();
        } else {
            this.extraModes = multiModeFeederDrtConfigGroup.modes().collect(Collectors.toSet());
            coveredDrtModes = multiModeFeederDrtConfigGroup.getModalElements().stream().map(cfg -> cfg.accessEgressModeName).toList();
        }
        if(multiModeDrtConfigGroup == null) {
            if(multiModeFeederDrtConfigGroup != null) {
                logger.warn(String.format("A %s config was supplied but a %s was not, DRT modes not covered by a feeder mode will not be considered", MultiModeFeederDrtConfigGroup.GROUP_NAME, MultiModeDrtConfigGroup.GROUP_NAME));
            }
        } else {
            this.extraModes.addAll(multiModeDrtConfigGroup.modes().filter(mode -> !coveredDrtModes.contains(mode)).toList());
        }
    }

    @Override
    public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
        Collection<String> modes = this.delegate.getAvailableModes(person, trips);
        modes.addAll(this.extraModes);
        return modes;
    }
}
