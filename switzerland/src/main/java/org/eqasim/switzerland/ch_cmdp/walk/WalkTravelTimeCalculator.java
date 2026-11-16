package org.eqasim.switzerland.ch_cmdp.walk;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/** Shared geometry/calculation service. The default incurs no nearest-node lookups or allocations. */
@Singleton
public final class WalkTravelTimeCalculator {
    private final WalkTravelTime expression;
    private final Network network;
    private final boolean teleport;

    @Inject public WalkTravelTimeCalculator(Scenario scenario, WalkTravelTime expression) {
        this.expression = expression;
        network = scenario.getNetwork();
        teleport = expression instanceof TeleportationWalkTravelTime;
        // Build the lazy spatial index before parallel routing starts; afterwards it is read-only.
        if (!isDefault() && !network.getNodes().isEmpty())
            NetworkUtils.getNearestNode(network, network.getNodes().values().iterator().next().getCoord());
    }

    public boolean isDefault() { return teleport; }

    public double calculate(Coord from, Coord to, Person person, double departureTime, double baseline) {
        if (teleport) return baseline;

        // compute nearest nodes
        Node a = network.getNodes().isEmpty() ? null : NetworkUtils.getNearestNode(network, from);
        Node b = network.getNodes().isEmpty() ? null : NetworkUtils.getNearestNode(network, to);

        // compute distance and slope
        double nodesDistance = a == null ? 0 : CoordUtils.calcProjectedEuclideanDistance(a.getCoord(), b.getCoord());
        boolean elevations = nodesDistance > 0 && a.getCoord().hasZ() && b.getCoord().hasZ();
        double nodesElevationChange = 0.0;
        if (elevations) {
            double Za = a.getCoord().getZ();
            double Zb = b.getCoord().getZ();
            // Preprocessing uses zero (or effectively zero) for nodes without fetched elevation.
            elevations = Double.isFinite(Za) && Double.isFinite(Zb) && Math.abs(Za) > 1e-3 && Math.abs(Zb) > 1e-3;
            nodesElevationChange = elevations ? Zb - Za : 0.0;
        }

        return expression.calculate(new WalkTravelTimeContext(from, to, a, b,
                CoordUtils.calcProjectedEuclideanDistance(from, to), nodesDistance, nodesElevationChange, elevations,
                person, departureTime, baseline));
    }
}
