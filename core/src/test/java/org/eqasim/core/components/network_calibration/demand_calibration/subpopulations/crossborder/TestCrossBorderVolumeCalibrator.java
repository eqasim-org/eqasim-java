package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.RouteImpact;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore.StationGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore.Status;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficCategory;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScoringTracker;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCrossBorderVolumeCalibrator {
    @Test
    public void donorRankingSnapshotsEachMutableRouteImpactOnce() {
        Id<Link> link = Id.createLinkId("border");
        CrossBorderStation station = new CrossBorderStation("border", link);
        java.util.List<Id<Person>> persons = java.util.stream.IntStream.range(0, 100)
                .mapToObj(index -> Id.<Person>createPersonId("person_" + index))
                .toList();
        ConcurrentHashMap<Id<Person>, AtomicInteger> reads = new ConcurrentHashMap<>();

        java.util.List<Id<Person>> ranked = CrossBorderVolumeCalibrator.rankByPassages(
                persons, station, personId -> {
                    int read = reads.computeIfAbsent(personId, ignored -> new AtomicInteger())
                            .incrementAndGet();
                    return RouteImpact.from(java.util.Map.of(link, read));
                });

        assertEquals(100, ranked.size());
        assertEquals(100, reads.size());
        assertEquals(1, reads.values().stream()
                .mapToInt(AtomicInteger::get).max().orElseThrow());
    }

    @Test
    public void overestimatedStationRemovesCrossBorderAgentToAcceptedBand() {
        Fixture fixture = fixture(121.0);
        when(fixture.extractor.extract(fixture.person))
                .thenReturn(fixture.impact, RouteImpact.empty());
        fixture.tracker.refresh();
        when(fixture.state.removedAt(fixture.crossBorderStation)).thenReturn(Set.of());
        when(fixture.state.isRemoved(fixture.person.getId())).thenReturn(false);
        when(fixture.editor.removeCrossBorderTravel(
                fixture.person.getId(), fixture.crossBorderStation))
                .thenReturn(true);

        CrossBorderVolumeCalibrator.Result result = fixture.calibrator.update(fixture.tracker);

        assertEquals(1, result.removed());
        assertEquals(120.0, fixture.tracker
                .stationGroupScore(fixture.crossBorderStation.links()).simulatedFlow(), 1.0e-9);
    }

    @Test
    public void underestimatedStationAcceptsCloneThatCrossesThatStation() {
        Fixture fixture = fixture(79.0);
        Person clone = person(fixture.scenario, "clone", false);
        when(fixture.extractor.extract(fixture.person)).thenReturn(fixture.impact);
        when(fixture.extractor.extract(clone.getSelectedPlan())).thenReturn(fixture.impact);
        when(fixture.extractor.extract(clone)).thenReturn(fixture.impact);
        fixture.tracker.refresh();
        when(fixture.state.removedAt(fixture.crossBorderStation)).thenReturn(Set.of());
        when(fixture.state.isRemoved(fixture.person.getId())).thenReturn(false);
        when(fixture.cloneFactory.prepareClone(fixture.person.getId())).thenReturn(clone);

        CrossBorderVolumeCalibrator.Result result = fixture.calibrator.update(fixture.tracker);

        assertEquals(1, result.cloned());
        assertEquals(80.0, fixture.tracker
                .stationGroupScore(fixture.crossBorderStation.links()).simulatedFlow(), 1.0e-9);
    }

    @Test
    public void cloneIsAcceptedForTargetStationEvenWhenAnotherStationWorsens() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Person donor = person(scenario, "crossborder", true);
        Person clone = person(scenario, "clone", false);
        java.util.List<Id<Link>> targetLinks = addStationLinks(scenario, "a_target", 0.0);
        java.util.List<Id<Link>> otherLinks = addStationLinks(scenario, "z_other", 10_000.0);
        Id<Link> target = targetLinks.getFirst();
        Id<Link> other = otherLinks.getFirst();
        RouteImpact impact = RouteImpact.from(java.util.Map.of(target, 1, other, 1));

        CountsProcessor counts = mock(CountsProcessor.class);
        when(counts.linkIds()).thenReturn(Set.of(
                targetLinks.get(0), targetLinks.get(1), otherLinks.get(0), otherLinks.get(1)));
        for (Id<Link> link : counts.linkIds()) when(counts.getLinkCounts(link)).thenReturn(50.0f);
        FlowProcessor flows = mock(FlowProcessor.class);
        when(flows.getTotalLinkFlow(targetLinks.get(0))).thenReturn(39.0);
        when(flows.getTotalLinkFlow(targetLinks.get(1))).thenReturn(40.0);
        when(flows.getTotalLinkFlow(otherLinks.get(0))).thenReturn(60.0);
        when(flows.getTotalLinkFlow(otherLinks.get(1))).thenReturn(60.0);
        for (Id<Link> link : counts.linkIds()) {
            when(flows.getFlowContributionPerPassage(link)).thenReturn(1.0);
        }
        RouteImpact.Extractor extractor = mock(RouteImpact.Extractor.class);
        when(extractor.extract(donor)).thenReturn(impact);
        when(extractor.extract(clone.getSelectedPlan())).thenReturn(impact);
        when(extractor.extract(clone)).thenReturn(impact);
        TrafficScoringTracker tracker = new TrafficScoringTracker(
                scenario.getPopulation(), counts, flows, extractor, 1.0, 0.20, 0.20);
        tracker.refresh();

        CrossBorderState state = mock(CrossBorderState.class);
        when(state.isRemoved(donor.getId())).thenReturn(false);
        CrossBorderCloneFactory cloneFactory = mock(CrossBorderCloneFactory.class);
        when(cloneFactory.prepareClone(donor.getId())).thenReturn(clone);
        CrossBorderStation targetStation = station("target", targetLinks);
        CrossBorderStation otherStation = station("other", otherLinks);
        CrossBorderStationDetector detector = mock(CrossBorderStationDetector.class);
        when(detector.update(tracker)).thenReturn(java.util.List.of(targetStation, otherStation));
        CrossBorderVolumeCalibrator calibrator = new CrossBorderVolumeCalibrator(
                scenario.getPopulation(), state, mock(CrossBorderPopulationEditor.class),
                cloneFactory, detector, 0.67, 2);

        CrossBorderVolumeCalibrator.Result result = calibrator.update(tracker);

        assertEquals(1, result.cloned());
        assertEquals(80.0,
                tracker.stationGroupScore(targetLinks).simulatedFlow(), 1.0e-9);
        assertEquals(121.0,
                tracker.stationGroupScore(otherLinks).simulatedFlow(), 1.0e-9);
    }

    @Test
    public void roundTripCloneCountsBothDirectionalLinksAtOneStation() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Person donor = person(scenario, "crossborder", true);
        Person clone = person(scenario, "clone", false);
        java.util.List<Id<Link>> links = addStationLinks(scenario, "border", 0.0);
        RouteImpact roundTrip = RouteImpact.from(java.util.Map.of(
                links.get(0), 1, links.get(1), 1));

        CountsProcessor counts = mock(CountsProcessor.class);
        when(counts.linkIds()).thenReturn(Set.copyOf(links));
        FlowProcessor flows = mock(FlowProcessor.class);
        for (Id<Link> link : links) {
            when(counts.getLinkCounts(link)).thenReturn(50.0f);
            when(flows.getTotalLinkFlow(link)).thenReturn(39.0);
            when(flows.getFlowContributionPerPassage(link)).thenReturn(1.0);
        }
        RouteImpact.Extractor extractor = mock(RouteImpact.Extractor.class);
        when(extractor.extract(donor)).thenReturn(roundTrip);
        when(extractor.extract(clone.getSelectedPlan())).thenReturn(roundTrip);
        when(extractor.extract(clone)).thenReturn(roundTrip);
        TrafficScoringTracker tracker = new TrafficScoringTracker(
                scenario.getPopulation(), counts, flows, extractor, 1.0, 0.20, 0.20);
        tracker.refresh();

        CrossBorderState state = mock(CrossBorderState.class);
        when(state.isRemoved(donor.getId())).thenReturn(false);
        CrossBorderCloneFactory cloneFactory = mock(CrossBorderCloneFactory.class);
        when(cloneFactory.prepareClone(donor.getId())).thenReturn(clone);
        CrossBorderStation station = station("border", links);
        CrossBorderStationDetector detector = mock(CrossBorderStationDetector.class);
        when(detector.update(tracker)).thenReturn(java.util.List.of(station));
        CrossBorderVolumeCalibrator calibrator = new CrossBorderVolumeCalibrator(
                scenario.getPopulation(), state, mock(CrossBorderPopulationEditor.class),
                cloneFactory, detector, 0.67, 2);

        CrossBorderVolumeCalibrator.Result result = calibrator.update(tracker);

        assertEquals(1, result.cloned());
        assertEquals(80.0, tracker.stationGroupScore(links).simulatedFlow(), 1.0e-9);
    }

    @Test
    public void expansionAppliesOnlyConfiguredFractionOfRequiredCorrection() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Person donor = person(scenario, "donor", true);
        Id<Link> in = Id.createLinkId("in");
        CrossBorderStation station = new CrossBorderStation("border", in);
        RouteImpact impact = RouteImpact.from(java.util.Map.of(in, 1));
        TrafficScoringTracker tracker = mock(TrafficScoringTracker.class);
        when(tracker.stationGroupScore(station.links())).thenReturn(new StationGroup(
                station.links(), 80.0, 100.0, -0.20, Status.UNDER));
        when(tracker.requiredAdditionFlow(station.links())).thenReturn(10.0);
        when(tracker.personsAt(in, TrafficCategory.CROSS_BORDER))
                .thenReturn(java.util.List.of(donor.getId()));
        when(tracker.routeImpact(donor.getId())).thenReturn(impact);
        when(tracker.canAddAt(station.links(), impact)).thenReturn(true);
        when(tracker.flowContribution(impact, station.links())).thenReturn(1.0);
        when(tracker.extract(org.mockito.ArgumentMatchers.any(Plan.class))).thenReturn(impact);

        Person[] clones = new Person[7];
        for (int i = 0; i < clones.length; i++) {
            clones[i] = person(scenario, "clone_" + i, false);
        }
        CrossBorderCloneFactory cloneFactory = mock(CrossBorderCloneFactory.class);
        when(cloneFactory.prepareClone(donor.getId())).thenReturn(
                clones[0], clones[1], clones[2], clones[3], clones[4], clones[5], clones[6]);
        CrossBorderStationDetector detector = mock(CrossBorderStationDetector.class);
        when(detector.update(tracker)).thenReturn(java.util.List.of(station));
        CrossBorderState state = mock(CrossBorderState.class);
        when(state.removedAt(station)).thenReturn(Set.of());
        CrossBorderVolumeCalibrator calibrator = new CrossBorderVolumeCalibrator(
                scenario.getPopulation(), state, mock(CrossBorderPopulationEditor.class),
                cloneFactory, detector, 0.67, 1);

        CrossBorderVolumeCalibrator.Result result = calibrator.update(tracker);

        assertEquals(7, result.cloned());
    }

    @Test
    public void reductionAppliesOnlyConfiguredFractionOfRequiredCorrection() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Id<Link> in = Id.createLinkId("in");
        Id<Link> out = Id.createLinkId("out");
        CrossBorderStation station = new CrossBorderStation(
                "border", in, out);
        RouteImpact impact = RouteImpact.from(java.util.Map.of(in, 1));
        java.util.List<Id<Person>> personIds = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            personIds.add(person(scenario, "person_" + i, true).getId());
        }
        TrafficScoringTracker tracker = mock(TrafficScoringTracker.class);
        when(tracker.stationGroupScore(station.links())).thenReturn(new StationGroup(
                station.links(), 120.0, 100.0, 0.20, Status.OVER));
        when(tracker.requiredRemovalFlow(station.links())).thenReturn(10.0);
        when(tracker.personsAt(in, TrafficCategory.CROSS_BORDER)).thenReturn(personIds);
        when(tracker.personsAt(out, TrafficCategory.CROSS_BORDER))
                .thenReturn(java.util.List.of());
        for (Id<Person> personId : personIds) when(tracker.routeImpact(personId)).thenReturn(impact);
        when(tracker.canRemoveAt(station.links(), impact)).thenReturn(true);
        when(tracker.flowContribution(impact, station.links())).thenReturn(1.0);
        CrossBorderPopulationEditor editor = mock(CrossBorderPopulationEditor.class);
        for (Id<Person> personId : personIds) {
            when(editor.removeCrossBorderTravel(personId, station)).thenReturn(true);
        }
        CrossBorderStationDetector detector = mock(CrossBorderStationDetector.class);
        when(detector.update(tracker)).thenReturn(java.util.List.of(station));
        CrossBorderVolumeCalibrator calibrator = new CrossBorderVolumeCalibrator(
                scenario.getPopulation(), mock(CrossBorderState.class), editor,
                mock(CrossBorderCloneFactory.class), detector, 0.67, 1);

        CrossBorderVolumeCalibrator.Result result = calibrator.update(tracker);

        assertEquals(7, result.removed());
    }

    private Fixture fixture(double flow) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Person person = person(scenario, "crossborder", true);
        java.util.List<Id<Link>> links = addStationLinks(scenario, "border", 0.0);
        Id<Link> station = links.getFirst();
        CrossBorderStation crossBorderStation = station("border", links);
        RouteImpact impact = RouteImpact.from(java.util.Map.of(station, 1));
        CountsProcessor counts = mock(CountsProcessor.class);
        when(counts.linkIds()).thenReturn(Set.copyOf(links));
        FlowProcessor flows = mock(FlowProcessor.class);
        for (Id<Link> link : links) {
            when(counts.getLinkCounts(link)).thenReturn(50.0f);
            when(flows.getTotalLinkFlow(link)).thenReturn(flow * 0.5);
            when(flows.getFlowContributionPerPassage(link)).thenReturn(1.0);
        }
        RouteImpact.Extractor extractor = mock(RouteImpact.Extractor.class);
        TrafficScoringTracker tracker = new TrafficScoringTracker(
                scenario.getPopulation(), counts, flows, extractor, 1.0, 0.20, 0.20);
        CrossBorderState state = mock(CrossBorderState.class);
        CrossBorderPopulationEditor editor = mock(CrossBorderPopulationEditor.class);
        CrossBorderCloneFactory cloneFactory = mock(CrossBorderCloneFactory.class);
        CrossBorderStationDetector detector = mock(CrossBorderStationDetector.class);
        when(detector.update(tracker)).thenReturn(java.util.List.of(crossBorderStation));
        CrossBorderVolumeCalibrator calibrator = new CrossBorderVolumeCalibrator(
                scenario.getPopulation(), state, editor, cloneFactory,
                detector, 0.67, 2);
        return new Fixture(scenario, person, station, crossBorderStation, impact, extractor, tracker,
                state, editor, cloneFactory, calibrator);
    }

    private static Person person(Scenario scenario, String id, boolean add) {
        Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(id));
        person.getAttributes().putAttribute("isCrossBorder", true);
        Plan plan = scenario.getPopulation().getFactory().createPlan();
        person.addPlan(plan);
        person.setSelectedPlan(plan);
        if (add) scenario.getPopulation().addPerson(person);
        return person;
    }

    private static java.util.List<Id<Link>> addStationLinks(
            Scenario scenario, String prefix, double x) {
        var factory = scenario.getNetwork().getFactory();
        var first = factory.createNode(Id.createNodeId(prefix + "_a"), new Coord(x, 0.0));
        var second = factory.createNode(Id.createNodeId(prefix + "_b"), new Coord(x + 100.0, 0.0));
        scenario.getNetwork().addNode(first);
        scenario.getNetwork().addNode(second);
        Link inbound = factory.createLink(Id.createLinkId(prefix + "_in"), first, second);
        Link outbound = factory.createLink(Id.createLinkId(prefix + "_out"), second, first);
        scenario.getNetwork().addLink(inbound);
        scenario.getNetwork().addLink(outbound);
        return java.util.List.of(inbound.getId(), outbound.getId());
    }

    private static CrossBorderStation station(
            String id, java.util.List<Id<Link>> links) {
        return new CrossBorderStation(id, links.get(0), links.get(1));
    }

    private record Fixture(
            Scenario scenario,
            Person person,
            Id<Link> station,
            CrossBorderStation crossBorderStation,
            RouteImpact impact,
            RouteImpact.Extractor extractor,
            TrafficScoringTracker tracker,
            CrossBorderState state,
            CrossBorderPopulationEditor editor,
            CrossBorderCloneFactory cloneFactory,
            CrossBorderVolumeCalibrator calibrator
    ) { }
}
