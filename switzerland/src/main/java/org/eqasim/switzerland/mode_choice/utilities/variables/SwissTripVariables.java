package org.eqasim.switzerland.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;


public class SwissTripVariables implements BaseVariables {

    public final boolean isWorkTrip;

    public SwissTripVariables(boolean isWorkTrip){

        this.isWorkTrip = isWorkTrip;
    }

}