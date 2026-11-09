package ch.sbb.matsim.routing.pt.raptor;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

/** Bounded, request-local retries for the standard walking stop search. */
final class RaptorSearchExpansion {
    static final int MAX_ATTEMPTS = 5;
    private RaptorSearchExpansion() { }

    static <T> T search(Facility from, Facility to, RaptorParameters parameters,
            BiFunction<RaptorParameters, RaptorStopFinder.Direction, List<InitialStop>> stops,
            BiFunction<List<InitialStop>, List<InitialStop>, T> query, Predicate<T> hasTransit) {
        RaptorParameters searchParameters = parameters;
        double radius = parameters.getSearchRadius();
        for (int attempt = 1; ; attempt++) {
            var access = stops.apply(searchParameters, RaptorStopFinder.Direction.ACCESS);
            var egress = stops.apply(searchParameters, RaptorStopFinder.Direction.EGRESS);
            T result = query.apply(access, egress);
            if (hasTransit.test(result) || attempt == MAX_ATTEMPTS
                    || parameters.getConfig().isUseIntermodalAccessEgress()) return result;
            // Account for the stop finder's existing nearest-stop extension before growing again.
            double explored = Math.max(radius, Math.max(furthest(from, access), furthest(to, egress)));
            double next = Math.max(explored * 2, explored + Math.max(1, parameters.getExtensionRadius()));
            if (!Double.isFinite(next) || next <= radius) return result;
            radius = next;
            searchParameters = new ExpandedParameters(parameters, radius);
        }
    }

    private static double furthest(Facility facility, List<InitialStop> stops) {
        double distance = 0;
        for (InitialStop stop : stops)
            distance = Math.max(distance, CoordUtils.calcEuclideanDistance(facility.getCoord(), stop.stop.getCoord()));
        return distance;
    }

    /** A view instead of changing parameters that may be shared by people, requests or router threads. */
    private static final class ExpandedParameters extends RaptorParameters {
        private final RaptorParameters delegate;
        private final double radius;
        ExpandedParameters(RaptorParameters delegate, double radius) {
            super(delegate.getConfig()); this.delegate = delegate; this.radius = radius;
        }
        @Override public double getSearchRadius() { return radius; }
        @Override public SwissRailRaptorConfigGroup getConfig() { return delegate.getConfig(); }
        @Override public double getExtensionRadius() { return delegate.getExtensionRadius(); }
        @Override public double getDirectWalkFactor() { return delegate.getDirectWalkFactor(); }
        @Override public double getBeelineWalkSpeed() { return delegate.getBeelineWalkSpeed(); }
        @Override public double getMarginalUtilityOfTravelTime_utl_s(String mode) { return delegate.getMarginalUtilityOfTravelTime_utl_s(mode); }
        @Override public double getMarginalUtilityOfWaitingPt_utl_s() { return delegate.getMarginalUtilityOfWaitingPt_utl_s(); }
        @Override public double getTransferPenaltyFixCostPerTransfer() { return delegate.getTransferPenaltyFixCostPerTransfer(); }
        @Override public double getTransferPenaltyPerTravelTimeHour() { return delegate.getTransferPenaltyPerTravelTimeHour(); }
        @Override public double getTransferPenaltyMinimum() { return delegate.getTransferPenaltyMinimum(); }
        @Override public double getTransferPenaltyMaximum() { return delegate.getTransferPenaltyMaximum(); }
        @Override public boolean isUseTransportModeUtilities() { return delegate.isUseTransportModeUtilities(); }
    }
}
