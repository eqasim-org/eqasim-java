package org.eqasim.examples.Drafts.DGeneralizedMultimodal.sharingPt;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Map;

public class PTStationFinder {
    private final Map<Id<TransitStopFacility>, TransitStopFacility> stopsFacilities;

    public PTStationFinder(Map<Id<TransitStopFacility>, TransitStopFacility>stopsFacilities) {
        this.stopsFacilities = stopsFacilities;
    }
    // Based on Activity location
    public Facility getPTStation(Person person, Activity origActivity, Network network) {
        double minDist = 999999999.0;
        double distance = 0.0;
        Coord homeXY = null;
        TransitStopFacility closestStopFacility=null;
//        for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
//            if (element instanceof Activity) {
//                Activity activity = (Activity) element;
//
//                if (activity.getType().equals("home")) {
//                    homeXY = activity.getCoord();
//                }
//
//            }
//
//        }

        for (Id<TransitStopFacility> stop:stopsFacilities.keySet()) {
            distance = CoordUtils.calcEuclideanDistance(stopsFacilities.get(stop).getCoord(), origActivity.getCoord());
            if (minDist > distance) {
                minDist = distance;
                closestStopFacility=stopsFacilities.get(stop);
            }
        }


        return closestStopFacility;
    }
    public Facility getPTStation(Activity origActivity, Network network) {
        double minDist = 999999999.0;
        double distance = 0.0;
        Coord homeXY = null;
        TransitStopFacility closestStopFacility=null;

        for (Id<TransitStopFacility> stop:stopsFacilities.keySet()) {
            distance = CoordUtils.calcEuclideanDistance(stopsFacilities.get(stop).getCoord(), origActivity.getCoord());
            if (minDist > distance) {
                minDist = distance;
                closestStopFacility=stopsFacilities.get(stop);
            }
        }


        return closestStopFacility;
    }
    // Based on Facility location
    public Facility getPTStation(Person person, Facility fromFacility, Network network) {
        double minDist = 999999999.0;
        double distance = 0.0;
        Coord homeXY = null;
        TransitStopFacility closestStopFacility=null;


        for (Id<TransitStopFacility> stop:stopsFacilities.keySet()) {
            distance = CoordUtils.calcEuclideanDistance(stopsFacilities.get(stop).getCoord(), fromFacility.getCoord());
            if (minDist > distance) {
                minDist = distance;
                closestStopFacility=stopsFacilities.get(stop);
            }
        }


        return closestStopFacility;
    }


}
