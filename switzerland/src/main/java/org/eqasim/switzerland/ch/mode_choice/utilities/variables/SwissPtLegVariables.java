package org.eqasim.switzerland.ch.mode_choice.utilities.variables;

import java.util.Set;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Zone;

public class SwissPtLegVariables implements BaseVariables {

    public Set<Zone> zones;
    public double    departureTime;
    public double    arrivalTime;
    public double    sbbDistance;
    public double    networkDistance;
    public String    fromNode;
    public String    toNode;

    public SwissPtLegVariables(Set<Zone> visitedZones, double departureTime, double arrivalTime, double networkDistance, double sbbDistance, String fromNode, String toNode) {
		this.departureTime = departureTime;
		this.arrivalTime   = arrivalTime;
        this.sbbDistance   = sbbDistance;
        this.fromNode      = fromNode;
        this.toNode        = toNode;
        this.networkDistance = networkDistance;

        this.zones = visitedZones;
	}

    public double getDepartureTime() {
        return departureTime;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public double getSbbDistance() {
        return sbbDistance;
    }

    public double getNetworkDistance() {
        return networkDistance;
    }

    public Set<Zone> getZones() {
        return this.zones;
    }

    @Override
    public String toString() {
        return "Leg info: " +
                "zones=" + this.zones +
                ", from=" + this.fromNode +
                ", to=" + this.toNode +
                ", departureTime=" + this.departureTime +
                ", arrivalTime=" + this.arrivalTime +
                ", distance=" + this.networkDistance +
                ", sbbDistance=" + this.sbbDistance +
                ".";
    }
        
}
