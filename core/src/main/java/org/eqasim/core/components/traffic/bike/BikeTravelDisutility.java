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
    private double sigmaNoise = 0.03;

    public BikeTravelDisutility(TravelTime travelTimeCalculator) {
        this.travelTimeCalculator = travelTimeCalculator;
        this.random = MatsimRandom.getLocalInstance();
    }

    @Override
    public double getLinkTravelDisutility(Link link, double v, Person person, Vehicle vehicle) {
        double travelTime = travelTimeCalculator.getLinkTravelTime(link, v, person, vehicle);
        double linkTypeDisutility = BikeDisutilities.linkTypeDisutility(link, travelTime);
        double gradientDisutility = BikeDisutilities.gradientDisutility(link, travelTime);
        double distanceDisutility = BikeDisutilities.distanceDisutility(link, travelTime);
        double disutility = travelTime +linkTypeDisutility + gradientDisutility + distanceDisutility;
        // add randomness
        double noise = sigmaNoise * random.nextGaussian();

        return disutility * (1.0 + noise);
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return 0.0;
    }
}
