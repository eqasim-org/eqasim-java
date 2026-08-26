package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestTrafficScore {
    private final Id<Link> first = Id.createLinkId("first");
    private final Id<Link> second = Id.createLinkId("second");

    @Test
    public void computesEveryDerivedValueFromRawStationInputs() {
        TrafficScore score = TrafficScore.compute(List.of(
                new TrafficScore.StationInput(first, 2, 121.0, 100.0),
                new TrafficScore.StationInput(second, 1, 79.0, 100.0)
        ), 0.20, 0.20);

        assertEquals(2, score.countingStations());
        assertEquals(3, score.passages());
        assertEquals(1, score.underestimatedStations());
        assertEquals(1, score.overestimatedStations());
        assertEquals(0, score.acceptableStations());
        assertEquals(-2, score.score());
        assertEquals(0.02, score.totalExcessError(), 1.0e-12);
    }

    @Test
    public void comparisonKeepsTheExistingAggregateErrorRule() {
        TrafficScore current = TrafficScore.compute(List.of(
                new TrafficScore.StationInput(first, 1, 120.0, 100.0),
                new TrafficScore.StationInput(second, 1, 120.0, 100.0)
        ), 0.05, 0.05);
        TrafficScore candidate = TrafficScore.compute(List.of(
                new TrafficScore.StationInput(first, 1, 105.0, 100.0),
                new TrafficScore.StationInput(second, 1, 130.0, 100.0)
        ), 0.05, 0.05);

        assertTrue(candidate.isBetterThan(current));
    }
}
