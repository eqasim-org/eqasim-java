package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring;

import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.eqasim.core.components.network_calibration.demand_calibration.Tools;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore.Comparison;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore.Station;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore.StationGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore.StationInput;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Authoritative scoring and traffic state for one calibration update.
 *
 * <p>Route extraction is performed outside the synchronization boundary.
 * Queries and commits are synchronized so cross-border station workers can
 * safely share this object while expensive routing remains parallel.</p>
 */
public final class TrafficScoringTracker {
    private final Population population;
    private final CountsProcessor countsProcessor;
    private final FlowProcessor flowProcessor;
    private final RouteImpact.Extractor extractor;
    private final double sampleSize;
    private final double underThreshold;
    private final double overThreshold;

    private final Map<Id<Link>, MutableStation> stations = new HashMap<>();
    private final Map<Id<Person>, AgentRoute> agentRoutes = new HashMap<>();
    private final Map<Id<Link>, Set<Id<Person>>> crossBorderByStation = new HashMap<>();
    private final Map<Id<Link>, Set<Id<Person>>> freightByStation = new HashMap<>();

    public TrafficScoringTracker(Population population,
                                 CountsProcessor countsProcessor,
                                 FlowProcessor flowProcessor,
                                 RouteImpact.Extractor extractor,
                                 double sampleSize,
                                 double underThreshold,
                                 double overThreshold) {
        if (!Double.isFinite(sampleSize) || sampleSize <= 0.0) {
            throw new IllegalArgumentException("sampleSize must be finite and positive");
        }
        this.population = population;
        this.countsProcessor = countsProcessor;
        this.flowProcessor = flowProcessor;
        this.extractor = extractor;
        this.sampleSize = sampleSize;
        this.underThreshold = underThreshold;
        this.overThreshold = overThreshold;
    }

    /** Rebuilds the iteration baseline from actual simulated flows and selected plans. */
    public void refresh() {
        Collection<? extends Person> persons = population.getPersons().values();
        List<PersonImpact> impacts = persons.size() < 2_000
                ? persons.stream().map(this::extract).toList()
                : persons.parallelStream().map(this::extract).toList();

        synchronized (this) {
            stations.clear();
            agentRoutes.clear();
            crossBorderByStation.clear();
            freightByStation.clear();

            for (Id<Link> linkId : countsProcessor.linkIds()) {
                double observed = countsProcessor.getLinkCounts(linkId);
                double flow = flowProcessor.getTotalLinkFlow(linkId) / sampleSize;
                double flowPerPassage = flowProcessor.getFlowContributionPerPassage(linkId) / sampleSize;
                if (observed > 0.0 && Double.isFinite(flow) && flow >= 0.0
                        && Double.isFinite(flowPerPassage) && flowPerPassage > 0.0) {
                    stations.put(linkId, new MutableStation(flow, observed, flowPerPassage));
                }
            }

            for (PersonImpact personImpact : impacts) {
                mergeTraversals(personImpact.impact(), 1, personImpact.categories(), false);
                if (personImpact.categories() != 0) {
                    agentRoutes.put(personImpact.personId(),
                            new AgentRoute(personImpact.impact(), personImpact.categories()));
                    addToInvertedIndexes(personImpact.personId(), personImpact.impact(), personImpact.categories());
                }
            }
        }
    }

    public TrafficScore getScore(Person person) {
        return getScore(extractor.extract(person));
    }

    public TrafficScore getScore(Plan plan) {
        return getScore(extractor.extract(plan));
    }

    public TrafficScore getScore(DiscreteModeChoiceTrip trip) {
        return getScore(extractor.extract(trip));
    }

    public synchronized TrafficScore getScore(Id<Person> personId) {
        AgentRoute route = agentRoutes.get(personId);
        return score(route == null ? RouteImpact.empty() : route.impact(), null, null);
    }

    public synchronized TrafficScore getScore(RouteImpact impact) {
        return score(impact, null, null);
    }

    public void update(Person person) {
        replace(person, extractor.extract(person));
    }

    public void update(Plan plan) {
        if (plan == null || plan.getPerson() == null) {
            throw new IllegalArgumentException("An updated plan must have an owning person");
        }
        replace(plan.getPerson(), extractor.extract(plan));
    }

