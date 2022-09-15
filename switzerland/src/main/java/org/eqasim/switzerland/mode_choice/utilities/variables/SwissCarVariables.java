package org.eqasim.switzerland.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;

public class SwissCarVariables extends CarVariables {

    public final double routedDistance_km;

    public SwissCarVariables (CarVariables delegate, double routedDistance_km){
        super(delegate.travelTime_min,delegate.cost_MU, delegate.euclideanDistance_km, delegate.accessEgressTime_min);
        this.routedDistance_km=routedDistance_km;
    }

}


