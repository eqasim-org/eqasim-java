package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.constraints;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Class finds the closest PT station to an activity or facility based on a qTree of PT Stations
 */
public class SMMPTStationFinder {
    private final QuadTree<TransitStopFacility> stopsFacilitiesQT;

    public SMMPTStationFinder(QuadTree<TransitStopFacility> stopsFacilitiesQT) {
        this.stopsFacilitiesQT = stopsFacilitiesQT;

    }
    // Based on Activity location
    public Facility getPTStation(Person person, Activity origActivity, Network network) {
        TransitStopFacility closestStopFacility=stopsFacilitiesQT.getClosest(origActivity.getCoord().getX(),origActivity.getCoord().getY());
        return closestStopFacility;
    }
    public Facility getPTStation(Activity origActivity, Network network) {
        TransitStopFacility closestStopFacility=stopsFacilitiesQT.getClosest(origActivity.getCoord().getX(),origActivity.getCoord().getY());

        return closestStopFacility;
    }
    // Based on Facility location
    public Facility getPTStation(Person person, Facility fromFacility, Network network) {
        TransitStopFacility closestStopFacility=stopsFacilitiesQT.getClosest(fromFacility.getCoord().getX(),fromFacility.getCoord().getY());

        return closestStopFacility;

    }


}
