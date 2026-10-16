package org.eqasim.core.components.routing.speedy;

import java.util.Arrays;

/** Exact double landmark rows. Blocks allow in-place compaction without allocating
 * a second full-size landmark table. Published only after preprocessing finishes.
 */
final class LandmarkTable {
    private static final int ROW_SHIFT = 10;
    private static final int ROWS_PER_BLOCK = 1 << ROW_SHIFT;
    private static final int ROW_MASK = ROWS_PER_BLOCK - 1;
    private final int rowCount;
    private final int width;
    private double[][] blocks;
    private int[] rowIndex;
    private int storedRows;

    LandmarkTable(int rowCount, int width) {
        this.rowCount = rowCount;
        this.width = width;
        storedRows = rowCount;
        blocks = new double[(rowCount + ROW_MASK) >>> ROW_SHIFT][];
        for (int block = 0; block < blocks.length; block++) {
            blocks[block] = new double[Math.min(ROWS_PER_BLOCK, rowCount - block * ROWS_PER_BLOCK) * width];
        }
    }

    void set(int row, int column, double value) {
        blocks[row >>> ROW_SHIFT][(row & ROW_MASK) * width + column] = value;
    }

    double get(int node, int column) {
        int row = row(node);
        return blocks[row >>> ROW_SHIFT][(row & ROW_MASK) * width + column];
    }

    int row(int node) { return rowIndex == null ? node : rowIndex[node]; }
    double[] block(int row) { return blocks[row >>> ROW_SHIFT]; }
    int offset(int row) { return (row & ROW_MASK) * width; }

    void compact() {
        if (rowCount < 256 || width == 0 || rowIndex != null) return;
        int capacity = 1;
        while (capacity < 2L * rowCount) {
            if (capacity > Integer.MAX_VALUE / 2) return;
            capacity <<= 1;
        }
        // Primitive open addressing avoids per-node map entries/boxed keys.
        int[] table = new int[capacity];
        int[] mapping = new int[rowCount];
        int unique = 0;
        for (int row = 0; row < rowCount; row++) {
            int bucket = hash(row) & (capacity - 1);
            while (table[bucket] != 0 && !sameRow(row, table[bucket] - 1)) {
                bucket = (bucket + 1) & (capacity - 1);
            }
            if (table[bucket] == 0) {
                table[bucket] = row + 1;
                mapping[row] = unique++;
            } else {
                mapping[row] = mapping[table[bucket] - 1];
            }
        }
        // Indirection is worthwhile only with at least 10% payload savings.
        long compactBytes = 8L * unique * width + 4L * rowCount;
        if (compactBytes > uncompressedBytes() * 0.9) return;
        int destination = 0;
        for (int row = 0; row < rowCount; row++) {
            if (mapping[row] != destination) continue;
            System.arraycopy(blocks[row >>> ROW_SHIFT], (row & ROW_MASK) * width,
                    blocks[destination >>> ROW_SHIFT], (destination & ROW_MASK) * width, width);
            destination++;
        }
        blocks = Arrays.copyOf(blocks, (unique + ROW_MASK) >>> ROW_SHIFT);
        int last = blocks.length - 1;
        blocks[last] = Arrays.copyOf(blocks[last], (((unique - 1) & ROW_MASK) + 1) * width);
        rowIndex = mapping;
        storedRows = unique;
    }

    private int hash(int row) {
        long hash = 0xcbf29ce484222325L;
        double[] values = blocks[row >>> ROW_SHIFT];
        int start = (row & ROW_MASK) * width;
        for (int column = 0; column < width; column++) {
            hash ^= Double.doubleToRawLongBits(values[start + column]);
            hash *= 0x100000001b3L;
        }
        hash ^= hash >>> 32;
        return (int) hash;
    }

    private boolean sameRow(int a, int b) {
        double[] left = blocks[a >>> ROW_SHIFT], right = blocks[b >>> ROW_SHIFT];
        int from = (a & ROW_MASK) * width, to = (b & ROW_MASK) * width;
        for (int column = 0; column < width; column++) {
            if (Double.doubleToRawLongBits(left[from + column])
                    != Double.doubleToRawLongBits(right[to + column])) return false;
        }
        return true;
    }

    int storedRows() { return storedRows; }
    long uncompressedBytes() { return 8L * rowCount * width; }
    long bytes() { return 8L * storedRows * width + (rowIndex == null ? 0 : 4L * rowCount); }
}
