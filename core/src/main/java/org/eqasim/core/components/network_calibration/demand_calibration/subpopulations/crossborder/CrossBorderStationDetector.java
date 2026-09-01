package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScoringTracker;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Incrementally discovers physical border stations and retains every accepted
 * station for the remainder of the run.
 */
public final class CrossBorderStationDetector {
    static final double MAX_PAIR_DISTANCE_METERS = 100.0;
    static final double SERIES_CONNECTION_THRESHOLD = 0.50;

    private static final Logger logger = LogManager.getLogger(CrossBorderStationDetector.class);

    private final Network network;
    private final double crossBorderShareThreshold;
    private final Map<String, CrossBorderStation> retained = new LinkedHashMap<>();
    private final Map<String, CrossBorderStation> seriesSuppressed = new LinkedHashMap<>();
    private final Set<Id<Link>> retainedLinks = new HashSet<>();
    private final Set<Id<Link>> seriesSuppressedLinks = new HashSet<>();
    private QuadTree<LinkPoint> countedLinkIndex;
    private Set<Id<Link>> indexedLinks = Set.of();

    public CrossBorderStationDetector(Network network, double crossBorderShareThreshold) {
        this.network = network;
        this.crossBorderShareThreshold = crossBorderShareThreshold;
    }

    /** Detects new stations, suppresses serial duplicates, and returns all retained stations. */
    public synchronized List<CrossBorderStation> update(TrafficScoringTracker tracker) {
        ensureIndex(tracker.monitoredLinks());
        Set<Id<Link>> eligible = tracker.monitoredLinks().stream()
                .filter(linkId -> !retainedLinks.contains(linkId)
                        && !seriesSuppressedLinks.contains(linkId))
                .filter(linkId -> tracker.crossBorderShare(linkId) >= crossBorderShareThreshold)
                .collect(java.util.stream.Collectors.toSet());

        List<PairCandidate> pairs = pairCandidates(eligible);
        Set<Id<Link>> pairedThisUpdate = new HashSet<>();
        List<CrossBorderStation> candidates = new ArrayList<>();
        for (PairCandidate pair : pairs) {
            if (pairedThisUpdate.contains(pair.first())
                    || pairedThisUpdate.contains(pair.second())) continue;
            pairedThisUpdate.add(pair.first());
            pairedThisUpdate.add(pair.second());
            candidates.add(station(pair.first(), pair.second()));
        }
        eligible.stream()
                .filter(linkId -> !pairedThisUpdate.contains(linkId))
                .sorted(Comparator.comparing(Id::toString))
                .map(CrossBorderStationDetector::singleLinkStation)
                .forEach(candidates::add);
        candidates.sort(Comparator
                .comparingDouble((CrossBorderStation station) ->
                        tracker.crossBorderShare(station.links()))
                .reversed()
                .thenComparing(CrossBorderStation::id));

        int suppressed = 0;
        for (CrossBorderStation candidate : candidates) {
            if (isSeriesDuplicate(candidate, tracker)) {
                seriesSuppressed.put(candidate.id(), candidate);
                seriesSuppressedLinks.addAll(candidate.links());
                suppressed++;
                continue;
            }
            retained.put(candidate.id(), candidate);
            retainedLinks.addAll(candidate.links());
        }
        if (!candidates.isEmpty()) {
            logger.info("Cross-border station detection: eligible-links={}, candidates={}, "
                            + "added={}, series-suppressed={}, retained={}",
                    eligible.size(), candidates.size(), candidates.size() - suppressed,
                    suppressed, retained.size());
        }
        return stations();
    }

    public synchronized List<CrossBorderStation> stations() {
        return retained.values().stream()
                .sorted(Comparator.comparing(CrossBorderStation::id))
                .toList();
    }

