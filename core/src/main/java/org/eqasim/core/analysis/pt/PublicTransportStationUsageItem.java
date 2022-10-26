package org.eqasim.core.analysis.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Map;

/**
 * A class to store statistics about passenger traffic in transit stops.
 * Stored information are nbAccesses and nbEgresses representing the number of passengers that have used the station as access or egress station respectively
 */
public class PublicTransportStationUsageItem {

    private TransitStopFacility transitStopFacility;
    private int nbAccesses = 0;
    private int nbEgresses = 0;

    public PublicTransportStationUsageItem(TransitStopFacility transitStopFacility) {
        this.transitStopFacility = transitStopFacility;
    }

    public PublicTransportStationUsageItem(TransitStopFacility transitStopFacility, int initialNbAccess, int initialNbEgresses) {
        this(transitStopFacility);
        this.nbAccesses = initialNbAccess;
        this.nbEgresses = initialNbEgresses;
    }

    public void addAccess() {
        this.nbAccesses += 1;
    }

    public void addEgress() {
        this.nbEgresses += 1;
    }


    public int getNbAccesses() {
        return this.nbAccesses;
    }

    public int getNbEgresses() {
        return this.nbEgresses;
    }

    public TransitStopFacility getTransitStopFacility() {
        return this.transitStopFacility;
    }

    public static PublicTransportStationUsageItem initIfAbsent(TransitStopFacility transitStopFacility, Map<Id<TransitStopFacility>, PublicTransportStationUsageItem> usagesMap) {
        if(!usagesMap.containsKey(transitStopFacility.getId())) {
            usagesMap.put(transitStopFacility.getId(), new PublicTransportStationUsageItem(transitStopFacility));
        }
        return usagesMap.get(transitStopFacility.getId());
    }

    public static void initOrAddAccess(TransitStopFacility transitStopFacility, Map<Id<TransitStopFacility>, PublicTransportStationUsageItem> usagesMap) {
        initIfAbsent(transitStopFacility, usagesMap).addAccess();
    }

    public static void initOrAddEgress(TransitStopFacility transitStopFacility, Map<Id<TransitStopFacility>, PublicTransportStationUsageItem> usagesMap) {
        initIfAbsent(transitStopFacility, usagesMap).addEgress();
    }
}
