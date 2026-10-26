package org.eqasim.core.components.traffic.bike;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/** Immutable, shared snapshot of bike link geometry and fixed cost coefficients.
 * Three floats per link; normal networks use direct ID indexing. Sparse ID spaces
 * use a primitive index instead of allocating arrays up to the largest global ID.
 * Construct after scenario/network preparation, before routing starts.
 */
public final class BikeLinkData {
    private static final int STRIDE = 3;
    private final float[] values;
    private final Int2IntOpenHashMap positions;

    public BikeLinkData(Network network) {
        int count = network.getLinks().size();
        int range = network.getLinks().keySet().stream().mapToInt(id -> id.index()).max().orElse(-1) + 1;
        boolean sparse = (long) range > 2L * count;
        positions = sparse ? new Int2IntOpenHashMap(count) : null;
        if (positions != null) positions.defaultReturnValue(-1);
        values = new float[Math.multiplyExact(sparse ? count : range, STRIDE)];
        // Zero speed marks absent links, allowing compatibility with unprepared/test networks.
        int slot = 0;
        for (Link link : network.getLinks().values()) {
            int index = positions == null ? link.getId().index() : slot++;
            if (positions != null) positions.put(link.getId().index(), index);
            int offset = STRIDE * index;
            values[offset] = (float) BikeGradientBasedLinkSpeedCalculator.slopeSpeed(link);
            values[offset + 1] = (float) (1.0 + BikeDisutilities.linkTypeDisutility(link, 1.0)
                    + BikeDisutilities.gradientDisutility(link, 1.0));
            values[offset + 2] = (float) BikeDisutilities.distanceDisutility(link, 0.0);
        }
    }

    private int offset(Link link) {
        int index = link.getId().index();
        if (positions != null) index = positions.get(index);
        return index >= 0 && index < values.length / STRIDE ? index * STRIDE : -1;
    }

    public double slopeSpeed(Link link) {
        int offset = offset(link);
        return offset >= 0 && values[offset] != 0 ? values[offset]
                : BikeGradientBasedLinkSpeedCalculator.slopeSpeed(link);
    }

    public double disutility(Link link, double travelTime) {
        int offset = offset(link);
        if (offset >= 0 && values[offset] != 0) {
            return values[offset + 1] * travelTime + values[offset + 2];
        }
        return travelTime + BikeDisutilities.linkTypeDisutility(link, travelTime)
                + BikeDisutilities.gradientDisutility(link, travelTime)
                + BikeDisutilities.distanceDisutility(link, travelTime);
    }
}
