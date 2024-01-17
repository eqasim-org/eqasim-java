package org.eqasim.core.simulation.modes.drt.mode_choice.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class DrtVariables implements BaseVariables {
    final public double travelTime_min;
    final public double cost_MU;
    final public double euclideanDistance_km;
    final public double waitingTime_min;
    final public double accessEgressTime_min;

    public DrtVariables(double travelTime_min, double cost_MU, double euclideanDistance_km, double waitingTime_min,
                        double accessEgressTime_min) {
        this.travelTime_min = travelTime_min;
        this.cost_MU = cost_MU;
        this.euclideanDistance_km = euclideanDistance_km;
        this.waitingTime_min = waitingTime_min;
        this.accessEgressTime_min = accessEgressTime_min;
    }
}
