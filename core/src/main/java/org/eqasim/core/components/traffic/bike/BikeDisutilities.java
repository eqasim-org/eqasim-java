package org.eqasim.core.components.traffic.bike;

import org.matsim.api.core.v01.network.Link;

public class BikeDisutilities {

    public static String getLinkType(Link link) {
        Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
        if (osmHighway != null) {
            return String.valueOf(osmHighway);
        }
        return "unknown";
    }

    public static double linkTypeDisutility(Link link, double travelTime) {
        // TODO: these parameters need calibration
        String linkType = getLinkType(link);
        switch (linkType){
            case "motorway","motorway_link", "trunk","trunk_link":
                return 0.4 * travelTime;
            case "primary", "primary_link":
                return 0.2 * travelTime;
            case "secondary", "secondary_link":
                return 0.1 * travelTime;
            default:
                return 0.0;
        }
    }

    public static double gradientDisutility(Link link, double travelTime) {
        double gradientPce = BikeGradientBasedLinkSpeedCalculator.percentageGradient(link);
        if (gradientPce > 10.0) {
            return 0.4 * travelTime;
        } else if (gradientPce > 5.0) {
            return 0.2 * travelTime;
        } else if (gradientPce > 2.0) {
            return 0.1 * travelTime;
        } else {
            return 0.0;
        }

    }


}
