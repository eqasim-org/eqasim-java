package org.eqasim.core.tools.random;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestNormal {
    @Test
    public void preservesOriginalDrawsForAllStringBoundariesAndSeeds() {
        String[] values = {"", "12345", "person-17", "é漢字", "\ud83d", "\ude00", "😀", null};
        long[] seeds = {0, 1997, -1, Long.MIN_VALUE, Long.MAX_VALUE};
        for (String first : values) for (String second : values) for (long seed : seeds) {
            same(original(first + second, seed), Normal.get(first, second, seed));
            same(original(first + second, seed), Normal.getWithPrefix(
                    Normal.hashPrefix(String.valueOf(first)), String.valueOf(second), seed));
        }
        Random random = new Random(41221);
        for (int i = 0; i < 20_000; i++) {
            String value = randomString(random);
            int split = random.nextInt(value.length() + 1);
            long seed = random.nextLong();
            same(original(value, seed), Normal.get(value, seed));
            same(original(value, seed), Normal.get(value.substring(0, split), value.substring(split), seed));
            same(original(value, seed), Normal.getWithPrefix(Normal.hashPrefix(value.substring(0, split)),
                    value.substring(split), seed));
        }
    }

    @Test(expected = NullPointerException.class)
    public void singleStringNullStillThrows() {
        Normal.get(null, 1997);
    }

    private static String randomString(Random random) {
        char[] chars = new char[random.nextInt(60)];
        for (int i = 0; i < chars.length; i++) chars[i] = (char) random.nextInt(65536);
        return new String(chars);
    }

    private static void same(double expected, double actual) {
        assertEquals(Double.doubleToRawLongBits(expected), Double.doubleToRawLongBits(actual));
    }

    // Frozen pre-optimization algorithm: preserve draws, rather than merely
    // checking that the new implementation returns a normal distribution.
    private static double original(String value, long seed) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x100000001b3L;
        }
        long h1 = mix(hash ^ seed);
        long h2 = mix(h1);
        double u1 = ((h1 >>> 11) + 1.0) / (1L << 53);
        double u2 = (h2 >>> 11) * (1.0 / (1L << 53));
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    private static long mix(long value) {
        value += 0x9E3779B97F4A7C15L;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
