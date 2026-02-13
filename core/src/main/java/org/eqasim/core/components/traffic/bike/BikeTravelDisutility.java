package org.eqasim.core.components.traffic.bike;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.Random;

public class BikeTravelDisutility implements TravelDisutility {

    private final TravelTime travelTimeCalculator;
    private final Random random;
    private double sigmaTravelTime = 0.05;
    private double sigmaDisutility = 0.1;

    public BikeTravelDisutility(TravelTime travelTimeCalculator) {
        this.travelTimeCalculator = travelTimeCalculator;
        this.random = MatsimRandom.getLocalInstance();
    }

    @Override
    public double getLinkTravelDisutility(Link link, double v, Person person, Vehicle vehicle) {
        double travelTime = travelTimeCalculator.getLinkTravelTime(link, v, person, vehicle);
        double linkTypeDisutility = BikeDisutilities.linkTypeDisutility(link, travelTime);
        double gradientDisutility = BikeDisutilities.gradientDisutility(link, travelTime);

        // add randomness to the router
        double random1 = 1.0 + sigmaTravelTime * random.nextGaussian();
        double random2  = 1.0 + sigmaDisutility * random.nextGaussian();

        return travelTime * random1 + (linkTypeDisutility + gradientDisutility) * random2;
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 0.0;
    }
}
