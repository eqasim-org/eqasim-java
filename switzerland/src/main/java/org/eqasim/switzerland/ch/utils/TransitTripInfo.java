package org.eqasim.switzerland.ch.utils;

public class TransitTripInfo {
    public final String lineId;
    public final String routeId;
    public final String departureId;

    public TransitTripInfo(String lineId, String routeId, String departureId) {
        this.lineId = lineId;
        this.routeId = routeId;
        this.departureId = departureId;
    }
}