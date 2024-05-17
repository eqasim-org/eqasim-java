package org.eqasim.core.analysis.cba.analyzers.ptAnalysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

final class PtTrip {
    static final class TripSegment {
        Id<Vehicle> vehicleId;
        double travelTime;
        double distance;
        Id<TransitRoute> transitRouteId;
        double vehicleDepartureTime=-1;
        final String mode;
        double waitingTime;

        TripSegment(String mode, Id<Vehicle> vehicleId, double travelTime, double distance, double waitingTime) {
            this.vehicleId = vehicleId;
            this.travelTime = travelTime;
            this.distance = distance;
            this.mode = mode;
            this.waitingTime = waitingTime;
        }
    }

    Id<Person> personId;
    List<TripSegment> segments;
    ActivityEndEvent previousActivityEnd;
    ActivityStartEvent nextActivityStart;

    PtTrip(Id<Person> personId) {
        this.personId = personId;
        this.segments = new ArrayList<>();
    }

    public TripSegment getLastSegment() {
        if(this.segments.size() > 0) {
            return this.segments.get(this.segments.size()-1);
        }
        return null;
    }

    public String getPurpose() {
        return this.previousActivityEnd.getActType() + " -- " + nextActivityStart.getActType();
    }

    public double getAccessTime() {
        if(this.segments.size() > 0 && this.segments.get(0).mode.equals("walk")){
            return this.segments.get(0).travelTime;
        }
        return 0.0;
    }
    public double getEgressTime() {
        if(this.segments.size() > 0 && this.segments.get(this.segments.size()-1).mode.equals("walk")) {
            return this.segments.get(this.segments.size()-1).travelTime;
        }
        return 0.0;
    }

    public double getTransferTime(){
        double transferTime=0;
        for(int i=1; i<segments.size()-1; i++) {
            if(this.segments.get(i).mode.equals("walk")){
                transferTime+=this.segments.get(i).travelTime;
            }
        }
        return transferTime;
    }

    public double getInVehicleTime() {
        double result=0;
        for(TripSegment segment: this.segments) {
            if(!segment.mode.equals("walk")) {
                result+=segment.travelTime;
            }
        }
        return result;
    }

    public double getTotalWaitingTime() {
        double result=0;
        for(TripSegment segment: this.segments) {
            if(!segment.mode.equals("walk")) {
                result+=segment.waitingTime;
            }
        }
        return result;
    }

    public double getSegmentAccessTime(int segmentIndex) {
        if(segmentIndex > 0 && this.segments.get(segmentIndex-1).mode.equals("walk")) {
            return this.segments.get(segmentIndex-1).travelTime;
        }
        return 0;
    }

    public double getSegmentEgressTime(int segmentIndex) {
        if(segmentIndex < this.segments.size()-1 && this.segments.get(segmentIndex+1).mode.equals("walk")) {
            return this.segments.get(segmentIndex+1).travelTime;
        }
        return 0;
    }
}
