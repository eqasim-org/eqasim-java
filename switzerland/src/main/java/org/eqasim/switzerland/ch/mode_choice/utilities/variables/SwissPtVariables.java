package org.eqasim.switzerland.ch.mode_choice.utilities.variables;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Zone;

public class SwissPtVariables implements BaseVariables  {
    
	public Set<Zone> zones = new HashSet<Zone>();

    public SwissPtVariables(Set<Zone> visitedZones){
        this.zones = visitedZones;
        
    }
    
}