    private List<PairCandidate> pairCandidates(Set<Id<Link>> eligible) {
        Map<String, PairCandidate> unique = new HashMap<>();
        List<Id<Link>> ordered = eligible.stream()
                .sorted(Comparator.comparing(Id::toString))
                .toList();
        for (Id<Link> firstId : ordered) {
            LinkPoint first = point(firstId);
            if (first == null) continue;
            LinkPoint nearest = countedLinkIndex.getDisk(
                            first.coord().getX(), first.coord().getY(), MAX_PAIR_DISTANCE_METERS)
                    .stream()
                    .filter(candidate -> !candidate.linkId().equals(firstId))
                    .filter(candidate -> eligible.contains(candidate.linkId()))
                    .min(Comparator
                            .comparingDouble((LinkPoint candidate) ->
                                    distance(first.coord(), candidate.coord()))
                            .thenComparing(candidate -> candidate.linkId().toString()))
                    .orElse(null);
            if (nearest == null) continue;
            PairCandidate pair = PairCandidate.of(firstId, nearest.linkId(),
                    distance(first.coord(), nearest.coord()));
            unique.putIfAbsent(pair.key(), pair);
        }
        return unique.values().stream()
                .sorted(Comparator.comparingDouble(PairCandidate::distance)
                        .thenComparing(PairCandidate::key))
                .toList();
    }

    private boolean isSeriesDuplicate(CrossBorderStation candidate,
                                      TrafficScoringTracker tracker) {
        List<CrossBorderStation> knownStations = new ArrayList<>(retained.values());
        knownStations.addAll(seriesSuppressed.values());
        for (CrossBorderStation existing : knownStations) {
            double connection = tracker.crossBorderConnectionShare(
                    candidate.links(), existing.links());
            if (connection > SERIES_CONNECTION_THRESHOLD) {
                logger.info("Cross-border station {} suppressed as serial duplicate of {} "
                                + "(connected-share={})",
                        candidate.id(), existing.id(), connection);
                return true;
            }
        }
        return false;
    }

    private void ensureIndex(Set<Id<Link>> monitoredLinks) {
        if (countedLinkIndex != null && indexedLinks.equals(monitoredLinks)) return;
        List<LinkPoint> points = monitoredLinks.stream()
                .map(this::pointFromNetwork)
                .filter(java.util.Objects::nonNull)
                .toList();
        double minX = points.stream().mapToDouble(point -> point.coord().getX()).min().orElse(-1.0);
        double minY = points.stream().mapToDouble(point -> point.coord().getY()).min().orElse(-1.0);
        double maxX = points.stream().mapToDouble(point -> point.coord().getX()).max().orElse(1.0);
        double maxY = points.stream().mapToDouble(point -> point.coord().getY()).max().orElse(1.0);
        countedLinkIndex = new QuadTree<>(minX - 1.0, minY - 1.0, maxX + 1.0, maxY + 1.0);
        points.forEach(point -> countedLinkIndex.put(
                point.coord().getX(), point.coord().getY(), point));
        indexedLinks = Set.copyOf(monitoredLinks);
    }

    private LinkPoint point(Id<Link> linkId) {
        Link link = network.getLinks().get(linkId);
        return link == null ? null : new LinkPoint(linkId, midpoint(link));
    }

    private LinkPoint pointFromNetwork(Id<Link> linkId) {
        return point(linkId);
    }

    private static CrossBorderStation station(Id<Link> first, Id<Link> second) {
        Id<Link> in = first.toString().compareTo(second.toString()) <= 0 ? first : second;
        Id<Link> out = in.equals(first) ? second : first;
        return new CrossBorderStation(in + "<->" + out, in, out);
    }

    private static CrossBorderStation singleLinkStation(Id<Link> linkId) {
        return new CrossBorderStation(linkId.toString(), linkId);
    }

    private static Coord midpoint(Link link) {
        Coord from = link.getFromNode().getCoord();
        Coord to = link.getToNode().getCoord();
        return new Coord((from.getX() + to.getX()) * 0.5,
                (from.getY() + to.getY()) * 0.5);
    }

    private static double distance(Coord first, Coord second) {
        return Math.hypot(first.getX() - second.getX(), first.getY() - second.getY());
    }

    private record LinkPoint(Id<Link> linkId, Coord coord) { }

    private record PairCandidate(Id<Link> first, Id<Link> second,
                                 double distance, String key) {
        static PairCandidate of(Id<Link> first, Id<Link> second, double distance) {
            boolean ordered = first.toString().compareTo(second.toString()) <= 0;
            Id<Link> left = ordered ? first : second;
            Id<Link> right = ordered ? second : first;
            return new PairCandidate(left, right, distance, left + "<->" + right);
        }
    }
}
