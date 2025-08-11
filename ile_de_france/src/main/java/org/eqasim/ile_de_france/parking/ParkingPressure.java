package org.eqasim.ile_de_france.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import com.google.inject.Singleton;

@Singleton
public class ParkingPressure {
    static public final String LINK_ATTRIBUTE = "parkingPressure";

    private final IdMap<Link, Double> values = new IdMap<>(Link.class);

    public ParkingPressure(Network network) {
        for (Link link : network.getLinks().values()) {
            Double value = (Double) link.getAttributes().getAttribute(LINK_ATTRIBUTE);

            if (value != null) {
                values.put(link.getId(), value);
            }
        }
    }

    public double getParkingPressure(Id<Link> link) {
        return values.getOrDefault(link, 0.0);
    }
}
