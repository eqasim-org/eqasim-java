package org.eqasim.core.tools.random;

public class Normal {

    public static double get(String str1, String str2, long baseSeed) {
        return Normal.get(str1+str2, baseSeed);
    }

    public static double get(String str, long baseSeed) {
        long key = fnv1a64(str) ^ baseSeed;
        long h1 = splitMix64(key);
        long h2 = splitMix64(h1);

        // +1 keeps u1 in (0,1], avoiding log(0)
        double u1 = (toFraction(h1) + 1.0) / (1L << 53);
        double u2 = toFraction(h2) * (1.0 / (1L << 53));

        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    /**
     * 64-bit SplitMix64 mixer; very fast, good avalanche.
     */
    private static long splitMix64(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    // top 53 of 64 mixed bits, matching a double's mantissa width
    private static long toFraction(long mixed) {
        return mixed >>> 11;
    }

    /**
     * 64-bit FNV-1a hash of the string. Used instead of String.hashCode()
     * (only 32 bits) to give SplitMix64 a well-distributed 64-bit key.
     */
    private static long fnv1a64(String s) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            hash ^= s.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
