package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;

public interface PtStageCostCalculator {
    
    /**
     * Calculates the price for a PT trip consisting of multiple legs.
     *
     * @param legs the list of PT legs (e.g. one trip can contain multiple legs)
     * @return the total ticket price
     */
    double calculatePrice(String authorityId, List<SwissPtLegVariables> legs, boolean hasHalbtax, PersonVariables personVariables);
}