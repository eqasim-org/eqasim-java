package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;

public class SharingPTVariables implements BaseVariables {
    // Variables for PT
    public double travelTime_u_min;
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


    public SharingPTVariables(double travelTime_u_min, double access_Time, double egress_Time, double cost, double crowding, int transfers, double travelTime_u_min_Sharing, double access_Time_Sharing, double egress_Time_Sharing, double parkingTime_u_min_Sharing, int freeFloating, double cost_Sharing, int pedelec, double battery, double availability) {
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
    public double getTravelTime_u_min() {
        return travelTime_u_min;
    }

    public void setTravelTime_u_min(double travelTime_u_min) {
        this.travelTime_u_min = travelTime_u_min;
    }

    public double getAccess_Time() {
        return access_Time;
    }

    public void setAccess_Time(double access_Time) {
        this.access_Time = access_Time;
    }

    public double getEgress_Time() {
        return egress_Time;
    }

    public void setEgress_Time(double egress_Time) {
        this.egress_Time = egress_Time;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getCrowding() {
        return crowding;
    }

    public void setCrowding(double crowding) {
        this.crowding = crowding;
    }

    public int getTransfers() {
        return transfers;
    }

    public void setTransfers(int transfers) {
        this.transfers = transfers;
    }

    public double getTravelTime_u_min_Sharing() {
        return travelTime_u_min_Sharing;
    }

    public void setTravelTime_u_min_Sharing(double travelTime_u_min_Sharing) {
        this.travelTime_u_min_Sharing = travelTime_u_min_Sharing;
    }

    public double getAccess_Time_Sharing() {
        return access_Time_Sharing;
    }

    public void setAccess_Time_Sharing(double access_Time_Sharing) {
        this.access_Time_Sharing = access_Time_Sharing;
    }

    public double getEgress_Time_Sharing() {
        return egress_Time_Sharing;
    }

    public void setEgress_Time_Sharing(double egress_Time_Sharing) {
        this.egress_Time_Sharing = egress_Time_Sharing;
    }

    public double getParkingTime_u_min_Sharing() {
        return parkingTime_u_min_Sharing;
    }

    public void setParkingTime_u_min_Sharing(double parkingTime_u_min_Sharing) {
        this.parkingTime_u_min_Sharing = parkingTime_u_min_Sharing;
    }

    public int getFreeFloating() {
        return freeFloating;
    }

    public void setFreeFloating(int freeFloating) {
        this.freeFloating = freeFloating;
    }

    public double getCost_Sharing() {
        return cost_Sharing;
    }

    public void setCost_Sharing(double cost_Sharing) {
        this.cost_Sharing = cost_Sharing;
    }

    public int getPedelec() {
        return pedelec;
    }

    public void setPedelec(int pedelec) {
        this.pedelec = pedelec;
    }

    public double getBattery() {
        return battery;
    }

    public void setBattery(double battery) {
        this.battery = battery;
    }

    public double getAvailability() {
        return availability;
    }

    public void setAvailability(double availability) {
        this.availability = availability;
    }






}
