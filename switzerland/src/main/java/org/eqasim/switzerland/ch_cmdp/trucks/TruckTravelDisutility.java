package org.eqasim.switzerland.ch_cmdp.trucks;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;
import org.eqasim.core.tools.random.Normal;
import org.eqasim.switzerland.ch_cmdp.tolls.Tolls;
import org.eqasim.switzerland.ch_cmdp.tolls.MarginalCostOfTolls;
import org.matsim.core.router.util.TravelTime;

/** Self-contained truck cost in seconds. Edit the formula/helpers here to change truck preferences. */
public final class TruckTravelDisutility implements TravelDisutility {
    private final TravelTime travelTime;
    private final double distanceWeight;
    private final double sigma;
    private final long seed;
    private final Tolls tolls;
    private final MarginalCostOfTolls marginalCostOfTolls;

    public TruckTravelDisutility(TravelTime travelTime, double distanceWeight, double sigma, long seed,
            Tolls tolls, MarginalCostOfTolls marginalCostOfTolls) {
        this.travelTime = travelTime;
        this.distanceWeight = distanceWeight;
        this.sigma = sigma;
        this.seed = seed;
        this.tolls = tolls;
        this.marginalCostOfTolls = marginalCostOfTolls;
    }

    @Override public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        // No car disutility or RoutingPenalty is called. Only the observed travel-time input is shared.
        double timeCost = travelTime.getLinkTravelTime(link, time, person, vehicle);
        double distanceCost = distanceCost(link);
        return (timeCost + distanceCost) * routingMultiplier(link, person)
                // + tollCost(link, vehicle)
                + roadClassPenalty(link);
    }

    @Override public double getLinkMinimumTravelDisutility(Link link) {
        // Tolls are nonnegative. Account for the random multiplier's floor to keep ALT bounds valid.
        double minimumMultiplier = sigma > 1e-6 ? 1e-3 : 1.0;
        return (link.getLength() / link.getFreespeed() + distanceCost(link)) * minimumMultiplier
                + roadClassPenalty(link);
    }

    private double distanceCost(Link link) { return distanceWeight * (link.getLength() / 100.0); }

    private double routingMultiplier(Link link, Person person) {
        if (sigma <= 1e-6) return 1.0;
        return Math.max(1.0 + sigma * Normal.get(link.getId().toString(), person.getId().toString(), seed), 1e-3);
    }

    private double tollCost(Link link, Vehicle vehicle) {
        double toll = tolls.getToll(link);
        return toll > 0 ? toll * marginalCostOfTolls.getMarginalCostOfTolls(vehicle) : 0.0;
    }

    /** Read current freespeed on every call so freespeed calibration cannot leave stale penalties. */
    public static double roadClassPenalty(Link link) {
        Object highway = link.getAttributes().getAttribute("osm:way:highway");
        double fraction = switch (highway == null ? "" : highway.toString()) {
            case "motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link" -> 0.0;
            case "secondary", "secondary_link" -> 0.10;
            case "tertiary", "tertiary_link" -> 0.15;
            default -> 0.20;
        };
        // Keep the surcharge time-independent, including for the router's lower-bound calculation.
        return fraction == 0 ? 0 : fraction * link.getLength() / link.getFreespeed();
    }
}
