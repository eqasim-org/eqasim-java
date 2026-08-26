package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.RouteImpact;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficCategory;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore.StationGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore.Status;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScoringTracker;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Physical-station cross-border volume expansion and reduction, executed in parallel. */
public final class CrossBorderVolumeCalibrator {
    private static final Logger logger = LogManager.getLogger(CrossBorderVolumeCalibrator.class);
    private static final int MAX_CONSECUTIVE_CLONE_MISSES = 20;

    private final Population population;
    private final CrossBorderState state;
    private final CrossBorderPopulationEditor populationEditor;
    private final CrossBorderCloneFactory cloneFactory;
    private final CrossBorderStationDetector stationDetector;
    private final double updateFraction;
    private final int parallelism;
    private final Map<Id<Person>, Object> personLocks = new ConcurrentHashMap<>();

    public CrossBorderVolumeCalibrator(Population population,
                                 CrossBorderState state,
                                 CrossBorderPopulationEditor populationEditor,
                                 CrossBorderCloneFactory cloneFactory,
                                 CrossBorderStationDetector stationDetector,
                                 double updateFraction,
                                 int parallelism) {
        this.population = population;
        this.state = state;
        this.populationEditor = populationEditor;
        this.cloneFactory = cloneFactory;
        this.stationDetector = stationDetector;
        this.updateFraction = updateFraction;
        this.parallelism = Math.max(1, parallelism);
    }

