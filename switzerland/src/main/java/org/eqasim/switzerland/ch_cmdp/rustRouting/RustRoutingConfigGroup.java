// Configures the optional native, scheduled public-transport routing backend.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import org.matsim.core.config.ReflectiveConfigGroup;

public final class RustRoutingConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "swissRustRouting";

    @Parameter
    @Comment(
            "Use the Rust scheduled PT router instead of SwissRailRaptor; road routing is"
                    + " unchanged.")
    public boolean enabled = false;

    @Parameter
    @Comment(
            "Optional native library path, relative to this config. Empty uses bundled/system"
                + " libraries or automatic installation.")
    public String libraryPath = "";

    @Parameter
    @Comment(
            "Automatically download/cache a compiled router, or build its pinned source if no"
                + " library is supplied.")
    public boolean autoInstall = true;

    @Parameter
    @Comment(
            "Router release/tag used for automatic installation; cached installations are"
                + " versioned.")
    public String version = "v0.2.0";

    @Parameter
    @Comment(
            "Installation root relative to this config; empty uses ~/.eqasim/rust-router."
                + " Version/platform subdirectories are added.")
    public String installationDirectory = "";

    @Parameter
    @Comment(
            "Allow cloning and building the pinned router when no compiled download is available."
                + " Requires Git, Cargo and a native linker.")
    public boolean allowSourceBuild = true;

    @Parameter
    @Comment(
            "Optional existing Rust router checkout relative to this config. Builds it as-is"
                + " instead of downloading or cloning; does not modify Git state.")
    public String sourceDirectory = "";

    @Parameter
    @Comment("Maximum vehicle boardings per itinerary, between 1 and 64.")
    public int maxBoardings = 64;

    @Parameter
    @Comment("Maximum journey duration in seconds, including access/egress walking.")
    public double maxDurationSeconds = 172800;

    @Parameter
    @Comment("Use service-aware nested partition refinement for regional lower bounds.")
    public boolean networkPartition = true;

    @Parameter
    @Comment("Experimental cost-aware transfer suffix pruning; uses additional shared memory.")
    public boolean transferBlocks = false;

    @Parameter
    @Comment("Maximum stops settled while preparing query-local lower bounds; 0 is unlimited. Does not limit the route search.")
    public int localBoundSettlements = 1024;

    @Parameter
    @Comment("Heuristic connection/transfer work budget per coordinate candidate search; 0 is exact. A feasible best-so-far route may be returned and marked rustRoutingBudgetLimited.")
    public int coordinateWorkBudget = 2000000;
    @Parameter
    @Comment("Expand found journeys with a pre-departure gap above this many seconds; 0 disables this trigger.")
    public double qualityWaitSeconds = 1800;
    @Parameter
    @Comment("Expand found journeys longer than this many seconds; 0 disables this trigger.")
    public double qualityJourneySeconds = 10800;
    @Parameter
    @Comment("Maximum radius in metres for quality-triggered expansion; existing no-path retries remain supported.")
    public double qualityRadiusMeters = 2000;

    public RustRoutingConfigGroup() {
        super(GROUP_NAME);
    }
}
