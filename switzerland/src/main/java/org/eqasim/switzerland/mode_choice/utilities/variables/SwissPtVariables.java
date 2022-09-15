package org.eqasim.switzerland.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;

public class SwissPtVariables extends PtVariables {

    public final double routedInVehicleDistance_km;

    public SwissPtVariables (PtVariables delegate, double routedInVehicleDistance_km){
        super(delegate.inVehicleTime_min, delegate.waitingTime_min, delegate.accessEgressTime_min,
                delegate.numberOfLineSwitches, delegate.euclideanDistance_km, delegate.cost_MU);
        this.routedInVehicleDistance_km=routedInVehicleDistance_km;

    }

}

