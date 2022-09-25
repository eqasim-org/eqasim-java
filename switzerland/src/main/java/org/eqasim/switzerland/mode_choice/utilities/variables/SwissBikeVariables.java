package org.eqasim.switzerland.mode_choice.utilities.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;

public class SwissBikeVariables implements BaseVariables {

    public final double propS1L1;
    public final double propS2L1;
    public final double propS3L1;
    public final double propS4L1;
    public final double propS1L2;
    public final double propS2L2;
    public final double propS3L2;
    public final double propS4L2;
    public final double routedDistance_km;
    public final double averageUphillGradient;
    public final double travelTime_min;


    public SwissBikeVariables(double travelTime_min,double propS1L1, double propS2L1, double propS3L1, double propS4L1, double propS1L2,
                              double propS2L2, double propS3L2, double propS4L2, double routedDistance_km, double averageUphillGradient){

        this.travelTime_min = travelTime_min;
        this.propS1L1=propS1L1;
        this.propS2L1=propS2L1;
        this.propS3L1=propS3L1;
        this.propS4L1=propS4L1;
        this.propS1L2=propS1L2;
        this.propS2L2=propS2L2;
        this.propS3L2=propS3L2;
        this.propS4L2=propS4L2;
        this.routedDistance_km=routedDistance_km;
        this.averageUphillGradient = averageUphillGradient;
    }

}
