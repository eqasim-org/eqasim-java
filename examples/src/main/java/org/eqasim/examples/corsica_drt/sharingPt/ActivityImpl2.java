package org.eqasim.examples.corsica_drt.sharingPt;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class ActivityImpl2  implements Activity {
    private double endTime ;
    private double startTime ;
    private OptionalTime dur ;
    private String type;
    private Coord coord = null;
    private Id<Link> linkId = null;
    private Id<ActivityFacility> facilityId = null;
    private final Attributes attributes = new Attributes();

    public ActivityImpl2(String type, double endTime,double startTime,Coord coord) {
        this.type = type.intern();
        this.endTime=endTime;
        this.startTime=startTime;
        this.coord=coord;
    }


    @Override
    public OptionalTime getEndTime() {
        return OptionalTime.defined(endTime);
    }

    public final void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    @Override
    public void setEndTimeUndefined() {

    }



    public final void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    @Override
    public void setStartTimeUndefined() {

    }

    @Override
    public OptionalTime getMaximumDuration() {
        return this.dur;
    }

    public final String getType() {
        return this.type;
    }

    public final void setType(String type) {
        this.type = type.intern();
    }

    public final Coord getCoord() {
        return this.coord;
    }

    @Override
    public OptionalTime getStartTime() {
        return null;
    }
    public double getStartTime2() {
        return startTime;
    }
    public double getEndTime2() {
        return endTime;
    }
    public void setCoord(Coord coord) {
        this.coord = coord;
    }

    public final Id<Link> getLinkId() {
        return this.linkId;
    }

    public final Id<ActivityFacility> getFacilityId() {
        return this.facilityId;
    }

    public final void setFacilityId(Id<ActivityFacility> facilityId) {
        this.facilityId = facilityId;
    }

    public final void setLinkId(Id<Link> linkId) {
        this.linkId = linkId;
    }

    public final String toString() {
        return "[type=" + this.getType() + "][coord=" + this.getCoord() + "][linkId=" + this.linkId + "][startTime=" + Time.writeTime(this.getStartTime()) + "][endTime=" + Time.writeTime(this.getEndTime()) + "][duration=" + Time.writeTime(this.getMaximumDuration()) + "][facilityId=" + this.facilityId + "]";
    }



    public void setMaximumDuration(double dur) {
        this.dur =  OptionalTime.defined(dur);
    }

    @Override
    public void setMaximumDurationUndefined() {

    }

    public Attributes getAttributes() {
        return this.attributes;
    }
}
