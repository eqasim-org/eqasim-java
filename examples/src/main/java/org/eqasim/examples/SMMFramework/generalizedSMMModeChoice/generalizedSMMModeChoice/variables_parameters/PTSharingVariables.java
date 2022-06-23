package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class PTSharingVariables implements BaseVariables {
    // Variables for PT
    public double travelTime_u_min;

    public PTSharingVariables(double travelTime_u_min, double access_Time, double egress_Time, double cost, double crowding, int transfers, double travelTime_u_min_Sharing, double access_Time_Sharing, double egress_Time_Sharing, double parkingTime_u_min_Sharing, int freeFloating, double cost_Sharing, int pedelec, double battery, double availability) {
        this.travelTime_u_min = travelTime_u_min;
        this.access_Time = access_Time;
        this.egress_Time = egress_Time;
        this.cost = cost;
        this.crowding = crowding;
        this.transfers = transfers;
        this.travelTime_u_min_Sharing = travelTime_u_min_Sharing;
        this.access_Time_Sharing = access_Time_Sharing;
        this.egress_Time_Sharing = egress_Time_Sharing;
        this.parkingTime_u_min_Sharing = parkingTime_u_min_Sharing;
        this.freeFloating = freeFloating;
        this.cost_Sharing = cost_Sharing;
        this.pedelec = pedelec;
        this.battery = battery;
        this.availability = availability;
    }

    public double access_Time;
    public double egress_Time;
    public double cost;
    public double crowding;
    public int transfers;

    // Variables For BikeSharing
    public double travelTime_u_min_Sharing;
    public double access_Time_Sharing;
    public double egress_Time_Sharing;

    public double parkingTime_u_min_Sharing;

    public int freeFloating;
    public double cost_Sharing;
    public int pedelec;
    public double battery;
    public double availability;




}