    public Result update(TrafficScoringTracker tracker) {
        List<CrossBorderStation> stations = stationDetector.update(tracker);
        List<CrossBorderStation> targets = stations.stream()
                .filter(station -> {
                    StationGroup score = tracker.stationGroupScore(station.links());
                    return score != null && score.status() != Status.ACCEPTABLE;
                })
                .toList();
        if (targets.isEmpty()) return new Result(0, 0, 0);

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(parallelism, targets.size()));
        try {
            List<Future<StationResult>> futures = new ArrayList<>(targets.size());
            for (CrossBorderStation station : targets) {
                futures.add(executor.submit(() -> updateStation(station, tracker)));
            }
            int cloned = 0;
            int removed = 0;
            int restored = 0;
            for (Future<StationResult> future : futures) {
                StationResult result = future.get();
                cloned += result.cloned();
                removed += result.removed();
                restored += result.restored();
            }
            logger.info("Cross-border volume calibration: stations={}, cloned={}, removed={}, restored={}",
                    targets.size(), cloned, removed, restored);
            return new Result(cloned, removed, restored);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cross-border calibration interrupted", exception);
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new IllegalStateException("Cross-border station calibration failed", exception.getCause());
        } finally {
            executor.shutdown();
        }
    }

    private StationResult updateStation(CrossBorderStation station,
                                        TrafficScoringTracker tracker) {
        StationGroup score = tracker.stationGroupScore(station.links());
        if (score == null) return new StationResult(station, 0, 0, 0);
        return score.status() == Status.UNDER
                ? expand(station, tracker)
                : reduce(station, tracker);
    }

    private StationResult expand(CrossBorderStation station,
                                 TrafficScoringTracker tracker) {
        double updateLimit = tracker.requiredAdditionFlow(station.links()) * updateFraction;
        double applied = 0.0;
        int restored = 0;
        for (Id<Person> personId : state.removedAt(station)) {
            if (applied + 1.0e-9 >= updateLimit
                    || !hasStatus(station, tracker, Status.UNDER)) break;
            Object lock = personLocks.computeIfAbsent(personId, ignored -> new Object());
            synchronized (lock) {
                if (!state.isRemoved(personId)) continue;
                RouteImpact restoredImpact = tracker.extract(state.removedPlan(personId));
                int passages = station.passages(restoredImpact);
                if (passages == 0 || !tracker.canAddAt(station.links(), restoredImpact)) continue;
                if (populationEditor.restoreTravel(personId)) {
                    Person person = person(personId);
                    if (person != null) {
                        tracker.update(person);
                        applied += tracker.flowContribution(restoredImpact, station.links());
                    }
                    restored++;
                }
            }
        }

        List<Id<Person>> donorIds = station.links().stream()
                .flatMap(link -> tracker.personsAt(link, TrafficCategory.CROSS_BORDER).stream())
                .distinct()
                .filter(personId -> !state.isRemoved(personId))
                .toList();
        List<Id<Person>> donors = rankByPassages(
                donorIds, station, tracker::routeImpact);
        int cloned = 0;
        int donorIndex = 0;
        int misses = 0;
        while (applied + 1.0e-9 < updateLimit
                && hasStatus(station, tracker, Status.UNDER)
                && !donors.isEmpty()) {
            Id<Person> donorId = donors.get(donorIndex++ % donors.size());
            Person clone;
            Object donorLock = personLocks.computeIfAbsent(donorId, ignored -> new Object());
            synchronized (donorLock) {
                if (state.isRemoved(donorId)) continue;
                clone = cloneFactory.prepareClone(donorId);
            }
            if (clone == null) {
                donors = donors.stream().filter(id -> !id.equals(donorId)).toList();
                donorIndex = 0;
                continue;
            }
            RouteImpact impact = tracker.extract(clone.getSelectedPlan());
            if (station.passages(impact) == 0) {
                cloneFactory.discardClone(clone);
                if (++misses >= MAX_CONSECUTIVE_CLONE_MISSES * Math.max(1, donors.size())) break;
                continue;
            }
            if (!tracker.canAddAt(station.links(), impact)) {
                cloneFactory.discardClone(clone);
                if (++misses >= MAX_CONSECUTIVE_CLONE_MISSES * Math.max(1, donors.size())) break;
                continue;
            }
            cloneFactory.commitClone(clone);
            tracker.update(clone);
            applied += tracker.flowContribution(impact, station.links());
            cloned++;
            misses = 0;
        }
        logStation(station, tracker, cloned, 0, restored);
        return new StationResult(station, cloned, 0, restored);
    }

    private StationResult reduce(CrossBorderStation station,
                                 TrafficScoringTracker tracker) {
        double updateLimit = tracker.requiredRemovalFlow(station.links()) * updateFraction;
        double applied = 0.0;
        List<Id<Person>> candidateIds = station.links().stream()
                .flatMap(link -> tracker.personsAt(link, TrafficCategory.CROSS_BORDER).stream())
                .distinct()
                .toList();
        List<Id<Person>> candidates = rankByPassages(
                candidateIds, station, tracker::routeImpact);
        int removed = 0;
        for (Id<Person> personId : candidates) {
            if (applied + 1.0e-9 >= updateLimit
                    || !hasStatus(station, tracker, Status.OVER)) break;
            Object lock = personLocks.computeIfAbsent(personId, ignored -> new Object());
            synchronized (lock) {
                if (state.isRemoved(personId)
                        || station.passages(tracker.routeImpact(personId)) == 0) continue;
                RouteImpact impact = tracker.routeImpact(personId);
                if (!tracker.canRemoveAt(station.links(), impact)) continue;
                if (populationEditor.removeCrossBorderTravel(personId, station)) {
                    Person person = person(personId);
                    if (person != null) {
                        tracker.update(person);
                        applied += tracker.flowContribution(impact, station.links());
                    }
                    removed++;
                }
            }
        }
        logStation(station, tracker, 0, removed, 0);
        return new StationResult(station, 0, removed, 0);
    }

    private Person person(Id<Person> personId) {
        synchronized (population) {
            return population.getPersons().get(personId);
        }
    }

    /** Snapshots mutable tracker values before sorting parallel station work. */
    static List<Id<Person>> rankByPassages(
            Collection<Id<Person>> personIds,
            CrossBorderStation station,
            Function<Id<Person>, RouteImpact> impactProvider) {
        return personIds.stream()
                .map(personId -> new RankedPerson(
                        personId, station.passages(impactProvider.apply(personId))))
                .sorted(Comparator.comparingInt(RankedPerson::passages)
                        .reversed()
                        .thenComparing(candidate -> candidate.personId().toString()))
                .map(RankedPerson::personId)
                .toList();
    }

    private boolean hasStatus(CrossBorderStation station,
                              TrafficScoringTracker tracker,
                              Status status) {
        StationGroup score = tracker.stationGroupScore(station.links());
        return score != null && score.status() == status;
    }

    private void logStation(CrossBorderStation station,
                            TrafficScoringTracker tracker,
                            int cloned, int removed, int restored) {
        StationGroup score = tracker.stationGroupScore(station.links());
        logger.info("Cross-border station {}: links={}, status={}, simulated-flow={}, "
                        + "observed-count={}, cloned={}, removed={}, restored={}",
                station.id(), station.links(), score.status(), score.simulatedFlow(),
                score.observedCount(), cloned, removed, restored);
    }

    public record Result(int cloned, int removed, int restored) { }
    private record RankedPerson(Id<Person> personId, int passages) { }
    private record StationResult(CrossBorderStation station,
                                 int cloned, int removed, int restored) { }
}