    public Comparison previewUpdate(Person person, Plan candidatePlan) {
        RouteImpact candidate = extractor.extract(candidatePlan);
        synchronized (this) {
            AgentRoute cached = agentRoutes.get(person.getId());
            RouteImpact oldImpact = cached == null ? extractor.extract(person): cached.impact();

            List<Id<Link>> affectedLinks = affectedLinks(oldImpact, candidate);
            return new Comparison(
                    score(oldImpact, null, null, affectedLinks),
                    score(candidate, oldImpact, candidate, affectedLinks));
        }
    }

    public RouteImpact extract(Plan plan) {
        return extractor.extract(plan);
    }

    public synchronized RouteImpact routeImpact(Id<Person> personId) {
        AgentRoute route = agentRoutes.get(personId);
        return route == null ? RouteImpact.empty() : route.impact();
    }

    public synchronized Set<Id<Link>> monitoredLinks() {
        return Set.copyOf(stations.keySet());
    }

    public synchronized List<Id<Person>> personsAt(Id<Link> station, TrafficCategory category) {
        Map<Id<Link>, Set<Id<Person>>> index = category == TrafficCategory.CROSS_BORDER
                ? crossBorderByStation : freightByStation;
        List<Id<Person>> result = new ArrayList<>(index.getOrDefault(station, Set.of()));
        result.sort(java.util.Comparator.comparing(Id::toString));
        return List.copyOf(result);
    }

    public synchronized Set<Id<Person>> crossBorderPersonsAt(Collection<Id<Link>> linkIds) {
        Set<Id<Person>> result = new HashSet<>();
        for (Id<Link> linkId : linkIds) {
            result.addAll(crossBorderByStation.getOrDefault(linkId, Set.of()));
        }
        return Set.copyOf(result);
    }

    /** Share of the smaller station's cross-border passages also seen at the other station. */
    public synchronized double crossBorderConnectionShare(
            Collection<Id<Link>> firstLinks, Collection<Id<Link>> secondLinks) {
        Set<Id<Person>> firstPersons = crossBorderPersonsAt(firstLinks);
        Set<Id<Person>> secondPersons = crossBorderPersonsAt(secondLinks);
        if (firstPersons.isEmpty() || secondPersons.isEmpty()) return 0.0;

        long firstPassages = stationPassages(firstPersons, firstLinks);
        long secondPassages = stationPassages(secondPersons, secondLinks);
        long connectedPassages = 0;
        Set<Id<Person>> smaller = firstPersons.size() <= secondPersons.size()
                ? firstPersons : secondPersons;
        Set<Id<Person>> other = smaller == firstPersons ? secondPersons : firstPersons;
        for (Id<Person> personId : smaller) {
            if (!other.contains(personId)) continue;
            AgentRoute route = agentRoutes.get(personId);
            if (route == null) continue;
            connectedPassages += Math.min(
                    passagesOn(route.impact(), firstLinks),
                    passagesOn(route.impact(), secondLinks));
        }
        long denominator = Math.min(firstPassages, secondPassages);
        return denominator == 0 ? 0.0 : connectedPassages / (double) denominator;
    }

    public synchronized double crossBorderShare(Id<Link> linkId) {
        MutableStation station = stations.get(linkId);
        return station == null || station.totalPassages == 0 ? 0.0
                : station.crossBorderPassages / (double) station.totalPassages;
    }

    public synchronized double crossBorderShare(Collection<Id<Link>> linkIds) {
        int total = 0;
        int crossBorder = 0;
        for (Id<Link> linkId : linkIds) {
            MutableStation station = stations.get(linkId);
            if (station != null) {
                total += station.totalPassages;
                crossBorder += station.crossBorderPassages;
            }
        }
        return total == 0 ? 0.0 : crossBorder / (double) total;
    }

    public synchronized Station stationScore(Id<Link> linkId) {
        MutableStation station = stations.get(linkId);
        return station == null ? null : TrafficScore.station(
                stationInput(linkId, 0, station.currentFlow), underThreshold, overThreshold);
    }

    /** Combines directional count links into one logical counting station. */
    public synchronized StationGroup stationGroupScore(Collection<Id<Link>> linkIds) {
        List<Id<Link>> monitored = linkIds.stream()
                .filter(stations::containsKey)
                .distinct()
                .sorted(java.util.Comparator.comparing(Id::toString))
                .toList();
        if (monitored.isEmpty()) return null;

        double flow = 0.0;
        double observed = 0.0;
        for (Id<Link> linkId : monitored) {
            MutableStation station = stations.get(linkId);
            flow += station.currentFlow;
            observed += station.observed;
        }
        return TrafficScore.stationGroup(
                monitored, flow, observed, underThreshold, overThreshold);
    }

