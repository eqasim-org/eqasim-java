package org.eqasim.switzerland.ch_cmdp.utils.routing.multimodalRouter;

import org.eqasim.switzerland.ch_cmdp.utils.routing.multimodalRouter.server.MultiModeRouterServer;

/**
 * Long-lived multi-mode CSV routing service. Loads data eagerly in the background; poll /ready before /route.
 * CSV paths in HTTP requests refer to the server filesystem. Arguments use --name value.
 * <ul>
 * <li>network-path (required): network XML/XML.GZ; config-path: MATSim config, required for PT/walk/bike.</li>
 * <li>port (required), host (127.0.0.1): HTTP binding.</li>
 * <li>threads: routing pool size, inherits global.numberOfThreads; batch-size (128): task size;
 * chunk-size (65536): maximum rows retained per chunk.</li>
 * <li>events-path: recorded road times; ignored with MARouter. Otherwise calibrated freeflow is used.</li>
 * <li>start-time/end-time/interval: event time range/bin in seconds (0/86400/900), or explicit VDF overrides.</li>
 * <li>departure-time: override all CSV departures; omitted uses each row's departure_time.</li>
 * <li>routingDistanceUtility: distance disutility, inherits eqasim.routingDistanceUtility, then 0.</li>
 * <li>tolls-value-of-time/tolls-sigma: toll conversion and heterogeneity; inherit networkCalibration,
 * then 15 currency/hour and 0.</li>
 * <li>return-links (false): legacy hyphen-separated road links; export-routes (false): reusable itinerary JSON.</li>
 * <li>compute-cost (false): load fare models and return CHF costs; car-cost-model: simple or weiss,
 * inherits simulation model selection, then simple.</li>
 * <li>marouter (false): weighted static road assignment instead of events/freeflow.</li>
 * <li>marouter-iterations (1): total passes; every pass is incremental; later passes start
 * against previous times and average replacement flows using 1/iteration.</li>
 * <li>marouter-increment (0.10): trip fraction per increment in every pass; marouter-seed: shuffle seed,
 * inherits global.randomSeed, then 4711.</li>
 * <li>marouter-truck-pcu (2): truck capacity consumption; truck-max-speed (80/3.6): speed ceiling in m/s.</li>
 * <li>marouter-sample-size: capacity fraction, inherits eqasim.sampleSize, then 1. CSV weight independently
 * multiplies vehicle PCU; all road iterations preserve both factors.</li>
 * <li>use-rust-pt-router (true): native backend from RunSimulation; false selects RAPTOR.
 * Native installation/search settings come from swissRustRouting in the config.</li>
 * <li>pt-headway-interval (0): seconds before/after departure for optional RAPTOR headway estimation;
 * zero writes NaN. PT components are always returned independently of itinerary export.</li>
 * <li>log-file: optional main log destination; omitted keeps configured logging.</li>
 * <li>initialization-log-file: optional separate initialization log; omitted writes initialization to
 * the main log. Includes network/schedule loading and native preprocessing.</li>
 * </ul>
 * Router-specific config defaults are in multiModeRouter; explicit CLI values take precedence.
 */
public final class RunServer {
    private RunServer() {}
    public static void main(String[] args) throws Exception { MultiModeRouterServer.main(args); }
}
