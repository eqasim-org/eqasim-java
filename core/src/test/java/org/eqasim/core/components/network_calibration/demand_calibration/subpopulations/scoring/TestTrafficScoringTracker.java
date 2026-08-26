package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring;

import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore.Comparison;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScore.Status;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTrafficScoringTracker {
    private final Id<Link> over = Id.createLinkId("over");
    private final Id<Link> under = Id.createLinkId("under");

    @Test
    public void personPlanAndTripOverloadsUseOneScoreDefinition() {
        Fixture fixture = fixture();
        RouteImpact impact = RouteImpact.from(Map.of(over, 1, under, 1));
        DiscreteModeChoiceTrip trip = mock(DiscreteModeChoiceTrip.class);
        when(fixture.extractor.extract(fixture.person)).thenReturn(impact);
        when(fixture.extractor.extract(fixture.plan)).thenReturn(impact);
        when(fixture.extractor.extract(trip)).thenReturn(impact);
        fixture.tracker.refresh();

        TrafficScore personScore = fixture.tracker.getScore(fixture.person);

        assertEquals(personScore, fixture.tracker.getScore(fixture.plan));
        assertEquals(personScore, fixture.tracker.getScore(trip));
        assertEquals(2, personScore.countingStations());
        assertEquals(1, personScore.overestimatedStations());
        assertEquals(1, personScore.underestimatedStations());
        assertEquals(-2, personScore.score());
        assertFalse(personScore.isBalanced());
    }

    @Test
    public void updateSubtractsOldRouteAndAddsNewRouteImmediately() {
        Fixture fixture = fixture();
        RouteImpact oldImpact = RouteImpact.from(Map.of(over, 1));
        RouteImpact newImpact = RouteImpact.from(Map.of(under, 1));
        when(fixture.extractor.extract(fixture.person)).thenReturn(oldImpact, newImpact);

        fixture.tracker.refresh();
        assertEquals(Status.OVER, fixture.tracker.stationScore(over).status());
        assertEquals(-1, fixture.tracker.getScore(fixture.person.getId()).score());

        fixture.tracker.update(fixture.person);

        assertEquals(120.0, fixture.tracker.currentFlow(over), 1.0e-9);
        assertEquals(80.0, fixture.tracker.currentFlow(under), 1.0e-9);
        assertEquals(Status.ACCEPTABLE, fixture.tracker.stationScore(over).status());
        assertEquals(Status.ACCEPTABLE, fixture.tracker.stationScore(under).status());
        assertEquals(0, fixture.tracker.getScore(fixture.person.getId()).score());
    }

    @Test
    public void previewEvaluatesReplacementWithoutMutatingLiveState() {
        Fixture fixture = fixture();
        RouteImpact oldImpact = RouteImpact.from(Map.of(over, 1));
        RouteImpact newImpact = RouteImpact.from(Map.of(under, 1));
        Plan candidate = fixture.scenario.getPopulation().getFactory().createPlan();
        when(fixture.extractor.extract(fixture.person)).thenReturn(oldImpact);
        when(fixture.extractor.extract(candidate)).thenReturn(newImpact);
        fixture.tracker.refresh();

        Comparison comparison = fixture.tracker.previewUpdate(fixture.person, candidate);

        assertTrue(comparison.improves());
        assertEquals(121.0, fixture.tracker.currentFlow(over), 1.0e-9);
        assertEquals(79.0, fixture.tracker.currentFlow(under), 1.0e-9);
    }

    @Test
    public void previewRejectsRemovalThatMakesAnUnderestimatedStationWorse() {
        Fixture fixture = fixture();
        RouteImpact oldImpact = RouteImpact.from(Map.of(under, 1));
        RouteImpact candidateImpact = RouteImpact.empty();
        Plan candidate = fixture.scenario.getPopulation().getFactory().createPlan();
        when(fixture.extractor.extract(fixture.person)).thenReturn(oldImpact);
        when(fixture.extractor.extract(candidate)).thenReturn(candidateImpact);
        fixture.tracker.refresh();

        Comparison comparison = fixture.tracker.previewUpdate(fixture.person, candidate);

        assertFalse(comparison.improves());
        assertEquals(1, comparison.candidate().stations().size());
        assertEquals(78.0, comparison.candidate().stations().getFirst().simulatedFlow(), 1.0e-9);
        assertEquals(79.0, fixture.tracker.currentFlow(under), 1.0e-9);
    }

    @Test
    public void committedFreightMoveIsUsedAsBaselineForTheNextPreview() {
        Fixture fixture = fixture();
        RouteImpact oldImpact = RouteImpact.from(Map.of(over, 1));
        RouteImpact relocatedImpact = RouteImpact.from(Map.of(under, 1));
        Plan reverseCandidate = fixture.scenario.getPopulation().getFactory().createPlan();
        when(fixture.extractor.extract(fixture.person))
                .thenReturn(oldImpact, relocatedImpact, relocatedImpact);
        when(fixture.extractor.extract(reverseCandidate)).thenReturn(oldImpact);
        fixture.tracker.refresh();

        fixture.tracker.update(fixture.person);
        Comparison comparison = fixture.tracker.previewUpdate(fixture.person, reverseCandidate);

        assertEquals(120.0, fixture.tracker.currentFlow(over), 1.0e-9);
        assertEquals(80.0, fixture.tracker.currentFlow(under), 1.0e-9);
        assertFalse(comparison.improves());
    }

    @Test
    public void crossBorderShareUsesPassagesFromAllAgents() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Person crossBorder = addPerson(scenario, "crossborder", true);
        Person regular = addPerson(scenario, "regular", false);
        CountsProcessor counts = mock(CountsProcessor.class);
        when(counts.linkIds()).thenReturn(Set.of(over));
        when(counts.getLinkCounts(over)).thenReturn(100.0f);
        FlowProcessor flows = mock(FlowProcessor.class);
        when(flows.getTotalLinkFlow(over)).thenReturn(100.0);
        when(flows.getFlowContributionPerPassage(over)).thenReturn(1.0);
        RouteImpact.Extractor extractor = mock(RouteImpact.Extractor.class);
        RouteImpact onePassage = RouteImpact.from(Map.of(over, 1));
        when(extractor.extract(crossBorder)).thenReturn(onePassage);
        when(extractor.extract(regular)).thenReturn(onePassage);
        TrafficScoringTracker tracker = new TrafficScoringTracker(
                scenario.getPopulation(), counts, flows, extractor,
                1.0, 0.10, 0.10);

        tracker.refresh();

        assertEquals(0.5, tracker.crossBorderShare(over), 1.0e-12);
    }

    private Fixture fixture() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("freight"));
        person.getAttributes().putAttribute("isFreight", true);
        Plan plan = scenario.getPopulation().getFactory().createPlan();
        person.addPlan(plan);
        person.setSelectedPlan(plan);
        scenario.getPopulation().addPerson(person);

        CountsProcessor counts = mock(CountsProcessor.class);
        when(counts.linkIds()).thenReturn(Set.of(over, under));
        when(counts.getLinkCounts(over)).thenReturn(100.0f);
        when(counts.getLinkCounts(under)).thenReturn(100.0f);
        FlowProcessor flows = mock(FlowProcessor.class);
        when(flows.getTotalLinkFlow(over)).thenReturn(121.0);
        when(flows.getTotalLinkFlow(under)).thenReturn(79.0);
        when(flows.getFlowContributionPerPassage(over)).thenReturn(1.0);
        when(flows.getFlowContributionPerPassage(under)).thenReturn(1.0);
        RouteImpact.Extractor extractor = mock(RouteImpact.Extractor.class);
        TrafficScoringTracker tracker = new TrafficScoringTracker(
                scenario.getPopulation(), counts, flows, extractor, 1.0, 0.20, 0.20);
        return new Fixture(scenario, person, plan, extractor, tracker);
    }

    private static Person addPerson(Scenario scenario, String id, boolean crossBorder) {
        Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(id));
        if (crossBorder) person.getAttributes().putAttribute("isCrossBorder", true);
        Plan plan = scenario.getPopulation().getFactory().createPlan();
        person.addPlan(plan);
        person.setSelectedPlan(plan);
        scenario.getPopulation().addPerson(person);
        return person;
    }

    private record Fixture(Scenario scenario, Person person, Plan plan,
                           RouteImpact.Extractor extractor, TrafficScoringTracker tracker) { }
}
