// Retains compatibility with released native libraries that predate coordinate routing.
package org.eqasim.switzerland.ch_cmdp.rustRouting;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;
import java.util.*;
final class LegacyRustEndpoints {
    private LegacyRustEndpoints() {}
    static double[] route(RustTimetableSnapshot snapshot, RustRoutingRuntime runtime, long handle, RoutingRequest request) {
        double radius = snapshot.parameters.getSearchRadius();
        double[] journey = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            if (attempt > 0) runtime.recordExpansion();
            var access = candidates(snapshot, request.getFromFacility(), radius, true);
            var egress = candidates(snapshot, request.getToFacility(), radius, false);
            journey =
                    runtime.routeNative(
                            handle,
                            NativePtRouter.millis(request.getDepartureTime()),
                            runtime.settings.maxBoardings,
                            NativePtRouter.millis(runtime.settings.maxDurationSeconds),
                            access,
                            egress);
            if (journey.length > 0) break;
            double explored = radius;
            for (int i = 0; i < access.length; i += 2)
                explored =
                        Math.max(
                                explored,
                                RustTimetableSnapshot.distance(
                                        request.getFromFacility().getCoord(),
                                        snapshot.stops[access[i]].getCoord()));
            for (int i = 0; i < egress.length; i += 2)
                explored =
                        Math.max(
                                explored,
                                RustTimetableSnapshot.distance(
                                        request.getToFacility().getCoord(),
                                        snapshot.stops[egress[i]].getCoord()));
            radius =
                    Math.max(
                            explored * 2,
                            explored + Math.max(1, snapshot.parameters.getExtensionRadius()));
        }
        if (journey == null || journey.length == 0) return new double[0];
        double directTime =
                RustTimetableSnapshot.distance(
                                request.getFromFacility().getCoord(),
                                request.getToFacility().getCoord())
                        / snapshot.parameters.getBeelineWalkSpeed();
        double directCost =
                directTime * -snapshot.parameters.getMarginalUtilityOfTravelTime_utl_s("walk");
        if (directCost * snapshot.parameters.getDirectWalkFactor() < journey[0]) return new double[0];
        return journey;
    }
    private static int[] candidates(
            RustTimetableSnapshot snapshot, Facility facility, double radius, boolean access) {
        var coord = facility.getCoord();
        Collection<Integer> stops = snapshot.spatial.getDisk(coord.getX(), coord.getY(), radius);
        if (stops.size() < 2) {
            int closest = snapshot.spatial.getClosest(coord.getX(), coord.getY());
            double nearest =
                    RustTimetableSnapshot.distance(coord, snapshot.stops[closest].getCoord());
            stops =
                    snapshot.spatial.getDisk(
                            coord.getX(),
                            coord.getY(),
                            nearest + snapshot.parameters.getExtensionRadius());
        }
        // Stable integer order makes ties reproducible regardless of QuadTree traversal order.
        int[] ids = new int[stops.size()];
        int position = 0;
        for (int id : stops) ids[position++] = id;
        Arrays.sort(ids);
        int[] pairs = new int[ids.length * 2];
        for (int i = 0; i < ids.length; i++) {
            var stop = snapshot.stops[ids[i]];
            double extra =
                    access ? snapshot.stopAccessSeconds[ids[i]] : snapshot.stopEgressSeconds[ids[i]];
            pairs[2 * i] = ids[i];
            pairs[2 * i + 1] =
                    Math.toIntExact(
                            NativePtRouter.millis(
                                    Math.ceil(
                                                    RustTimetableSnapshot.distance(
                                                                    coord, stop.getCoord())
                                                            / snapshot.parameters
                                                                    .getBeelineWalkSpeed())
                                            + extra));
        }
        return pairs;
    }

}
