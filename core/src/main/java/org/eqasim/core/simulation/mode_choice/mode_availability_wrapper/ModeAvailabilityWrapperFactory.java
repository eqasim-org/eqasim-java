package org.eqasim.core.simulation.mode_choice.mode_availability_wrapper;

import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;

public interface ModeAvailabilityWrapperFactory {
    ModeAvailability wrap(ModeAvailability wrapped);
}
