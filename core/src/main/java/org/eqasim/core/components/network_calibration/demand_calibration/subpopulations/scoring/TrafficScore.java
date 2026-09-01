package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete score definition for subpopulation calibration.
 *
 * <p>This is the one place to change how counted links are classified, how a
 * route score is aggregated, or how two scores are compared. The tracker only
 * supplies raw station inputs and owns the mutable flow state.</p>
 */
public record TrafficScore(
        int countingStations,
        int passages,
        int underestimatedStations,
        int overestimatedStations,
        int acceptableStations,
        int signedImbalance,
        int score,
        double totalExcessError,
        List<Station> stations
) {
    public TrafficScore {
        stations = List.copyOf(stations);
    }

    public boolean isBalanced() {
        return underestimatedStations == 0 && overestimatedStations == 0;
    }

    public boolean isWorthRelocating() {
        return !isBalanced() || countingStations==0;
    }

    /** Computes every derived score value from raw link-level inputs. */
    public static TrafficScore compute(List<StationInput> inputs,
                                       double underThreshold,
                                       double overThreshold) {
        List<Station> stations = new ArrayList<>(inputs.size());
        int passages = 0;
        int under = 0;
        int over = 0;
        int acceptable = 0;
        double excessError = 0.0;

        for (StationInput input : inputs) {
            Station station = station(input, underThreshold, overThreshold);
            stations.add(station);
            passages += input.passages();
            switch (station.status()) {
                case UNDER -> {
                    under++;
                    excessError += (-station.relativeError()) -underThreshold;
                }
                case OVER -> {
                    over++;
                    excessError += station.relativeError() - overThreshold;
                }
                case ACCEPTABLE -> acceptable++;
            }
        }

        int imbalance = over - under;
        return new TrafficScore(stations.size(), passages, under, over, acceptable,
                imbalance, -(over + under), excessError, stations);
    }

    /** Computes one link's status and relative error from raw flow values. */
    public static Station station(StationInput input,
                                  double underThreshold,
                                  double overThreshold) {
        double relativeError = input.observedCount() <= 0.0 ? 0.0
                : (input.simulatedFlow() - input.observedCount()) / input.observedCount();
        return new Station(input.linkId(), input.passages(), input.simulatedFlow(),
                input.observedCount(), relativeError,
                status(relativeError, underThreshold, overThreshold));
    }

    /** Computes the score for a logical station represented by several links. */
    public static StationGroup stationGroup(List<Id<Link>> linkIds,
                                            double simulatedFlow,
                                            double observedCount,
                                            double underThreshold,
                                            double overThreshold) {
        double relativeError = observedCount <= 0.0 ? 0.0
                : (simulatedFlow - observedCount) / observedCount;
        return new StationGroup(linkIds, simulatedFlow, observedCount, relativeError,
                status(relativeError, underThreshold, overThreshold));
    }

    /**
     * Preserves the current acceptance rule: at least one station must improve,
     * the aggregate absolute relative error must fall, and an outside-tolerance
     * error may not flip directly from over to under or vice versa. An individual
     * station may become worse when the aggregate error still improves.
     */
    public boolean isBetterThan(TrafficScore other) {
        final double epsilon = 1.0e-6;
        if (stations.size() != other.stations.size()) return false;

        double candidateAbsoluteError = 0.0;
        double currentAbsoluteError = 0.0;
        boolean strictlyBetter = false;
        Map<Id<Link>, Station> otherByLink = null;

        for (int index = 0; index < stations.size(); index++) {
            Station candidateStation = stations.get(index);
            Station currentStation = other.stations.get(index);
            if (!candidateStation.linkId().equals(currentStation.linkId())) {
                if (otherByLink == null) {
                    otherByLink = new HashMap<>();
                    for (Station station : other.stations) {
                        otherByLink.put(station.linkId(), station);
                    }
                }
                currentStation = otherByLink.get(candidateStation.linkId());
            }
            if (currentStation == null || oppositeErrors(currentStation.status(), candidateStation.status())) {
                return false;
            }

            double candidateError = Math.abs(candidateStation.relativeError());
            double currentError = Math.abs(currentStation.relativeError());

            if (candidateError + epsilon < currentError) {
                strictlyBetter = true;
            }
            candidateAbsoluteError += candidateError;
            currentAbsoluteError += currentError;
        }
        return strictlyBetter && candidateAbsoluteError + epsilon < currentAbsoluteError;
    }

    private static Status status(double relativeError,
                                 double underThreshold,
                                 double overThreshold) {
        return relativeError < -underThreshold ? Status.UNDER
                : relativeError > overThreshold ? Status.OVER
                : Status.ACCEPTABLE;
    }

    private static boolean oppositeErrors(Status current, Status candidate) {
        return (current == Status.OVER && candidate == Status.UNDER)
                || (current == Status.UNDER && candidate == Status.OVER);
    }

    /** Raw values supplied by the mutable traffic state. */
    public record StationInput(
            Id<Link> linkId,
            int passages,
            double simulatedFlow,
            double observedCount
    ) { }

    /** Fully derived score for one counted link. */
    public record Station(
            Id<Link> linkId,
            int passages,
            double simulatedFlow,
            double observedCount,
            double relativeError,
            Status status
    ) { }

    /** Aggregated score for a logical station represented by several links. */
    public record StationGroup(
            List<Id<Link>> linkIds,
            double simulatedFlow,
            double observedCount,
            double relativeError,
            Status status
    ) {
        public StationGroup {
            linkIds = List.copyOf(linkIds);
        }
    }

    public record Comparison(TrafficScore current, TrafficScore candidate) {
        public boolean improves() {
            return candidate.isBetterThan(current);
        }
    }

    public enum Status {
        UNDER,
        ACCEPTABLE,
        OVER
    }
}
