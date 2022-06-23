package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class KraussPTVariables implements BaseVariables {
    public double travelTime_u_min;
    public double access_Time;
    public double egress_Time;
    public double cost;
    public double crowding;
    public int transfers;
    public KraussPTVariables(double travelTime_u_min, double access_Time, double egress_Time, double cost, double crowding, int transfers) {
        travelTime_u_min = travelTime_u_min;
        access_Time = access_Time;
        egress_Time = egress_Time;
        cost = cost;
        crowding = crowding;
        transfers = transfers;
    }



}
