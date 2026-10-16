package org.eqasim.switzerland.ch_cmdp.utils.routing;

import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.core.simulation.policies.routing.ZeroRoutingPenalty;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.eqasim.core.components.routing.speedy.MemoryEfficientRoutingModule;
import org.eqasim.core.components.routing.speedy.MemoryEfficientSpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.eqasim.core.components.travel_time.FloatTravelTimeCalculator;
import org.matsim.vehicles.VehicleUtils;

/**
 * Standalone car-routing memory experiment. Each case runs in a fresh JVM, loads
 * the same input and generates the same 100 OD pairs and departure times.
 *
 * Usage: --network-path network.xml.gz [--config-path config.xml] [--threads 4]
 * [--seed 518745] [--travel-time-tolerance-seconds 1] [--travel-time-tolerance-percent 1].
 * Without a config, Lausanne defaults are used: SpeedyALT,
 * 900 s bins through 30 h, distance utility 0.42, no routing randomness.
 * Runs original SpeedyALT with MATSim's 64-bit travel-time storage and restrictions,
 * optimized SpeedyALT with 16 and 32 landmarks, shared float times and restrictions, then original
 * SpeedyALT with original times without restrictions. JVM heap/GC options are
 * forwarded to all workers.
 *
 * Component sizes are incremental post-GC heap estimates, not deep object sizes.
 * JVM samples include heap and non-heap, but exclude native allocations; Windows working-set and Linux /proc RSS
 * measurements cover resident process memory separately. All-link,
 * all-bin free-speed observations deliberately model a fully populated collector.
 * No input files are modified. This is not a whole-simulation memory benchmark.
 */
public final class CheckNetworkMemoryUsage {
    private static final Logger log = LogManager.getLogger(CheckNetworkMemoryUsage.class);
    private static final double MIB = 1024.0 * 1024.0;
    private static final int TRIPS = 100;
    private static final List<String> CASES = List.of("original-with-restrictions",
            "optimized-with-restrictions", "optimized-32-with-restrictions", "without-restrictions");
    private static final String ROUTE_PREFIX = "BENCHMARK_ROUTE ";
    private static final ObjectMapper JSON = new ObjectMapper();

    private CheckNetworkMemoryUsage() { }

    public static void main(String[] args) throws Exception {
        // This standalone diagnostic must retain its statistics even when the
        // surrounding application/test logging configuration suppresses INFO.
        org.apache.logging.log4j.core.config.Configurator.setLevel(
                CheckNetworkMemoryUsage.class.getName(), org.apache.logging.log4j.Level.INFO);
        var cmd = new CommandLine.Builder(args).requireOptions("network-path")
                .allowOptions("config-path", "threads", "seed", "worker-case", "travel-time-tolerance-seconds",
                        "travel-time-tolerance-percent", MemoryEfficientRoutingModule.OPTION).build();
        TravelTimeTolerance tolerance = travelTimeTolerance(cmd);
        if (cmd.hasOption(MemoryEfficientRoutingModule.OPTION)) {
            MemoryEfficientRoutingModule.isEnabled(cmd); // Validate legacy arguments.
            if (!cmd.hasOption("worker-case")) log.warn(
                    "--memory-efficient-routing is no longer needed in this benchmark: all four cases always run.");
        }
        if (cmd.hasOption("worker-case")) {
            runWorker(cmd);
        } else {
            log.info("Independent JVM comparison: original SpeedyALT/original travel times with restrictions, optimized SpeedyALT with 16/32 landmarks and float travel times with restrictions, original SpeedyALT/original travel times without restrictions. Input is read-only.");
            var cases = new ArrayList<CaseReport>();
            for (String name : CASES) {
                cases.add(runChild(args, name));
            }
            printSummary(cases.get(0), cases.get(1), cases.get(2), cases.get(3));
            printRouteComparison(cases.get(0), cases.get(1), tolerance);
            printRouteComparison(cases.get(0), cases.get(2), tolerance);
            printRouteComparison(cases.get(1), cases.get(2), tolerance);
        }
    }

