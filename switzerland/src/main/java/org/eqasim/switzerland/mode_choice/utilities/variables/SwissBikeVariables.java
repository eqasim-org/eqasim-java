package org.eqasim.switzerland.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;

public class SwissBikeVariables implements BaseVariables {
//    public final Integer age;
//    public final boolean isFemale;
    public final boolean isWorkTrip;
    public final double travelTime_hour;
    public final double propS1L1;
    public final double propS2L1;
    public final double propS3L1;
    public final double propS4L1;
    public final double propS1L2;
    public final double propS2L2;
    public final double propS3L2;
    public final double propS4L2;
    public final double routedDistance;

    public SwissBikeVariables(boolean isWorkTrip, double travelTime_hour,
                              double propS1L1, double propS2L1, double propS3L1, double propS4L1, double propS1L2,
                              double propS2L2, double propS3L2, double propS4L2, double routedDistance){

        this.isWorkTrip = isWorkTrip;
        this.travelTime_hour = travelTime_hour;
        this.propS1L1=propS1L1;
        this.propS2L1=propS2L1;
        this.propS3L1=propS3L1;
        this.propS4L1=propS4L1;
        this.propS1L2=propS1L2;
        this.propS2L2=propS2L2;
        this.propS3L2=propS3L2;
        this.propS4L2=propS4L2;
        this.routedDistance=routedDistance;
    }

}
