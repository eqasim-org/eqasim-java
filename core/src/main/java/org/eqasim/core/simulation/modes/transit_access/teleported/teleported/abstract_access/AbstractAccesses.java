package org.eqasim.core.simulation.modes.transit_access.teleported.teleported.abstract_access;

import org.matsim.api.core.v01.IdMap;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;

public class AbstractAccesses {
    public final IdMap<AbstractAccessItem, AbstractAccessItem> abstractAccessItems;
    public final IdMap<TransitStopFacility, List<AbstractAccessItem>> abstractAccessItemsByTransitStop;

    public AbstractAccesses(IdMap<AbstractAccessItem, AbstractAccessItem> abstractAccessItems) {
        this.abstractAccessItems = abstractAccessItems;
        this.abstractAccessItemsByTransitStop = AbstractAccessItem.getAbstractAccessItemsByTransitStop(this.abstractAccessItems.values());
    }

    public IdMap<AbstractAccessItem, AbstractAccessItem> getAbstractAccessItems() {
        return this.abstractAccessItems;
    }

    public IdMap<TransitStopFacility, List<AbstractAccessItem>> getAbstractAccessItemsByTransitStop() {
        return this.abstractAccessItemsByTransitStop;
    }
}