    private static CaseReport runChild(String[] args, String name) throws Exception {
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        for (String option : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            // Do not forward debugger ports, agents or launcher-specific options.
            if (option.startsWith("-Xmx") || option.startsWith("-Xms") || option.startsWith("-XX:")) {
                command.add(option);
            }
        }
        command.addAll(List.of("-cp", System.getProperty("java.class.path"),
                CheckNetworkMemoryUsage.class.getName()));
        command.addAll(List.of(args));
        command.addAll(List.of("--worker-case", name));
        Process child = new ProcessBuilder(command).redirectErrorStream(true).start();
        Map<String, Double> metrics = new ConcurrentHashMap<>();
        Map<Integer, Route> routes = new ConcurrentHashMap<>();
        var outputFailure = new java.util.concurrent.atomic.AtomicReference<Exception>();
        Thread output = Thread.ofPlatform().start(() -> {
            try (var reader = child.inputReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(ROUTE_PREFIX)) {
                        Route route = JSON.readValue(line.substring(ROUTE_PREFIX.length()), Route.class);
                        if (routes.putIfAbsent(route.index, route) != null) {
                            throw new java.io.IOException("Duplicate route result: " + route.index);
                        }
                        continue; // Keep complete route payloads out of the human-readable log.
                    }
                    readMetrics(line, metrics);
                    System.out.println(line);
                }
            } catch (Exception e) {
                outputFailure.set(e);
                log.warn("Could not read worker output", e);
                child.destroyForcibly();
            }
        });
        Process monitor = null;
        LinuxMemorySampler linux = System.getProperty("os.name").startsWith("Linux")
                ? new LinuxMemorySampler(child.pid()) : null;
        if (System.getProperty("os.name").startsWith("Windows")) {
            // One persistent OS monitor, rather than launching PowerShell per sample.
            String script = "[System.Threading.Thread]::CurrentThread.CurrentCulture=[System.Globalization.CultureInfo]::InvariantCulture; $samples=0; $sum=0.0; $peak=0L; "
                    + "while ($true) { $p=Get-Process -Id " + child.pid() + " -ErrorAction SilentlyContinue; "
                    + "if ($null -eq $p) { break }; try { $sum += $p.WorkingSet64; $samples++; "
                    + "$peak=[Math]::Max($peak,$p.PeakWorkingSet64) } catch {}; Start-Sleep -Milliseconds 100 }; "
                    + "if ($samples -gt 0) { 'OS_WORKING_SET samples={0} average_MiB={1:F3} peak_MiB={2:F3}' "
                    + "-f $samples,($sum/$samples/1048576),($peak/1048576) }";
            monitor = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-WindowStyle", "Hidden", "-Command", script).redirectErrorStream(true).start();
        }
        int status = child.waitFor();
        output.join();
        if (monitor != null) {
            String statistics = new String(monitor.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            monitor.waitFor();
            log.info("{} {}", name, statistics);
            var matches = java.util.regex.Pattern.compile("(average|peak)_MiB=([0-9.]+)").matcher(statistics);
            while (matches.find()) metrics.put("rss_" + matches.group(1), Double.parseDouble(matches.group(2)));
        } else if (linux != null) {
            linux.close();
            metrics.putAll(linux.metrics());
            log.info("{} OS_RSS average_MiB={} peak_MiB={} samples={}", name,
                    metrics.get("rss_average"), metrics.get("rss_peak"), linux.count);
        } else {
            log.info("{} OS resident-memory monitoring is supported on Windows and Linux; unavailable on this OS.", name);
        }
        if (status != 0) throw new IllegalStateException(name + " worker exited with status " + status);
        if (outputFailure.get() != null) throw new IllegalStateException("Invalid worker output", outputFailure.get());
        if (routes.size() != TRIPS || !routes.keySet().containsAll(
                java.util.stream.IntStream.range(0, TRIPS).boxed().toList())) {
            throw new IllegalStateException(name + " did not report all " + TRIPS + " routes");
        }
        return new CaseReport(name, metrics, routes);
    }

    private record CaseReport(String name, Map<String, Double> metrics, Map<Integer, Route> routes) { }

    // Store the actual sequences, not hashes. Long bit patterns preserve exact
    // floating-point results through JSON, including unreachable-path NaNs.
    record Route(int index, String from, String to, long departureBits, boolean reachable,
            List<String> links, List<String> nodes, long timeBits, long costBits) { }

    record TravelTimeTolerance(double seconds, double percent) {
        TravelTimeTolerance {
            if (!Double.isFinite(seconds) || seconds < 0 || !Double.isFinite(percent) || percent < 0)
                throw new IllegalArgumentException("Travel-time tolerances must be finite and nonnegative");
        }
        double allowedDifference(double reference) { return Math.max(seconds, reference * (percent / 100)); }
    }

    static TravelTimeTolerance travelTimeTolerance(CommandLine command) {
        return new TravelTimeTolerance(
                Double.parseDouble(command.getOption("travel-time-tolerance-seconds").orElse("1")),
                Double.parseDouble(command.getOption("travel-time-tolerance-percent").orElse("1")));
    }

    record RouteComparison(boolean trip, boolean reachability, boolean validTimes, boolean time,
            double absoluteDifference, double relativePercent, double allowedDifference) {
        boolean similar() { return trip && reachability && validTimes && time; }
    }

    static RouteComparison compareRoutes(Route original, Route optimized, TravelTimeTolerance tolerance) {
        boolean trip = original.index == optimized.index && original.from.equals(optimized.from)
                && original.to.equals(optimized.to) && original.departureBits == optimized.departureBits;
        boolean reachability = original.reachable == optimized.reachable;
        if (!original.reachable || !optimized.reachable)
            return new RouteComparison(trip, reachability, true, reachability, Double.NaN, Double.NaN, Double.NaN);
        double before = Double.longBitsToDouble(original.timeBits), after = Double.longBitsToDouble(optimized.timeBits);
        boolean valid = Double.isFinite(before) && before >= 0 && Double.isFinite(after) && after >= 0;
        double difference = valid ? Math.abs(after - before) : Double.NaN;
        double relative = valid ? (before > 0 ? difference / before * 100 : after == 0 ? 0 : Double.NaN) : Double.NaN;
        double allowed = valid ? tolerance.allowedDifference(before) : Double.NaN;
        return new RouteComparison(trip, reachability, valid, valid && difference <= allowed, difference, relative, allowed);
    }

    private static void printRouteComparison(CaseReport original, CaseReport optimized, TravelTimeTolerance tolerance) {
        System.out.println("\nCompared cases: " + original.name + " vs " + optimized.name);
        System.out.println(routeComparisonReport(original.routes, optimized.routes, tolerance));
    }

    static String routeComparisonReport(Map<Integer, Route> original, Map<Integer, Route> optimized,
            TravelTimeTolerance tolerance) {
        StringBuilder report = new StringBuilder("\nTRAVEL-TIME COMPARISON: both cases WITH restrictions\n");
        report.append(String.format(Locale.ROOT,
                "Allowed difference: max(%.3f s, %.3f%% of reference travel time).%n", tolerance.seconds, tolerance.percent));
        int similar = 0, reachable = 0, unreachable = 0, reachabilityDifferences = 0, invalid = 0, tripDifferences = 0;
        var differences = new ArrayList<Double>();
        var percentages = new ArrayList<Double>();
        for (int i = 0; i < original.size(); i++) {
            Route before = original.get(i), after = optimized.get(i);
            RouteComparison comparison = compareRoutes(before, after, tolerance);
            if (!comparison.trip) tripDifferences++;
            if (!comparison.reachability) reachabilityDifferences++;
            if (before.reachable && after.reachable) {
                reachable++;
                if (comparison.similar()) similar++;
                if (!comparison.validTimes) invalid++;
                else if (comparison.trip) {
                    differences.add(comparison.absoluteDifference);
                    if (Double.isFinite(comparison.relativePercent)) percentages.add(comparison.relativePercent);
                }
            }
            if (!before.reachable && !after.reachable) unreachable++;
            String status = !comparison.trip ? "TRIP DIFFERENCE" : !comparison.reachability ? "REACHABILITY DIFFERENCE"
                    : !before.reachable ? "BOTH UNREACHABLE" : !comparison.validTimes ? "INVALID TRAVEL TIME"
                    : comparison.time ? "SIMILAR" : "OUTSIDE TOLERANCE";
            report.append(String.format(Locale.ROOT,
                    "Trip %3d: %-23s reference_s=%s compared_s=%s difference_s=%s difference_pct=%s allowed_s=%s%n",
                    i + 1, status, number(Double.longBitsToDouble(before.timeBits)), number(Double.longBitsToDouble(after.timeBits)),
                    number(comparison.absoluteDifference), number(comparison.relativePercent), number(comparison.allowedDifference)));
        }
        report.append("Travel times within tolerance: ").append(similar).append('/').append(reachable)
                .append("; reachable in both: ").append(reachable).append("; unreachable in both: ").append(unreachable).append('\n')
                .append("Reachability differences: ").append(reachabilityDifferences)
                .append("; invalid reachable travel-time pairs: ").append(invalid)
                .append("; trip differences: ").append(tripDifferences).append('\n');
        appendDifferenceStatistics(report, "Absolute travel-time difference (s)", differences);
        appendDifferenceStatistics(report, "Relative travel-time difference (%)", percentages);
        report.append("Path IDs and costs do not determine travel-time similarity.\n")
                .append("Travel-time similarity: ")
                .append(similar != reachable || reachabilityDifferences > 0 || tripDifferences > 0
                        ? "DIFFERENCES FOUND" : reachable == 0 ? "NOT EVALUATED (no reachable trips in both cases)" : "PASS");
        return report.toString();
    }

    private static void appendDifferenceStatistics(StringBuilder report, String label, List<Double> values) {
        if (values.isEmpty()) { report.append(label).append(": N/A\n"); return; }
        values.sort(Double::compare);
        report.append(String.format(Locale.ROOT, "%s: mean=%.6f p95=%.6f max=%.6f%n", label,
                values.stream().mapToDouble(Double::doubleValue).average().orElseThrow(),
                values.get((int) Math.ceil(values.size() * 0.95) - 1), values.get(values.size() - 1)));
    }

    private static void metric(String key, double value) {
        System.out.println("BENCHMARK_METRIC " + key + "=" + value);
    }

    private static void readMetrics(String line, Map<String, Double> metrics) {
        if (!line.startsWith("BENCHMARK_METRIC ")) return;
        String[] pair = line.substring("BENCHMARK_METRIC ".length()).split("=", 2);
        if (pair.length == 2) metrics.put(pair[0], Double.parseDouble(pair[1]));
    }

    private static void printSummary(CaseReport original, CaseReport optimized16, CaseReport optimized32, CaseReport without) {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("landmarks_requested", "Requested landmarks");
        rows.put("landmark_payload", "Compact landmark payload (MiB)");
        rows.put("successful_trips", "Successful trips / " + TRIPS);
        rows.put("network_load_seconds", "Network load (s)");
        rows.put("travel_times_build_seconds", "Travel-time build (s)");
        rows.put("router_build_seconds", "Router build (s)");
        rows.put("routing_seconds", "Routing wall time (s)");
        rows.put("mean_trip_seconds", "Mean trip routing (s)");
        rows.put("network_heap", "Network heap estimate (MiB)");
        rows.put("travel_times_heap", "Travel-time heap estimate (MiB)");
        rows.put("router_heap", "Router heap estimate (MiB)");
        rows.put("retained_heap", "Retained heap after routing (MiB)");
        rows.put("heap_average", "Lifetime average heap (MiB)");
        rows.put("heap_peak", "Lifetime peak heap (MiB)");
        rows.put("jvm_average", "Lifetime average heap+nonheap (MiB)");
        rows.put("jvm_peak", "Lifetime peak heap+nonheap (MiB)");
        rows.put("rss_average", "Process average resident RAM (MiB)");
        rows.put("rss_peak", "Process peak resident RAM (MiB)");
        StringBuilder table = new StringBuilder("\nSUMMARY: four-case routing comparison\n");
        String layout = "%-38s %18s %18s %18s %20s%n";
        table.append(String.format(Locale.ROOT, layout,
                "Metric", "Original + turns", "Optimized 16", "Optimized 32", "Original, no turns"));
        table.append("-".repeat(134)).append('\n');
        rows.forEach((key, label) -> table.append(String.format(Locale.ROOT, layout, label,
                number(original.metrics.get(key)), number(optimized16.metrics.get(key)),
                number(optimized32.metrics.get(key)), number(without.metrics.get(key)))));
        table.append("\nPERCENT CHANGES\n");
        table.append(String.format(Locale.ROOT, layout, "Metric", "Opt16 vs original", "Opt32 vs original",
                "32 vs 16", "No turns vs original"));
        table.append("-".repeat(134)).append('\n');
        rows.forEach((key, label) -> table.append(String.format(Locale.ROOT, layout, label,
                percent(original.metrics.get(key), optimized16.metrics.get(key)),
                percent(original.metrics.get(key), optimized32.metrics.get(key)),
                percent(optimized16.metrics.get(key), optimized32.metrics.get(key)),
                percent(original.metrics.get(key), without.metrics.get(key)))));
        table.append("Turns = turn restrictions. Original cases use 16 landmarks; optimized cases request 16 and 32.\n")
                .append("Counts are capped for tiny graphs; actual counts are logged in Compact routing graphs.\n")
                .append("Compact landmark payload reports shared primitive tables for optimized cases only.\n")
                .append("Travel-time storage: original cases = MATSim 64-bit packed means/counts; optimized = shared 32-bit float means.\n")
                .append("Negative time/memory percentages mean a reduction.\n")
                .append("Component sizes are post-GC estimates; averages/heap peaks are sampled every 100 ms.\n")
                .append("Resident RAM: Windows working set / Linux VmRSS; peaks include OS high-water marks.\n")
                .append("N/A means unavailable; resident memory and heap are overlapping measurements, not additive.");
        System.out.println(table);
    }

    private static String number(Double value) {
        return value == null || !Double.isFinite(value) ? "N/A" : String.format(Locale.ROOT, "%.3f", value);
    }

    private static String percent(Double before, Double after) {
        return before == null || after == null || before <= 0 ? "N/A"
                : String.format(Locale.ROOT, "%+.2f%%", (after - before) / before * 100);
    }

    /** Parse Linux /proc status values (kB in procfs means 1024 bytes). */
    static Map<String, Long> parseLinuxStatus(String status) {
        Map<String, Long> values = new java.util.HashMap<>();
        for (String line : status.split("\\R")) {
            if (line.startsWith("VmRSS:") || line.startsWith("VmHWM:")) {
                String[] fields = line.trim().split("\\s+");
                values.put(fields[0].replace(":", ""), Long.parseLong(fields[1]) * 1024L);
            }
        }
        return values;
    }

    static final class LinuxMemorySampler implements AutoCloseable {
        private final Path status;
        private final java.util.concurrent.ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        private long count, sum, peak;

        LinuxMemorySampler(long pid) {
            this(Path.of("/proc", Long.toString(pid), "status"));
        }

        LinuxMemorySampler(Path status) {
            this.status = status;
            sample();
            timer.scheduleAtFixedRate(this::sample, 100, 100, TimeUnit.MILLISECONDS);
        }

        private synchronized void sample() {
            try {
                var values = parseLinuxStatus(Files.readString(status));
                Long rss = values.get("VmRSS");
                if (rss == null) return; // Process may have exited / become a zombie.
                count++;
                sum += rss;
                peak = Math.max(peak, Math.max(rss, values.getOrDefault("VmHWM", rss)));
            } catch (java.io.IOException ignored) {
                // procfs disappears when the worker exits, or may be access-restricted.
            }
        }

        synchronized Map<String, Double> metrics() {
            if (count == 0) return Map.of();
            return Map.of("rss_average", sum / (double) count / MIB, "rss_peak", peak / MIB);
        }

        @Override public void close() { timer.shutdownNow(); }
    }

    record Trip(Id<Node> from, Id<Node> to, double departure) { }

    private static void runWorker(CommandLine cmd) throws Exception {
        String name = cmd.getOptionStrict("worker-case");
        boolean efficientRouting = name.equals("optimized-with-restrictions") || name.equals("optimized-32-with-restrictions");
        int landmarks = name.equals("optimized-32-with-restrictions") ? 32 : 16;
        log.info("Router implementation: {}", efficientRouting ? "memory-efficient SpeedyALT" : "MATSim SpeedyALT");
        if (!CASES.contains(name)) {
            throw new IllegalArgumentException("Unknown worker case: " + name);
        }
        Config config = cmd.hasOption("config-path") ? ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"))
                : ConfigUtils.createConfig();
        if (!cmd.hasOption("config-path")) {
            config.travelTimeCalculator().setTraveltimeBinSize(900);
            config.travelTimeCalculator().setMaxTime(30 * 3600);
        }
        double distanceUtility = 0.42;
        double noise = 0;
        if (config.getModules().containsKey("eqasim")) {
            var params = config.getModules().get("eqasim").getParams();
            distanceUtility = Double.parseDouble(params.getOrDefault("routingDistanceUtility", "0.42"));
            noise = Double.parseDouble(params.getOrDefault("sigmaRoutingRandomness", "0.0"));
        }
        int threads = Integer.parseInt(cmd.getOption("threads").orElse("4"));
        if (threads < 1) throw new IllegalArgumentException("threads must be positive");
        long seed = Long.parseLong(cmd.getOption("seed").orElse("518745"));
        log.info("CASE {} pid={} threads={} seed={} SpeedyALT distanceUtility={} noise={} bins={}s maxTime={}s",
                name, ProcessHandle.current().pid(), threads, seed, distanceUtility, noise,
                config.travelTimeCalculator().getTraveltimeBinSize(), config.travelTimeCalculator().getMaxTime());
        log.info("Requested landmarks: {}", landmarks);
        metric("landmarks_requested", landmarks);
        log.info("Memory sizes are post-GC incremental heap estimates; GC/class loading can make small deltas noisy or negative.");

        try (var samples = new MemorySampler()) {
            long baseline = retainedHeap();
            long start = System.nanoTime();
            Network full = NetworkUtils.readNetwork(cmd.getOptionStrict("network-path"));
            double loadSeconds = secondsSince(start);
            long loaded = retainedHeap();
            long restrictions = full.getLinks().values().stream()
                    .filter(l -> NetworkUtils.getDisallowedNextLinks(l) != null).count();
            if (name.equals("without-restrictions")) {
                full.getLinks().values().forEach(l -> l.getAttributes().removeAttribute("disallowedNextLinks"));
            }
            long afterRestrictions = retainedHeap();
            log.info("{} full_network nodes={} links={} restricted_links={} load_seconds={} loaded_heap_delta_MiB={} restriction_removal_delta_MiB={}",
                    name, full.getNodes().size(), full.getLinks().size(), restrictions, loadSeconds,
                    mib(loaded - baseline), mib(afterRestrictions - loaded));

            Network car = NetworkUtils.createNetwork();
            new TransportModeNetworkFilter(full).filter(car, Set.of("car"));
            // Keep every car link, as the simulation router does. Disconnected OD
            // pairs are reported as failures, never silently replaced by new trips.
            List<Trip> trips = selectTrips(car, seed);
            long networks = retainedHeap();
            log.info("{} network_heap_MiB={} car_filter_increment_MiB={} car_nodes={} car_links={}",
                    name, mib(networks - baseline), mib(networks - afterRestrictions),
                    car.getNodes().size(), car.getLinks().size());

            metric("network_heap", (networks - baseline) / MIB);
            metric("network_load_seconds", loadSeconds);
            start = System.nanoTime();
            Object collector;
            TravelTime travelTime;
            if (efficientRouting) {
                var floats = buildTravelTimes(full, config);
                collector = floats;
                travelTime = floats.getLinkTravelTimes();
                log.info("{} Shared float travel-time storage: {}", name, floats.getStorageStatistics());
            } else {
                var original = buildOriginalTravelTimes(full, config);
                collector = original;
                travelTime = original.getLinkTravelTimes();
                log.info("{} Original MATSim travel-time storage: 64-bit packed means/counts; shared across routing workers", name);
            }
            double tableSeconds = secondsSince(start);
            long withTimes = retainedHeap();
            log.info("{} travel_times_heap_MiB={} build_seconds={}", name, mib(withTimes - networks), tableSeconds);

            metric("travel_times_heap", (withTimes - networks) / MIB);
            metric("travel_times_build_seconds", tableSeconds);
            var costs = new EqasimTravelDisutilityFactory(new ZeroRoutingPenalty(), distanceUtility, noise, seed)
                    .createTravelDisutility(travelTime);
            LeastCostPathCalculatorFactory factory = efficientRouting
                    ? new MemoryEfficientSpeedyALTFactory(landmarks) : new SpeedyALTFactory();
            var routers = new ArrayList<LeastCostPathCalculator>();
            start = System.nanoTime();
            // Shared graph/landmarks, private search arrays per worker. Construct
            // serially because turn-restriction expansion may mutate graph state.
            for (int i = 0; i < threads; i++) routers.add(factory.createPathCalculator(car, costs, travelTime));
            double routerSeconds = secondsSince(start);
            if (factory instanceof MemoryEfficientSpeedyALTFactory optimized) {
                log.info("Compact routing graphs: {}", optimized.getStatistics());
                metric("landmark_payload", optimized.getStatistics().stream()
                        .mapToLong(MemoryEfficientSpeedyALTFactory.Statistics::landmarkBytes).sum() / MIB);
            }
            long withRouters = retainedHeap();
            log.info("{} router_heap_MiB={} build_seconds={} (shared graph/landmarks + {} search workspaces)",
                    name, mib(withRouters - withTimes), routerSeconds, threads);

            metric("router_heap", (withRouters - withTimes) / MIB);
            metric("router_build_seconds", routerSeconds);
            var person = org.matsim.core.population.PopulationUtils.getFactory().createPerson(Id.createPersonId("benchmark"));
            var type = VehicleUtils.createVehicleType(Id.create("benchmarkCar", org.matsim.vehicles.VehicleType.class));
            type.setNetworkMode("car");
            var vehicle = VehicleUtils.createVehicle(Id.createVehicleId("benchmarkCar"), type);
            var executor = Executors.newFixedThreadPool(threads);
            var futures = new ArrayList<java.util.concurrent.Future<List<Result>>>();
            samples.reset();
            start = System.nanoTime();
            try {
                for (int worker = 0; worker < threads; worker++) {
                    final int index = worker;
                    futures.add(executor.submit(() -> {
                        var results = new ArrayList<Result>();
                        for (int i = index; i < trips.size(); i += threads) {
                            Trip trip = trips.get(i);
                            long begin = System.nanoTime();
                            var path = routers.get(index).calcLeastCostPath(car.getNodes().get(trip.from),
                                    car.getNodes().get(trip.to), trip.departure, person, vehicle);
                            results.add(new Result(i, secondsSince(begin), path));
                        }
                        return results;
                    }));
                }
                var results = new ArrayList<Result>();
                for (var future : futures) results.addAll(future.get());
                double routingSeconds = secondsSince(start);
                samples.sample();
                String routingMemory = samples.summary();
                metric("routing_seconds", routingSeconds);
                metric("mean_trip_seconds", results.stream().mapToDouble(Result::seconds).average().orElse(0));
                metric("successful_trips", results.stream().filter(r -> r.path != null).count());
                results.sort(Comparator.comparingInt(Result::index));
                for (Result result : results) {
                    Trip trip = trips.get(result.index);
                    var path = result.path;
                    log.info("{} trip={} from={} to={} departure_s={} routing_s={} travel_time_s={} cost={} links={}",
                            name, result.index + 1, trip.from, trip.to, trip.departure,
                            result.seconds, path == null ? Double.NaN : path.travelTime,
                            path == null ? Double.NaN : path.travelCost, path == null ? -1 : path.links.size());
                    // Serialize after timing ends so comparison I/O is not counted as routing.
                    Route route = new Route(result.index, trip.from.toString(), trip.to.toString(),
                            Double.doubleToLongBits(trip.departure), path != null,
                            path == null ? List.of() : path.links.stream().map(l -> l.getId().toString()).toList(),
                            path == null ? List.of() : path.nodes.stream().map(n -> n.getId().toString()).toList(),
                            Double.doubleToLongBits(path == null ? Double.NaN : path.travelTime),
                            Double.doubleToLongBits(path == null ? Double.NaN : path.travelCost));
                    System.out.println(ROUTE_PREFIX + JSON.writeValueAsString(route));
                }
                log.info("{} ROUTING wall_seconds={} mean_trip_seconds={} successful={}/{} {}", name,
                        routingSeconds, results.stream().mapToDouble(Result::seconds).average().orElse(0),
                        results.stream().filter(r -> r.path != null).count(), TRIPS, routingMemory);
                for (int i = 0; i < routers.size(); i++) {
                    if (routers.get(i) instanceof org.eqasim.core.components.routing.speedy.SpeedyALT optimized) {
                        log.info("{} worker={} search_statistics={}", name, i, optimized.getSearchStatistics());
                    }
                }
            } finally {
                executor.shutdownNow();
                executor.awaitTermination(1, TimeUnit.MINUTES);
            }
            long afterRouting = retainedHeap();
            log.info("{} retained_after_routing_MiB={} routing_retained_delta_MiB={} baseline_MiB={} total_heap_capacity_MiB={}",
                    name, mib(afterRouting), mib(afterRouting - withRouters), mib(baseline),
                    mib(Runtime.getRuntime().maxMemory()));
            metric("retained_heap", afterRouting / MIB);
            // Keep all measured components alive through the last GC measurement.
            Reference.reachabilityFence(full);
            Reference.reachabilityFence(car);
            Reference.reachabilityFence(collector);
            Reference.reachabilityFence(factory);
            Reference.reachabilityFence(routers);
        }
    }

    private record Result(int index, double seconds, LeastCostPathCalculator.Path path) { }

    static List<Trip> selectTrips(Network network, long seed) {
        var nodes = new ArrayList<>(network.getNodes().values());
        nodes.sort(Comparator.comparing(n -> n.getId().toString()));
        if (nodes.size() < 2) throw new IllegalArgumentException("At least two car-network nodes are required");
        var random = new Random(seed);
        var trips = new ArrayList<Trip>();
        for (int i = 0; i < TRIPS; i++) {
            int from = random.nextInt(nodes.size());
            int to = random.nextInt(nodes.size() - 1);
            if (to >= from) to++;
            trips.add(new Trip(nodes.get(from).getId(), nodes.get(to).getId(),
                    5 * 3600 + random.nextDouble() * 17 * 3600));
        }
        return trips;
    }

    static FloatTravelTimeCalculator buildTravelTimes(Network network, Config config) {
        var settings = config.travelTimeCalculator();
        var observed = new org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup();
        observed.setTraveltimeBinSize(settings.getTraveltimeBinSize());
        observed.setMaxTime(settings.getMaxTime());
        observed.setTravelTimeGetterType(settings.getTravelTimeGetterType());
        observed.setFilterModes(false);
        observed.setCalculateLinkTravelTimes(true);
        observed.setCalculateLinkToLinkTravelTimes(false);
        var collector = new FloatTravelTimeCalculator(network, observed);
        populateTravelTimes(network, config, collector, collector);
        collector.prepareForRouting();
        return collector;
    }

    static TravelTimeCalculator buildOriginalTravelTimes(Network network, Config config) {
        var settings = config.travelTimeCalculator();
        var builder = new TravelTimeCalculator.Builder(network);
        builder.setTimeslice(settings.getTraveltimeBinSize());
        builder.setMaxTime(settings.getMaxTime());
        builder.setFilterModes(false);
        builder.setCalculateLinkTravelTimes(true);
        builder.setCalculateLinkToLinkTravelTimes(false);
        builder.configure(settings);
        var collector = builder.build();
        populateTravelTimes(network, config, collector, collector);
        return collector;
    }

    private static void populateTravelTimes(Network network, Config config,
            LinkEnterEventHandler enters, LinkLeaveEventHandler leaves) {
        var settings = config.travelTimeCalculator();
        var vehicleId = Id.createVehicleId("syntheticFreeflowObservation");
        // Include the overflow bin, and use independent observations in every bin
        // so this really allocates the simulation collector's per-bin storage.
        int lastBin = (int) Math.ceil(settings.getMaxTime() / settings.getTraveltimeBinSize());
        for (var link : network.getLinks().values()) {
            for (int bin = 0; bin <= lastBin; bin++) {
                double departure = (bin + 0.5) * settings.getTraveltimeBinSize();
                double time = link.getLength() / link.getFreespeed(departure);
                if (!Double.isFinite(time) || time < 0) throw new IllegalArgumentException("Invalid free-speed time on " + link.getId());
                enters.handleEvent(new LinkEnterEvent(departure, vehicleId, link.getId()));
                leaves.handleEvent(new LinkLeaveEvent(departure + time, vehicleId, link.getId()));
            }
        }
    }

    private static long retainedHeap() throws InterruptedException {
        System.gc();
        Thread.sleep(200);
        System.gc();
        Thread.sleep(200);
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    private static double secondsSince(long start) { return (System.nanoTime() - start) / 1e9; }
    private static String mib(long bytes) { return String.format(Locale.ROOT, "%.3f", bytes / MIB); }

    private static final class MemorySampler implements AutoCloseable {
        private final java.util.concurrent.ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        private long count, heapSum, jvmSum, peakHeap, peakJvm;
        private long allCount, allHeapSum, allJvmSum, allPeakHeap, allPeakJvm;

        MemorySampler() { timer.scheduleAtFixedRate(this::sample, 0, 100, TimeUnit.MILLISECONDS); }

        synchronized void sample() {
            var bean = ManagementFactory.getMemoryMXBean();
            long heap = bean.getHeapMemoryUsage().getUsed();
            long jvm = heap + bean.getNonHeapMemoryUsage().getUsed();
            count++; heapSum += heap; jvmSum += jvm;
            peakHeap = Math.max(peakHeap, heap); peakJvm = Math.max(peakJvm, jvm);
            allCount++; allHeapSum += heap; allJvmSum += jvm;
            allPeakHeap = Math.max(allPeakHeap, heap); allPeakJvm = Math.max(allPeakJvm, jvm);
        }

        synchronized void reset() { count = heapSum = jvmSum = peakHeap = peakJvm = 0; sample(); }

        synchronized String summary() {
            return "samples=" + count + " heap_average_MiB=" + mib(heapSum / Math.max(1, count))
                    + " heap_peak_MiB=" + mib(peakHeap) + " heap_plus_nonheap_average_MiB="
                    + mib(jvmSum / Math.max(1, count)) + " heap_plus_nonheap_peak_MiB=" + mib(peakJvm);
        }

        @Override public void close() {
            timer.shutdownNow();
            synchronized (this) {
                metric("heap_average", allHeapSum / (double) Math.max(1, allCount) / MIB);
                metric("heap_peak", allPeakHeap / MIB);
                metric("jvm_average", allJvmSum / (double) Math.max(1, allCount) / MIB);
                metric("jvm_peak", allPeakJvm / MIB);
                log.info("WORKER_LIFETIME samples={} heap_average_MiB={} heap_peak_MiB={} heap_plus_nonheap_average_MiB={} heap_plus_nonheap_peak_MiB={}",
                        allCount, mib(allHeapSum / Math.max(1, allCount)), mib(allPeakHeap),
                        mib(allJvmSum / Math.max(1, allCount)), mib(allPeakJvm));
            }
        }
    }
}
