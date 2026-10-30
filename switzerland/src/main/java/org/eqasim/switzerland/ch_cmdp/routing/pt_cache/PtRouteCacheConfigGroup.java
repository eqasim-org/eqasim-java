package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import org.matsim.core.config.ReflectiveConfigGroup;

/** Run-local PT itinerary cache; disabled unless explicitly requested. */
public final class PtRouteCacheConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "swissPtRouteCache";

    @Parameter public boolean enabled = false;
    @Parameter public int memoryMb = 512;
    /** Maximum departure-time difference for reusing a stored itinerary; zero requires exact times. */
    @Parameter public double departureTimeToleranceSeconds = 10.0;
    /** Optional local directory, relative to the configuration file. */
    @Parameter public String directory = "";

    public PtRouteCacheConfigGroup() { super(GROUP_NAME); }
}
