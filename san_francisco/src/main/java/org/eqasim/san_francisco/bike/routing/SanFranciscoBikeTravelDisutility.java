package org.eqasim.san_francisco.bike.routing;

import org.eqasim.san_francisco.bike.reader.BikeInfo;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.Set;

public class SanFranciscoBikeTravelDisutility implements TravelDisutility {
    double costLengthWrongWay = 4.02;
    double costLengthBikePath = 0.57;
    double costLengthBikeLane = 0.49;
    double costLengthBikeRoute = 0.92;
    double costNotPermitted = 1000;

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {

        double length = link.getLength();
        double factor = 1.0;

        String bikeFacilityClass = link.getAttributes().getAttribute("bikeFacilityClass").toString();

        switch (bikeFacilityClass) {
            case "class_1":
                factor = costLengthBikePath;
                break;
            case "class_2":
                factor = costLengthBikeLane;
                break;
            case "class_3":
                factor = costLengthBikeRoute;
                break;
            default:
                break;
        }

        boolean bikeOppositeDirection = (boolean) link.getAttributes().getAttribute("bikeOppositeDirection");
        if (bikeOppositeDirection) {
            factor = costLengthWrongWay;
        }

        boolean isPermitted = (boolean) link.getAttributes().getAttribute("bikeIsPermitted");
        if (!isPermitted) {
            factor = costNotPermitted;
        }

        return factor * length;
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 0.0;
    }
}