    /** Full-sample flow represented by this route on the supplied station links. */
    public synchronized double flowContribution(RouteImpact impact, Collection<Id<Link>> linkIds) {
        double contribution = 0.0;
        for (Id<Link> linkId : linkIds) {
            MutableStation station = stations.get(linkId);
            if (station != null) {
                contribution += impact.passagesOn(linkId) * station.flowPerPassage;
            }
        }
        return contribution;
    }

    public synchronized boolean canAddAt(Collection<Id<Link>> linkIds, RouteImpact impact) {
        StationGroup score = stationGroupScore(linkIds);
        if (score == null) return false;
        double after = score.simulatedFlow() + flowContribution(impact, linkIds);
        return after <= score.observedCount() * (1.0 + overThreshold) + 1.0e-9;
    }

    public synchronized boolean canRemoveAt(Collection<Id<Link>> linkIds, RouteImpact impact) {
        StationGroup score = stationGroupScore(linkIds);
        if (score == null) return false;
        double after = score.simulatedFlow() - flowContribution(impact, linkIds);
        return after >= score.observedCount() * (1.0 - underThreshold) - 1.0e-9;
    }

    public synchronized double requiredAdditionFlow(Collection<Id<Link>> linkIds) {
        StationGroup score = stationGroupScore(linkIds);
        return score == null ? 0.0 : Math.max(0.0,
                score.observedCount() * (1.0 - underThreshold) - score.simulatedFlow());
    }

    public synchronized double requiredRemovalFlow(Collection<Id<Link>> linkIds) {
        StationGroup score = stationGroupScore(linkIds);
        return score == null ? 0.0 : Math.max(0.0,
                score.simulatedFlow() - score.observedCount() * (1.0 + overThreshold));
    }

    public synchronized double currentFlow(Id<Link> linkId) {
        MutableStation station = stations.get(linkId);
        return station == null ? 0.0 : station.currentFlow;
    }

    private PersonImpact extract(Person person) {
        return new PersonImpact(person.getId(), extractor.extract(person), categoriesOf(person));
    }

    private synchronized void replace(Person person, RouteImpact replacement) {
        int newCategories = categoriesOf(person);
        AgentRoute old = agentRoutes.get(person.getId());
        RouteImpact oldImpact = old == null ? RouteImpact.empty() : old.impact();
        int oldCategories = old == null ? newCategories : old.categories();

        removeFromInvertedIndexes(person.getId(), oldImpact, oldCategories);
        mergeTraversals(oldImpact, -1, oldCategories, true);
        mergeTraversals(replacement, 1, newCategories, true);

        if (newCategories == 0) {
            agentRoutes.remove(person.getId());
        } else {
            agentRoutes.put(person.getId(), new AgentRoute(replacement, newCategories));
            addToInvertedIndexes(person.getId(), replacement, newCategories);
        }
    }

    private TrafficScore score(RouteImpact impact,
                               RouteImpact oldImpact,
                               RouteImpact newImpact) {
        List<Id<Link>> links = new ArrayList<>(impact.size());
        for (int index = 0; index < impact.size(); index++) {
            links.add(impact.linkAt(index));
        }
        return score(impact, oldImpact, newImpact, links);
    }

    private TrafficScore score(RouteImpact impact,
                               RouteImpact oldImpact,
                               RouteImpact newImpact,
                               List<Id<Link>> evaluatedLinks) {
        List<StationInput> inputs = new ArrayList<>(evaluatedLinks.size());

        for (Id<Link> linkId : evaluatedLinks) {
            MutableStation station = stations.get(linkId);
            if (station == null) continue;

            int routePassages = impact.passagesOn(linkId);
            double evaluatedFlow = station.currentFlow;
            if (oldImpact != null && newImpact != null) {
                int delta = newImpact.passagesOn(linkId) - oldImpact.passagesOn(linkId);
                evaluatedFlow = Math.max(0.0, evaluatedFlow + delta * station.flowPerPassage);
            }
            inputs.add(stationInput(linkId, routePassages, evaluatedFlow));
        }
        return TrafficScore.compute(inputs, underThreshold, overThreshold);
    }

