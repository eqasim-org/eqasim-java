package org.matsim.contribs.discrete_mode_choice.model.utilities;

public class RandomValueGenerator {
	
	private static long globalSeed = 1234L;
	
	public static void setGlobalSeed(long seed) {
        globalSeed = seed;
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

    /**
     * Deterministic “random” in [0,1) for a given person and tour/trip id.
     * @param personId unique person identifier
     * @param tourId   unique tour identifier (per person)
     */
    public static double randomForTour(long personId, long tourId) {
        // pack into one 64-bit key
        long key = (personId << 32) ^ tourId ^ globalSeed;
        // scramble it
        long mixed = splitMix64(key);
        // take the top 53 bits to form a double mantissa
        long fractionBits = mixed >>> 11; // keep top 53 of 64
        return fractionBits * (1.0 / (1L << 53));
    }

}
