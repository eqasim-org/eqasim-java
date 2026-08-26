package org.eqasim.core.components.network_calibration.Processors;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestFlowProcessorRobustError {
    private static final double H = 0.10;
    private static final double EPSILON = 1.0;

    @Test
    public void testSparseGroupScoreIsShrunkByEffectiveSampleSize() {
        FloatArrayList flows = new FloatArrayList(new float[] { 200.0f, 200.0f, 200.0f, 200.0f });
        FloatArrayList counts = new FloatArrayList(new float[] { 100.0f, 100.0f, 100.0f, 100.0f });
        FloatArrayList weights = new FloatArrayList(new float[] { 1.0f, 1.0f, 1.0f, 1.0f });

        FlowProcessor.RobustGroupError error = FlowProcessor.computeRobustGroupError(
                flows, counts, weights, H, EPSILON, 8.0);

        assertEquals(4.0, error.effectiveSampleSize(), 1.0e-9);
        assertEquals(error.rawScore() / 3.0, error.score(), 1.0e-9);
        assertTrue(error.rawScore() > 0.99);
    }

    @Test
    public void testLargeOutlierHasBoundedInfluence() {
        FloatArrayList counts = new FloatArrayList(new float[] { 100.0f });
        FloatArrayList weights = new FloatArrayList(new float[] { 1.0f });

        FlowProcessor.RobustGroupError moderate = FlowProcessor.computeRobustGroupError(
                new FloatArrayList(new float[] { 200.0f }), counts, weights, H, EPSILON, 0.0);
        FlowProcessor.RobustGroupError extreme = FlowProcessor.computeRobustGroupError(
                new FloatArrayList(new float[] { 1_000_000.0f }), counts, weights, H, EPSILON, 0.0);

        assertTrue(moderate.rawScore() > 0.99);
        assertTrue(extreme.rawScore() <= 1.0);
        assertEquals(moderate.rawScore(), extreme.rawScore(), 1.0e-5);
    }

    @Test
    public void testZeroSimulatedFlowProducesFiniteUnderestimation() {
        FlowProcessor.RobustGroupError error = FlowProcessor.computeRobustGroupError(
                new FloatArrayList(new float[] { 0.0f }),
                new FloatArrayList(new float[] { 100.0f }),
                new FloatArrayList(new float[] { 1.0f }),
                H, EPSILON, 0.0);

        assertTrue(Double.isFinite(error.score()));
        assertTrue(error.score() < -0.99);
    }
}
