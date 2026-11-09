package org.eqasim.core.components.traffic.bike;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.eqasim.core.components.routing.TravelTimeAwareDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.Random;

public class BikeTravelDisutility implements TravelTimeAwareDisutility {

    private final TravelTime travelTimeCalculator;
    private final Random random;
    private final BikeLinkData linkData;
    private final double sigmaNoise = 0.03;

    public BikeTravelDisutility(TravelTime travelTimeCalculator) {
        this(travelTimeCalculator, null);
    }

    public BikeTravelDisutility(TravelTime travelTimeCalculator, BikeLinkData linkData) {
        this.travelTimeCalculator = travelTimeCalculator;
        this.linkData = linkData;
        this.random = MatsimRandom.getLocalInstance();
    }

    @Override
    public TravelTime getTravelTimeCalculator() {
        return travelTimeCalculator;
    }

    @Override
    public double getLinkTravelDisutility(Link link, double v, Person person, Vehicle vehicle) {
        double travelTime = travelTimeCalculator.getLinkTravelTime(link, v, person, vehicle);
        return getLinkTravelDisutility(link, v, person, vehicle, travelTime);
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle, double travelTime) {
        double disutility = linkData == null
                ? travelTime + BikeDisutilities.linkTypeDisutility(link, travelTime)
                    + BikeDisutilities.gradientDisutility(link, travelTime)
                    + BikeDisutilities.distanceDisutility(link, travelTime)
                : linkData.disutility(link, travelTime);
        // add randomness
        double noise = sigmaNoise * random.nextGaussian();

        return disutility * (1.0 + noise);
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 0.0;
    }
}
