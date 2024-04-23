package org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.TransitWithAbstractAbstractAccessModuleConfigGroup;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.core.config.Config;

import java.util.*;

public class TransitWithAbstractAccessModeAvailabilityWrapper implements ModeAvailability {

    private final static Logger logger = LogManager.getLogger(TransitWithAbstractAccessModeAvailabilityWrapper.class);
    private final Set<String> extraModes;
    private final ModeAvailability delegate;

    public TransitWithAbstractAccessModeAvailabilityWrapper(Config config, ModeAvailability delegate) {
        this((TransitWithAbstractAbstractAccessModuleConfigGroup) config.getModules().get(TransitWithAbstractAbstractAccessModuleConfigGroup.GROUP_NAME), delegate);
    }

    public TransitWithAbstractAccessModeAvailabilityWrapper(TransitWithAbstractAbstractAccessModuleConfigGroup config, ModeAvailability delegate) {
        this.delegate = delegate;
        this.extraModes = new HashSet<>();
        if(config != null) {
            this.extraModes.add(config.getModeName());
        } else {
            logger.warn(String.format("No '%s' config group was provided", TransitWithAbstractAbstractAccessModuleConfigGroup.GROUP_NAME));
        }
    }

    @Override
    public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
        Collection<String> modes = this.delegate.getAvailableModes(person, trips);
        modes.addAll(this.extraModes);
        return modes;
    }
}
