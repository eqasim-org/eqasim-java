package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations;

import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.demand_calibration.Tools;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects link-level car/truck traversals from the population.
 *
 * <p>The traversal maps are consumed by {@link LinkErrorTracker} and by the
 * scoring formulas in {@link CalibrationFormulas}.</p>
 */
final class Analytics {
    private final TripListConverter tripListConverter;
    private final CountsProcessor countsProcessor;

    Analytics(TripListConverter tripListConverter, CountsProcessor countsProcessor) {
        this.tripListConverter = tripListConverter;
        this.countsProcessor = countsProcessor;
    }

    CountsProcessor countsProcessor() {
        return countsProcessor;
    }

    /**
     * Single pass over the population. Removed persons are scored with their
     * original (stored) plan so that restoration decisions see the trips that
     * would come back into the simulation.
     */
    PopulationTraversalStats collectPopulationTraversalStats(Map<Id<Person>, State.StoredPersonPlan> removedPlans,
                                                             Collection<? extends Person> persons) {
        Map<Id<Link>, Integer> allTraversals = new HashMap<>();
        Map<Id<Link>, Integer> subpopulationTraversals = new HashMap<>();
        Map<Id<Link>, Integer> crossBorderTraversals = new HashMap<>();

        Map<Id<Person>, Map<Id<Link>, Integer>> personTraversals = new HashMap<>();
        Map<Id<Person>, Map<Id<Link>, Integer>> crossBorderPersonTraversals = new HashMap<>();
        Map<Id<Person>, Map<Id<Link>, Integer>> removedPersonTraversals = new HashMap<>();

        for (Person person : persons) {
            State.StoredPersonPlan stored = removedPlans.get(person.getId());
            boolean isRemoved = stored != null;
            Plan plan = isRemoved ? stored.originalPlan() : person.getSelectedPlan();
            if (plan == null) {
                continue;
            }

            boolean inSubpopulation = Tools.isInSubPopulation(person);
            boolean crossBorder = Tools.isCrossBorderPerson(person);

            for (DiscreteModeChoiceTrip trip : tripListConverter.convert(plan)) {
                if (!Tools.isCarOrTruck(trip)) {
                    continue;
                }

                List<Id<Link>> linkIds = getRouteLinkIds(trip);
                for (Id<Link> linkId : linkIds) {
                    if (!countsProcessor.contains(linkId)) {
                        continue;
                    }

                    if (isRemoved) {
                        removedPersonTraversals
                                .computeIfAbsent(person.getId(), k -> new HashMap<>())
                                .merge(linkId, 1, Integer::sum);
                    } else {
                        allTraversals.merge(linkId, 1, Integer::sum);

                        if (inSubpopulation) {
                            subpopulationTraversals.merge(linkId, 1, Integer::sum);
                            personTraversals
                                    .computeIfAbsent(person.getId(), k -> new HashMap<>())
                                    .merge(linkId, 1, Integer::sum);
                        }

                        if (crossBorder) {
                            crossBorderTraversals.merge(linkId, 1, Integer::sum);
                            crossBorderPersonTraversals
                                    .computeIfAbsent(person.getId(), k -> new HashMap<>())
                                    .merge(linkId, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        return new PopulationTraversalStats(
                allTraversals,
                subpopulationTraversals,
                crossBorderTraversals,
                personTraversals,
                crossBorderPersonTraversals,
                removedPersonTraversals
        );
    }

    private List<Id<Link>> getRouteLinkIds(DiscreteModeChoiceTrip trip) {
        List<Id<Link>> routeLinkIds = new ArrayList<>();
        for (PlanElement element : trip.getInitialElements()) {
            if (element instanceof Leg leg && leg.getRoute() instanceof NetworkRoute route) {
                routeLinkIds.addAll(route.getLinkIds());
            }
        }
        return routeLinkIds;
    }

    record PopulationTraversalStats(
            Map<Id<Link>, Integer> allTraversals,
            Map<Id<Link>, Integer> subpopulationTraversals,
            Map<Id<Link>, Integer> crossBorderTraversals,
            Map<Id<Person>, Map<Id<Link>, Integer>> personTraversals,
            Map<Id<Person>, Map<Id<Link>, Integer>> crossBorderPersonTraversals,
            Map<Id<Person>, Map<Id<Link>, Integer>> removedPersonTraversals
    ) {
    }
}