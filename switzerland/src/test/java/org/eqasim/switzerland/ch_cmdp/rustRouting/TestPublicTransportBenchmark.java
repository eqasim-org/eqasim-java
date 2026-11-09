// Checks deterministic request replay, population sampling, and comparison statistics including missing routes.
package org.eqasim.switzerland.ch_cmdp.rustRouting;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.rules.TemporaryFolder;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.api.core.v01.*;
import java.util.*;

public class TestPublicTransportBenchmark {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    @Test public void savedRequestsPreserveFractionalDeparturesAndValidateCounts() throws Exception {
        var file=temporary.newFile().toPath();
        PtBenchmarkRequests.write(file,List.of(new PtBenchmarkRequests.Trip(new double[]{1,2},new double[]{3,4},21600.125)));
        var restored=PtBenchmarkRequests.read(file,1).getFirst();
        assertEquals(21600.125,restored.departure(),0);assertArrayEquals(new double[]{1,2},restored.origin(),0);
        assertThrows(IllegalArgumentException.class,()->PtBenchmarkRequests.read(file,2));
        assertThrows(IllegalArgumentException.class,()->new PtBenchmarkRequests.Trip(new double[]{Double.NaN,2},new double[]{3,4},0));
    }
    @Test public void mediansHandleEvenAndOddNumbersOfPasses() {
        assertEquals(2,PtBenchmarkStatistics.median(new double[]{1,3}),0);
        assertEquals(3,PtBenchmarkStatistics.median(new double[]{1,4,3}),0);
    }
    @Test public void reservoirsAreReproducibleAndBounded() {
        var a=new PtBenchmarkRequests.Reservoir(5,6012);var b=new PtBenchmarkRequests.Reservoir(5,6012);
        for(int i=0;i<200;i++) {
            var trip=new PtBenchmarkRequests.Trip(new double[]{i,0},new double[]{1000,0},21600);
            a.accept(trip);b.accept(trip);
        }
        assertEquals(200,a.eligible);assertEquals(5,a.trips.size());
        assertEquals(a.trips.stream().map(q->q.origin()[0]).toList(),b.trips.stream().map(q->q.origin()[0]).toList());
    }
    @Test public void samplingStreamsSelectedPlansAndExcludesFreightAndZeroDistance() throws Exception {
        var scenario=ScenarioUtils.createScenario(ConfigUtils.createConfig());var population=scenario.getPopulation();
        for(int i=0;i<12;i++) {
            var person=population.getFactory().createPerson(Id.createPersonId(i));var plan=PopulationUtils.createPlan();
            var from=PopulationUtils.createActivityFromCoord("home",new Coord(i*10,0));from.setEndTime(21600);
            var leg=PopulationUtils.createLeg("walk");leg.setDepartureTime(21600);leg.setTravelTime(600);
            plan.addActivity(from);plan.addLeg(leg);
            plan.addActivity(PopulationUtils.createActivityFromCoord("work",i==11?from.getCoord():new Coord(3000,0)));
            person.addPlan(plan);if(i==10)person.getAttributes().putAttribute("isFreight",1);
            population.addPerson(person);
        }
        var file=temporary.newFile("plans.xml.gz").toPath();
        new org.matsim.api.core.v01.population.PopulationWriter(population).write(file.toString());
        var target=ScenarioUtils.createScenario(ConfigUtils.createConfig());target.getConfig().plans().setInputFile(file.toString());
        assertEquals(10,PtBenchmarkRequests.sample(target,10,6012).size());
        assertTrue(target.getPopulation().getPersons().isEmpty());
    }
    @Test public void similarityUsesToleranceAndKeepsErrorsSeparateFromMissingTransit() {
        var a=new PtBenchmarkStatistics.Result(true,1000,100,5000,1,2,List.of("line"),null,1,false);
        var b=new PtBenchmarkStatistics.Result(true,1040,100,5200,2,3,List.of("other"),null,2,true);
        assertEquals(1L,PtBenchmarkStatistics.summarize(List.of(a,b),List.of(1.)).get("budget_limited_routes"));
        var missing=PtBenchmarkStatistics.describe(null,0,1,null);
        var error=PtBenchmarkStatistics.describe(null,0,1,"failure");
        var report=PtBenchmarkStatistics.compare(List.of(a,missing,error),List.of(b,missing,missing),60,5);
        assertEquals(1,report.get("pt_in_both"));assertEquals(1,report.get("no_pt_in_both"));
        assertEquals(1,report.get("similar_travel_times"));assertEquals(1,report.get("requests_with_errors"));
        assertEquals(40.0,(Double)report.get("mean_signed_time_difference_seconds"),0);
        assertTrue(Double.isNaN(missing.seconds()));
    }
}
