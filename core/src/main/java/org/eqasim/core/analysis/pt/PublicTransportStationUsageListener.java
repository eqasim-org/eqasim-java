package org.eqasim.core.analysis.pt;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.HashMap;
import java.util.Map;

public class PublicTransportStationUsageListener implements GenericEventHandler {
    private final Map<Id<TransitStopFacility>, PublicTransportStationUsageItem> usagesMap = new HashMap<>();
    private final TransitSchedule transitSchedule;


    public PublicTransportStationUsageListener(TransitSchedule transitSchedule) {
        this.transitSchedule = transitSchedule;
    }

    @Override
    public void handleEvent(GenericEvent event) {
        if(event instanceof PublicTransitEvent) {
            PublicTransitEvent publicTransitEvent = (PublicTransitEvent) event;
            PublicTransportStationUsageItem.initOrAddAccess(this.transitSchedule.getFacilities().get(publicTransitEvent.getAccessStopId()), this.usagesMap);
            PublicTransportStationUsageItem.initOrAddEgress(this.transitSchedule.getFacilities().get(publicTransitEvent.getEgressStopId()), this.usagesMap);
        }
    }

    public Map<Id<TransitStopFacility>, PublicTransportStationUsageItem> getUsagesMap() {
        return this.usagesMap;
    }

    @Override
    public void reset(int iteration) {
        usagesMap.clear();
    }
}
