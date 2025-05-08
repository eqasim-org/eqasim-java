package org.eqasim.switzerland.ch.utils.pt;

public class TransitTripInfo {
    public final String lineId;
    public final String lineName;
    public final String routeId;
    public final String departureId;

    public TransitTripInfo(String lineId, String lineName, String routeId, String departureId) {
        this.lineId = lineId;
        this.routeId = routeId;
        this.departureId = departureId;
        this.lineName = lineName;
    }
}