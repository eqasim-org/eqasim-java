package org.eqasim.switzerland.ch_cmdp.utils.routing.multimodalRouter.walks;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
/** Stateless configured beeline policy, also suitable for explicitly teleported bikes. */
public final class WalkRouting {
    private WalkRouting() {}
    public static double distance(Coord from, Coord to, double distanceFactor) {
        return distanceFactor * CoordUtils.calcEuclideanDistance(from, to);
    }
}
