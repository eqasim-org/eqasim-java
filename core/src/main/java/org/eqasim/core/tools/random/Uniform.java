package org.eqasim.core.tools.random;

/** Stateless uniform draws keyed by a stable ID and two integer coordinates. */
public final class Uniform {
    private Uniform() { }

    /**
     * Returns a draw in [0,1), independent of call/worker order. Route acceptance
     * uses iteration and trip ordinal as coordinates; no person state is retained.
     */
    public static double get(String id, long seed, int iteration, int tripIndex) {
        // Separate this stream from the normal route-cost noise. Pack integer
        // coordinates into disjoint halves before mixing all 64 seed/hash bits.
        long key = mix(Normal.hashPrefix(id) ^ seed ^ 0x632be59bd9b4e019L);
        key = mix(key ^ ((long) iteration << 32) ^ Integer.toUnsignedLong(tripIndex));
        return (key >>> 11) * 0x1.0p-53;
    }

    private static long mix(long value) {
        value += 0x9e3779b97f4a7c15L;
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
