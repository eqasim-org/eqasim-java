package org.eqasim.core.components.routing.speedy;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestCompactRoutingStorage {
    @Test public void unusedRoutersDoNotAllocateANetworkSizedIndex() {
        var state = new SearchWorkspace(2_000_000);
        long emptyBytes = state.bytes();
        assertTrue(emptyBytes < 2048);
        assertFalse(state.contains(1_999_999));
        state.beginQuery();
        assertEquals(emptyBytes + 8_000_000L, state.bytes());
        state.set(1_999_999, 3.5, 7.25);
        assertEquals(3.5, state.cost(1_999_999), 0);
        state.beginQuery();
        assertFalse(state.contains(1_999_999));
        assertEquals(emptyBytes + 8_000_000L, state.bytes());
    }

    @Test public void sparseRecordsGrowResetAndReuseWithoutFullNodeArrays() {
        var state = new SearchWorkspace(100000);
        assertFalse(state.beginQuery());
        for (int i = 0; i < 3000; i++) {
            int node = i * 337 % 100000;
            assertFalse(state.contains(node));
            state.set(node, i + 0.125, i + 0.25);
            state.incoming(node, i);
            assertEquals(i, state.slot(node));
        }
        assertEquals(3000, state.visited());
        assertTrue(state.bytes() < 600000); // dense search state alone would exceed 2 MiB
        for (int i = 0; i < 3000; i++) {
            int node = i * 337 % 100000;
            assertTrue(state.contains(node));
            assertEquals(i + 0.125, state.cost(node), 0);
            assertEquals(i + 0.25, state.time(node), 0);
            assertEquals(i, state.incoming(node));
        }
        assertFalse(state.beginQuery());
        for (int i = 0; i < 3000; i++) assertFalse(state.contains(i * 337 % 100000));
        assertEquals(0, state.visited());
        state.set(99999, 1, 2);
        assertEquals(0, state.slot(99999));
        state.set(99999, 3, 4);
        assertEquals(1, state.visited());
        assertEquals(3, state.cost(99999), 0);
    }

    @Test public void broadSearchSwitchesToDenseAndGenerationWrapDoesNotReuseOldRecords() throws Exception {
        var state = new SearchWorkspace(200);
        assertFalse(state.beginQuery());
        for (int i = 0; i < 190; i++) state.set(i, i, i);
        assertTrue(state.beginQuery());
        assertTrue(state.dense());
        assertEquals(0, state.visited());
        for (int i = 0; i < 200; i++) assertFalse(state.contains(i));
        state.set(199, Math.nextUp(1.0), 123.25);
        state.incoming(199, 77);
        assertEquals(199, state.slot(199));
        assertEquals(Math.nextUp(1.0), state.cost(199), 0);
        assertEquals(77, state.incoming(199));
        var generation = SearchWorkspace.class.getDeclaredField("generation");
        generation.setAccessible(true);
        generation.setInt(state, -1);
        assertFalse(state.beginQuery());
        assertFalse(state.contains(199));
        state.set(199, 2, 3);
        assertEquals(1, state.visited());
    }

    @Test public void packedHeapPositionsPreserveNodeTieOrderAcrossGrowthAndRemapping() {
        int count = 5003;
        int[] slots = new int[count];
        var dense = new DAryMinHeap(count, 6);
        var packed = new DAryMinHeap(count, 6, node -> slots[node], true);
        for (int round = 0; round < 3; round++) {
            dense.clear(); packed.clear();
            for (int node = 0; node < count; node++) {
                slots[node] = (node * 7919 + round) % count;
                dense.insert(node, 100 + node % 7);
                packed.insert(node, 100 + node % 7);
            }
            for (int node = 0; node < count; node += 7) {
                dense.decreaseKey(node, 50); packed.decreaseKey(node, 50);
            }
            while (!dense.isEmpty()) assertEquals(dense.poll(), packed.poll());
            assertTrue(packed.isEmpty());
        }
        packed.insert(100, 2);
        assertEquals(100, packed.poll());
        packed.decreaseKey(100, 1); // A popped singleton must be reinserted.
        assertEquals(100, packed.poll());
    }

    @Test public void landmarkRowsCompactAcrossBlocksWithoutChangingAnyDoubleBits() {
        int rows = 3500, width = 32, unique = 600;
        var table = new LandmarkTable(rows, width);
        for (int row = 0; row < rows; row++) for (int column = 0; column < width; column++) {
            table.set(row, column, value(row % unique, column));
        }
        table.compact();
        assertEquals(unique, table.storedRows());
        assertEquals(8L * unique * width + 4L * rows, table.bytes());
        assertTrue(table.bytes() < table.uncompressedBytes() / 4);
        for (int row = 0; row < rows; row++) for (int column = 0; column < width; column++) {
            assertEquals(Double.doubleToRawLongBits(value(row % unique, column)),
                    Double.doubleToRawLongBits(table.get(row, column)));
        }
        table.compact();
        assertEquals(unique, table.storedRows());
    }

    @Test public void uniqueLandmarkRowsStayDirectAndEmptyTablesAreSupported() {
        var table = new LandmarkTable(2100, 32);
        for (int row = 0; row < 2100; row++) for (int column = 0; column < 32; column++) {
            table.set(row, column, value(row, column));
        }
        table.compact();
        assertEquals(2100, table.storedRows());
        assertEquals(table.uncompressedBytes(), table.bytes());
        for (int row = 0; row < 2100; row++) assertEquals(value(row, 3), table.get(row, 3), 0);
        var empty = new LandmarkTable(0, 0);
        empty.compact();
        assertEquals(0, empty.bytes());
    }

    private static double value(int row, int column) {
        return switch (column) {
            case 0 -> row % 2 == 0 ? -0.0 : +0.0;
            case 1 -> Double.POSITIVE_INFINITY;
            case 2 -> Double.longBitsToDouble(0x7ff8000000000000L + row);
            default -> row * 32.0 + column + 0.125;
        };
    }
}
