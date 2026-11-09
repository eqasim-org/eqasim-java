// Loads the platform JNI library and defines the small primitive-array routing protocol.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import org.matsim.core.config.Config;

final class NativePtRouter {
    private static boolean loaded;
    static boolean hasCoordinates;
    static boolean hasCoordinatePolicy;

    private NativePtRouter() {}

    static synchronized void load(Config config, RustRoutingConfigGroup settings) {
        if (loaded) return;
        try {
            System.load(RustRouterInstallation.resolve(config, settings).toString());
            if (protocolVersion() != 1)
                throw new IllegalStateException(
                        "Unsupported Rust router protocol; rebuild the jar and native library"
                                + " together");
            try { hasCoordinates = coordinateProtocolVersion() == 1; }
            catch (UnsatisfiedLinkError olderRelease) { hasCoordinates = false; }
            try { hasCoordinatePolicy = coordinatePolicyVersion() == 1; }
            catch (UnsatisfiedLinkError olderRelease) { hasCoordinatePolicy = false; }
            loaded = true;
        } catch (Exception | UnsatisfiedLinkError e) {
            throw new IllegalStateException(
                    "Cannot initialize Rust PT routing for "
                            + System.getProperty("os.name")
                            + "/"
                            + System.getProperty("os.arch")
                            + ". Jar includes: "
                            + RustRouterInstallation.bundledPlatforms()
                            + ". Rebuild standalone with this platform included, or set"
                            + " swissRustRouting.libraryPath to its compiled library. A bundled"
                            + " matching library needs no Git/network on the compute node.",
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

    static native int coordinateProtocolVersion();

    // Header: native attempt count, followed by the existing endpoint/leg payload.
    static native double[] routeCoordinates(long handle, long departure, int boardings,
            long duration, double originX, double originY, double destinationX, double destinationY);

    static native int coordinatePolicyVersion();
    // Header: attempts, selected-search-truncated flag, then the original route payload.
    static native double[] routeCoordinatesDetailed(long handle, long departure, int boardings,
            long duration, double originX, double originY, double destinationX, double destinationY);

    static native void close(long handle);

    static long millis(double seconds) {
        double value = Math.round(seconds * 1000);
        if (!Double.isFinite(seconds) || seconds < 0 || value >= 0xffff_ffffL)
            throw new IllegalArgumentException(
                    "Rust routing time is outside the unsigned millisecond range: " + seconds);
        return (long) value;
    }
}
