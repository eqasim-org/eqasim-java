package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class KraussEScooterVariables implements BaseVariables {
    public KraussEScooterVariables(double travelTime_u_min, double access_Time, double egress_Time, double parkingTime_u_min, int freeFloating, double battery, double availability,double cost) {
        this.travelTime_u_min = travelTime_u_min;
        this.access_Time = access_Time;
        this.egress_Time = egress_Time;
        this.parkingTime_u_min = parkingTime_u_min;
        this.freeFloating = freeFloating;
        this.cost=cost;
        Battery = battery;
        Availability = availability;
    }
    public double cost;
    public double travelTime_u_min ;
    public double access_Time ;
    public double egress_Time;
    public double parkingTime_u_min ;
    public int freeFloating;
    public double Battery;
    public double Availability;

}
