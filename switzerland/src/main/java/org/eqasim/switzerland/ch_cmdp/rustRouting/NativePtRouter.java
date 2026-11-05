// Loads the platform JNI library and defines the small primitive-array routing protocol.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import org.matsim.core.config.Config;

final class NativePtRouter {
    private static boolean loaded;

    private NativePtRouter() {}

    static synchronized void load(Config config, RustRoutingConfigGroup settings) {
        if (loaded) return;
        try {
            System.load(RustRouterInstallation.resolve(config, settings).toString());
            if (protocolVersion() != 1)
                throw new IllegalStateException(
                        "Unsupported Rust router protocol; rebuild the jar and native library"
                            + " together");
            loaded = true;
        } catch (Exception | UnsatisfiedLinkError e) {
            throw new IllegalStateException(
                    "Cannot initialize Rust PT routing. Supply swissRustRouting.libraryPath, bundle"
                        + " a native library, or enable autoInstall. Private GitHub downloads need"
                        + " GITHUB_TOKEN/GH_TOKEN; source builds need Git/Cargo and a linker.",
                    e);
        }
    }

    static native long open(String snapshot, String costs);

    static native int protocolVersion();

    /**
     * Empty output means no transit path; header has cost/access/egress, followed by six numbers
     * per leg.
     */
    static native double[] route(
            long handle,
            long departureMillis,
            int maxBoardings,
            long maxDurationMillis,
            int[] accessStopDurationPairs,
            int[] egressStopDurationPairs);

    static native void close(long handle);

    static long millis(double seconds) {
        double value = Math.round(seconds * 1000);
        if (!Double.isFinite(seconds) || seconds < 0 || value >= 0xffff_ffffL)
            throw new IllegalArgumentException(
                    "Rust routing time is outside the unsigned millisecond range: " + seconds);
        return (long) value;
    }
}
