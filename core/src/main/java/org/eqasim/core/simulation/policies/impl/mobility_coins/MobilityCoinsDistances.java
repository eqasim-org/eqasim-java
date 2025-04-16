package org.eqasim.core.simulation.policies.impl.mobility_coins;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

public record MobilityCoinsDistances(
        double car_km,
        double carPassenger_km,
        double transit_km,
        double bicycle_km,
        double walk_km) {
    static public MobilityCoinsDistances calculate(List<? extends PlanElement> elements) {
        double car_km = 0.0;
        double carPassenger_km = 0.0;
        double transit_km = 0.0;
        double bicycle_km = 0.0;
        double walk_km = 0.0;

        for (PlanElement element : elements) {
            if (element instanceof Leg leg) {
                double distance_km = leg.getRoute().getDistance() * 1e-3;

                switch (leg.getMode()) {
                    case "car":
                        car_km += distance_km;
                        break;
                    case "car_passenger":
                        carPassenger_km += distance_km;
                        break;
                    case "pt":
                        transit_km += distance_km;
                        break;
                    case "bike":
                    case "bicycle":
                        bicycle_km += distance_km;
                        break;
                    case "walk":
                        walk_km += distance_km;
                        break;
                    default:
                        throw new IllegalStateException("Unknown mode: " + leg.getMode());
                }
            }

        }

        return new MobilityCoinsDistances(car_km, carPassenger_km, transit_km, bicycle_km, walk_km);
    }
}
