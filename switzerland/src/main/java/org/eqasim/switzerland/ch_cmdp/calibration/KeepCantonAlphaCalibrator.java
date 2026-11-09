package org.eqasim.switzerland.ch_cmdp.calibration;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.fast_calibration.AlphaCalibrationUtils;
import org.eqasim.core.components.fast_calibration.AlphaCalibratorConfig;
import org.eqasim.core.components.fast_calibration.FastCalibration;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/** Feedback calibration of canton ASCs against a snapshot of input-population shares. */
@Singleton
public final class KeepCantonAlphaCalibrator implements FastCalibration, IterationEndsListener {
    private static final Logger logger = LogManager.getLogger(KeepCantonAlphaCalibrator.class);
    private static final String PREFIX = "keep_canton:";
    private static final double SHARE_FLOOR = 0.0001;
    private final Scenario scenario;
    private final OutputDirectoryHierarchy output;
    private final SwissCmdpModeParameters parameters;
    private final TripListConverter converter;
    private final EqasimConfigGroup eqasimConfig;
    private final Set<String> cantons;
    private final List<String> calibratedModes;
    private final double beta;
    private final boolean active;
    private final Map<String, Map<String, Double>> initialShares;
    private final Map<String, long[]> initialCounts;
    private boolean finalParametersSaved;

    @Inject
    public KeepCantonAlphaCalibrator(Scenario scenario, OutputDirectoryHierarchy output,
            SwissCmdpModeParameters parameters, TripListConverter converter,
            AlphaCalibratorConfig config, EqasimConfigGroup eqasimConfig) {
        this.scenario = scenario;
        this.output = output;
        this.parameters = parameters;
        this.converter = converter;
        this.eqasimConfig = eqasimConfig;
        this.active = config.isActivate();
        this.cantons = parseCantons(config.getLevel());
        this.calibratedModes = config.getCalibratedModes().stream().distinct().toList();
        this.beta = config.getBeta();
        if (!Double.isFinite(beta) || beta < 0 || beta > 1) {
            throw new IllegalArgumentException("keep_canton beta must be between 0 and 1");
        }
        for (String mode : calibratedModes) {
            if (!CantonCalibrationTrips.MODES.contains(mode)) {
                throw new IllegalArgumentException("Unsupported keep_canton calibrated mode: " + mode);
            }
        }
        if (active) {
            var counts = countTrips();
            initialCounts = counts;
            var targets = new LinkedHashMap<String, Map<String, Double>>();
            for (String canton : cantons) {
                long total = total(counts.get(canton));
                if (total == 0) {
                    throw new IllegalArgumentException("No eligible trips for keep_canton canton '" + canton
                            + "'. Use the population's cantonName values (for example geneva;vaud).");
                }
                targets.put(canton, Collections.unmodifiableMap(shares(counts.get(canton))));
                logger.info("Keeping initial mode shares for {} ({} eligible trips): {}", canton, total, targets.get(canton));
            }
            initialShares = Collections.unmodifiableMap(targets);
            writeInitialShares(counts);
        } else {
            initialShares = Map.of();
            initialCounts = Map.of();
        }
    }

    public static Set<String> parseCantons(String level) {
        String value = level == null ? "" : level.trim();
        if (!value.toLowerCase(Locale.ROOT).startsWith(PREFIX)) {
            throw new IllegalArgumentException("Expected keep_canton:<cantonName>;<cantonName>");
        }
        var names = new LinkedHashSet<String>();
        for (String name : value.substring(PREFIX.length()).split(";")) {
            if (!name.isBlank()) names.add(name.trim());
        }
        if (names.isEmpty()) throw new IllegalArgumentException("keep_canton requires at least one cantonName");
        return Collections.unmodifiableSet(names);
    }

    public Map<String, Map<String, Double>> getInitialShares() {
        return initialShares;
    }

    /** Both the input snapshot and later measurements use this full-population census. */
    private Map<String, long[]> countTrips() {
        var counts = new LinkedHashMap<String, long[]>();
        cantons.forEach(canton -> counts.put(canton, new long[CantonCalibrationTrips.MODES.size()]));
        for (var person : scenario.getPopulation().getPersons().values()) {
            var cantonCounts = counts.get(person.getAttributes().getAttribute("cantonName"));
            if (cantonCounts == null || !AlphaCalibrationUtils.isConsideredPerson(person)) continue;
            var plan = person.getSelectedPlan();
            if (plan == null) continue;
            for (var trip : converter.convert(plan)) {
                if (CantonCalibrationTrips.isConsidered(trip)) {
                    cantonCounts[CantonCalibrationTrips.MODES.indexOf(trip.getInitialMode())]++;
                }
            }
        }
        return counts;
    }

