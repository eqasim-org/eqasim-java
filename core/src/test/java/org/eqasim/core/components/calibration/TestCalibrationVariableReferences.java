package org.eqasim.core.components.calibration;

import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import org.eqasim.core.components.calibration.writer.CalibrationVariableReferences;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.population.PopulationUtils;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.estimation.CachedTripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.DefaultTourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityWriter;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class TestCalibrationVariableReferences {
    @Test public void distinguishesEqualUtilitiesAtDifferentTimesAndRetainsCacheHitReference() throws Exception {
        var f = PopulationUtils.getFactory();
        var person = f.createPerson(Id.createPersonId("00;agent\"1"));
        var trip = new DiscreteModeChoiceTrip(f.createActivityFromCoord("home", new Coord(0,0)),
                f.createActivityFromCoord("work", new Coord(1,0)), "pt", List.of(), 0,0,0,new AttributesImpl());
        var cache = new CachedTripEstimator((p,m,t,previous) -> {
            CalibrationVariableReferences.record();
            return new DefaultTripCandidate(-0.123456789, m, 60);
        },List.of("pt"));
        CalibrationVariableReferences.start();
        Locale before = Locale.getDefault();
        try {
            trip.setDepartureTime(3600);
            var a = cache.estimateTrip(person,"pt",trip,List.of());
            long refA = CalibrationVariableReferences.get(a);
            trip.setDepartureTime(7200);
            var b = cache.estimateTrip(person,"pt",trip,List.of());
            long refB = CalibrationVariableReferences.get(b);
            assertTrue(refA > 0); assertTrue(refB > refA);
            trip.setDepartureTime(3600);
            assertSame(a,cache.estimateTrip(person,"pt",trip,List.of()));
            assertEquals(refA,CalibrationVariableReferences.get(a));
            Locale.setDefault(Locale.GERMANY);
            Path file = Files.createTempFile("candidate-reference", ".csv");
            UtilityWriter.init(file.toString());
            UtilityWriter.writeCandidate(person.getId(),List.of(trip),List.of(
                    new DefaultTourCandidate(a.getUtility(),List.of(a)),new DefaultTourCandidate(b.getUtility(),List.of(b))),0);
            UtilityWriter.close();
            var lines = Files.readAllLines(file);
            assertTrue(lines.getFirst().endsWith(";variables_id"));
            assertTrue(lines.get(1).endsWith(";"+refA));
            assertTrue(lines.get(2).endsWith(";"+refB));
            assertTrue(lines.get(1).contains("-0.123456789"));
            assertTrue(lines.get(1).startsWith("\"00;agent\"\"1\";"));
            CalibrationVariableReferences.beginPerson(f.createPerson(Id.createPersonId("other")));
            assertEquals(-1,CalibrationVariableReferences.get(a));
        } finally {
            UtilityWriter.close();
            CalibrationVariableReferences.stop();
            Locale.setDefault(before);
        }
    }
}