    private static List<Id<Link>> affectedLinks(RouteImpact oldImpact, RouteImpact newImpact) {
        Set<Id<Link>> links = new HashSet<>();
        for (int index = 0; index < oldImpact.size(); index++) links.add(oldImpact.linkAt(index));
        for (int index = 0; index < newImpact.size(); index++) links.add(newImpact.linkAt(index));
        return links.stream().sorted(java.util.Comparator.comparing(Id::toString)).toList();
    }

    private StationInput stationInput(Id<Link> linkId, int passages, double flow) {
        MutableStation station = stations.get(linkId);
        return new StationInput(linkId, passages, flow, station.observed);
    }

    private void mergeTraversals(RouteImpact impact, int direction,
                                 int categories, boolean updateFlow) {
        for (int index = 0; index < impact.size(); index++) {
            MutableStation station = stations.get(impact.linkAt(index));
            if (station == null) continue;
            int passages = impact.passagesAt(index);
            station.totalPassages = Math.max(0,
                    station.totalPassages + direction * passages);
            if (TrafficCategory.contains(categories, TrafficCategory.CROSS_BORDER)) {
                station.crossBorderPassages = Math.max(0,
                        station.crossBorderPassages + direction * passages);
            }
            if (updateFlow) {
                station.currentFlow = Math.max(0.0,
                        station.currentFlow + direction * passages * station.flowPerPassage);
            }
        }
    }

    private void addToInvertedIndexes(Id<Person> personId, RouteImpact impact, int categories) {
        addToIndex(crossBorderByStation, personId, impact,
                TrafficCategory.contains(categories, TrafficCategory.CROSS_BORDER));
        addToIndex(freightByStation, personId, impact,
                TrafficCategory.contains(categories, TrafficCategory.FREIGHT));
    }

    private void removeFromInvertedIndexes(Id<Person> personId, RouteImpact impact, int categories) {
        removeFromIndex(crossBorderByStation, personId, impact,
                TrafficCategory.contains(categories, TrafficCategory.CROSS_BORDER));
        removeFromIndex(freightByStation, personId, impact,
                TrafficCategory.contains(categories, TrafficCategory.FREIGHT));
    }

    private static void addToIndex(Map<Id<Link>, Set<Id<Person>>> index,
                                   Id<Person> personId, RouteImpact impact, boolean add) {
        if (!add) return;
        for (int i = 0; i < impact.size(); i++) {
            index.computeIfAbsent(impact.linkAt(i), ignored -> new HashSet<>()).add(personId);
        }
    }

    private static void removeFromIndex(Map<Id<Link>, Set<Id<Person>>> index,
                                        Id<Person> personId, RouteImpact impact, boolean remove) {
        if (!remove) return;
        for (int i = 0; i < impact.size(); i++) {
            Set<Id<Person>> persons = index.get(impact.linkAt(i));
            if (persons != null) {
                persons.remove(personId);
                if (persons.isEmpty()) index.remove(impact.linkAt(i));
            }
        }
    }

    private static int categoriesOf(Person person) {
        int categories = 0;
        if (Tools.isCrossBorderPerson(person)) categories |= TrafficCategory.CROSS_BORDER.mask();
        if (Tools.isFreightPerson(person)) categories |= TrafficCategory.FREIGHT.mask();
        return categories;
    }

    private long stationPassages(Set<Id<Person>> personIds, Collection<Id<Link>> linkIds) {
        long passages = 0;
        for (Id<Person> personId : personIds) {
            AgentRoute route = agentRoutes.get(personId);
            if (route != null) passages += passagesOn(route.impact(), linkIds);
        }
        return passages;
    }

    private static int passagesOn(RouteImpact impact, Collection<Id<Link>> linkIds) {
        int passages = 0;
        for (Id<Link> linkId : linkIds) passages += impact.passagesOn(linkId);
        return passages;
    }

    private record PersonImpact(Id<Person> personId, RouteImpact impact, int categories) { }
    private record AgentRoute(RouteImpact impact, int categories) { }

    private static final class MutableStation {
        double currentFlow;
        final double observed;
        final double flowPerPassage;
        int totalPassages;
        int crossBorderPassages;
        MutableStation(double currentFlow, double observed, double flowPerPassage) {
            this.currentFlow = currentFlow;
            this.observed = observed;
            this.flowPerPassage = flowPerPassage;
        }
    }
}
