package org.eqasim.core.tools.random;

public class Normal {
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;

    public static double get(String str1, String str2, long baseSeed) {
        // Continue the same UTF-16 hash across both strings instead of allocating
        // their concatenation. String.valueOf preserves the old null semantics.
        return getWithPrefix(hashPrefix(String.valueOf(str1)), String.valueOf(str2), baseSeed);
    }

    public static double get(String str, long baseSeed) {
        return fromHash(hashPrefix(str), baseSeed);
    }

    /** Reusable hash of an immutable leading string, e.g. a network link ID. */
    public static long hashPrefix(String prefix) {
        return appendHash(FNV_OFFSET, prefix);
    }

    /** Exactly matches get(prefix + suffix, seed) for non-null strings and hashPrefix(prefix). */
    public static double getWithPrefix(long prefixHash, String suffix, long baseSeed) {
        return fromHash(appendHash(prefixHash, suffix), baseSeed);
    }

    private static double fromHash(long hash, long baseSeed) {
        long key = hash ^ baseSeed;
        long h1 = splitMix64(key);
        long h2 = splitMix64(h1);

        // Keep the original floating-point operations and order for exact draws.
        // +1 keeps u1 in (0,1], avoiding log(0).
        double u1 = (toFraction(h1) + 1.0) / (1L << 53);
        double u2 = toFraction(h2) * (1.0 / (1L << 53));
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    /** 64-bit SplitMix64 mixer. */
    private static long splitMix64(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    // Top 53 bits match the mantissa width of a double.
    private static long toFraction(long mixed) {
        return mixed >>> 11;
    }

    // FNV-1a over UTF-16 code units, matching the original concatenated string.
    private static long appendHash(long hash, String value) {
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
