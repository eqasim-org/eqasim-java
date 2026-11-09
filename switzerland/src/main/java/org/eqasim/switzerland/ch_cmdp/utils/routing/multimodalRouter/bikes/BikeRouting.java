package org.eqasim.switzerland.ch_cmdp.utils.routing.multimodalRouter.bikes;
import org.eqasim.core.components.traffic.bike.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;
import org.matsim.core.router.util.*;
/** Precomputes immutable gradient-aware bike speeds and road-type penalties once. */
public final class BikeRouting {
    public static TravelTime travelTime(Network network) {
        var speed = new BikeGradientBasedLinkSpeedCalculator((link, t, vehicleId) -> 0);
        float[] times = new float[network.getLinks().keySet().stream().mapToInt(id -> id.index()).max().orElseThrow() + 1];
        for (Link link : network.getLinks().values()) {
            times[link.getId().index()] = (float) (link.getLength() / speed.getMaximumBikeVelocity(null, link, 0));
        }
        return (link, t, person, vehicle) -> times[link.getId().index()];
    }

    public static TravelDisutility disutility(Network network, TravelTime time) {
        // The network is fixed: cache all gradient/road-type calculations, shared by workers.
        float[] costs = new float[network.getLinks().keySet().stream().mapToInt(id -> id.index()).max().orElseThrow() + 1];
        for (Link link : network.getLinks().values()) {
            double tt = time.getLinkTravelTime(link, 0, null, null);
            costs[link.getId().index()] = (float) (tt + BikeDisutilities.linkTypeDisutility(link, tt)
                    + BikeDisutilities.gradientDisutility(link, tt) + BikeDisutilities.distanceDisutility(link, tt));
        }
        return new TravelDisutility() {
            @Override
            public double getLinkTravelDisutility(Link link, double t, Person person, Vehicle vehicle) {
                return costs[link.getId().index()];
            }

            @Override
            public double getLinkMinimumTravelDisutility(Link link) {
                return costs[link.getId().index()];
            }
        };
    }

}
