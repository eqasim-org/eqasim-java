package org.eqasim.core.components.transit_with_abstract_access.analysis;

import org.eqasim.core.components.transit_with_abstract_access.abstract_access.AbstractAccessItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class AbstractAccessLegItem {
    public Id<Person> personId;
    public int personTripId;
    public int legIndex;
    public Id<AbstractAccessItem> abstractAccessItemId;
    public Id<TransitStopFacility> transitStopFacilityId;
    public boolean leavingCenterStop;

    public AbstractAccessLegItem(Id<Person> personId, int personTripId, int legIndex, Id<AbstractAccessItem> abstractAccessItemId, Id<TransitStopFacility> transitStopFacilityId, boolean leavingCenterStop) {
        this.personId = personId;
        this.personTripId = personTripId;
        this.legIndex = legIndex;
        this.abstractAccessItemId = abstractAccessItemId;
        this.transitStopFacilityId = transitStopFacilityId;
        this.leavingCenterStop = leavingCenterStop;
    }
}
