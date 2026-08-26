package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring;

import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.demand_calibration.Tools;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Compact immutable counted-link passages, sorted for deterministic behavior. */
public final class RouteImpact {
    private static final RouteImpact EMPTY = new RouteImpact(List.of(), new int[0]);

    private final List<Id<Link>> links;
    private final int[] passages;

    private RouteImpact(List<Id<Link>> links, int[] passages) {
        this.links = links;
        this.passages = passages;
    }

    public static RouteImpact empty() {
        return EMPTY;
    }

    public static RouteImpact from(Map<Id<Link>, Integer> traversals) {
        if (traversals.isEmpty()) {
            return EMPTY;
        }
        List<Map.Entry<Id<Link>, Integer>> entries = new ArrayList<>(traversals.entrySet());
        entries.removeIf(entry -> entry.getValue() == null || entry.getValue() <= 0);
        entries.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        if (entries.isEmpty()) {
            return EMPTY;
        }
        List<Id<Link>> links = new ArrayList<>(entries.size());
        int[] passages = new int[entries.size()];
        for (int index = 0; index < entries.size(); index++) {
            links.add(entries.get(index).getKey());
            passages[index] = entries.get(index).getValue();
        }
        return new RouteImpact(List.copyOf(links), passages);
    }

    public boolean isEmpty() { return links.isEmpty(); }
    public int size() { return links.size(); }
    public Id<Link> linkAt(int index) { return links.get(index); }
    public int passagesAt(int index) { return passages[index]; }

    public int passagesOn(Id<Link> linkId) {
        for (int index = 0; index < links.size(); index++) {
            if (links.get(index).equals(linkId)) {
                return passages[index];
            }
        }
        return 0;
    }

    /**
     * Converts MATSim plans and trips into the compact counted-link form above.
     * Keeping extraction beside the representation gives every calibrator one
     * authoritative route-impact definition.
     */
    public static final class Extractor {
        private final TripListConverter tripListConverter;
        private final CountsProcessor countsProcessor;

        public Extractor(TripListConverter tripListConverter, CountsProcessor countsProcessor) {
            this.tripListConverter = tripListConverter;
            this.countsProcessor = countsProcessor;
        }

        public RouteImpact extract(Person person) {
            return person == null ? RouteImpact.empty() : extract(person.getSelectedPlan());
        }

        public RouteImpact extract(Plan plan) {
            if (plan == null) return RouteImpact.empty();

            Map<Id<Link>, Integer> passages = new java.util.HashMap<>();
            for (DiscreteModeChoiceTrip trip : tripListConverter.convert(plan)) {
                merge(passages, trip);
            }
            return RouteImpact.from(passages);
        }

        public RouteImpact extract(DiscreteModeChoiceTrip trip) {
            Map<Id<Link>, Integer> passages = new java.util.HashMap<>();
            merge(passages, trip);
            return RouteImpact.from(passages);
        }

        private void merge(Map<Id<Link>, Integer> passages, DiscreteModeChoiceTrip trip) {
            if (trip == null || !Tools.isCarOrTruck(trip)) return;

            for (PlanElement element : trip.getInitialElements()) {
                if (!(element instanceof Leg leg) || !(leg.getRoute() instanceof NetworkRoute route)) {
                    continue;
                }
                for (Id<Link> linkId : route.getLinkIds()) {
                    if (countsProcessor.contains(linkId)) {
                        passages.merge(linkId, 1, Integer::sum);
                    }
                }
            }
        }
    }
}
