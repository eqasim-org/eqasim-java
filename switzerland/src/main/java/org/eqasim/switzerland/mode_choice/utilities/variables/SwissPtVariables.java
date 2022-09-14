package org.eqasim.switzerland.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;

public class SwissPtVariables extends PtVariables {

    public final double routedDistance;

    public SwissPtVariables (PtVariables delegate, double routedDistance){
        super(delegate.inVehicleTime_min, delegate.waitingTime_min, delegate.accessEgressTime_min,
                delegate.numberOfLineSwitches, delegate.euclideanDistance_km, delegate.cost_MU);
        this.routedDistance=routedDistance;
    }

}

