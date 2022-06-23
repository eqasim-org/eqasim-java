package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class KraussWalkVariables  implements BaseVariables {
    public double travelTime_u_min;

    public KraussWalkVariables(double travelTime_u_min) {
        this.travelTime_u_min = travelTime_u_min;
    }
}