    private static long total(long[] counts) {
        long total = 0;
        for (long count : counts) total += count;
        return total;
    }

    private static Map<String, Double> shares(long[] counts) {
        long total = total(counts);
        var shares = new LinkedHashMap<String, Double>();
        for (int i = 0; i < counts.length; i++) {
            shares.put(CantonCalibrationTrips.MODES.get(i), total == 0 ? 0.0 : (double) counts[i] / total);
        }
        return shares;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (!active) return;
        var counts = event.getIteration() == scenario.getConfig().controller().getFirstIteration()
                ? initialCounts : countTrips();
        for (String canton : cantons) {
            if (total(counts.get(canton)) == 0) {
                logger.warn("Skipping keep_canton update for {}: no eligible trips in this iteration", canton);
                continue;
            }
            if (event.getIteration() > scenario.getConfig().controller().getFirstIteration()) {
                updateConstants(canton, shares(counts.get(canton)));
            }
        }
        writeIteration(event.getIteration(), counts);
    }

    private void updateConstants(String canton, Map<String, Double> current) {
        var target = initialShares.get(canton);
        double referenceCorrection = Math.log(Math.max(target.get("pt"), SHARE_FLOOR))
                - Math.log(Math.max(current.get("pt"), SHARE_FLOOR));
        for (String mode : calibratedModes) {
            if (mode.equals("pt")) continue; // Keep the existing PT reference constant, including nonzero values.
            var constants = constants(mode);
            double correction = Math.log(Math.max(target.get(mode), SHARE_FLOOR))
                    - Math.log(Math.max(current.get(mode), SHARE_FLOOR)) - referenceCorrection;
            constants.put(canton, constants.getOrDefault(canton, 0.0) + (1 - beta) * correction);
        }
    }

    private Map<String, Double> constants(String mode) {
        return switch (mode) {
            case "car" -> parameters.swissCanton.car;
            case "pt" -> parameters.swissCanton.pt;
            case "walk" -> parameters.swissCanton.walk;
            case "bike" -> parameters.swissCanton.bike;
            case "car_passenger" -> parameters.swissCanton.cp;
            default -> throw new IllegalArgumentException("Unsupported mode: " + mode);
        };
    }

    private void writeInitialShares(Map<String, long[]> counts) {
        String path = output.getOutputFilename("keep_canton_initial_shares.csv");
        try (var writer = new PrintWriter(path)) {
            writer.println("canton,eligible_trips," + String.join(",", CantonCalibrationTrips.MODES));
            for (String canton : cantons) {
                writer.print(canton + "," + total(counts.get(canton)));
                for (String mode : CantonCalibrationTrips.MODES) writer.print("," + initialShares.get(canton).get(mode));
                writer.println();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write initial keep_canton shares: " + path, e);
        }
    }

    private void writeIteration(int iteration, Map<String, long[]> counts) {
        String path = output.getIterationFilename(iteration, "keep_canton_shares.csv");
        try (var writer = new PrintWriter(path)) {
            writer.println("canton,mode,target_share,current_share,eligible_trips,constant");
            for (String canton : cantons) {
                var current = shares(counts.get(canton));
                for (String mode : CantonCalibrationTrips.MODES) {
                    writer.println(canton + "," + mode + "," + initialShares.get(canton).get(mode) + ","
                            + current.get(mode) + "," + total(counts.get(canton)) + ","
                            + constants(mode).getOrDefault(canton, 0.0));
                }
            }
            parameters.saveToYamlFile(output.getIterationFilename(iteration, "mode_parameters.yml"));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write keep_canton diagnostics", e);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (active && event.isLastIteration()) saveFinalParameters();
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        if (active && !finalParametersSaved) saveFinalParameters();
    }

    private void saveFinalParameters() {
        String path = output.getOutputFilenameWithOutputPrefix("mode_parameters.yml");
        try {
            parameters.saveToYamlFile(path);
            // The output config sits alongside this file; keep it portable for restarts.
            eqasimConfig.setModeParametersPath(Path.of(path).getFileName().toString());
            finalParametersSaved = true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save final keep_canton parameters", e);
        }
    }
}
