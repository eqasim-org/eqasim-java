package org.matsim.contribs.discrete_mode_choice.model.estimation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.population.PopulationUtils;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class TestCachedTripEstimator {
    @Test public void identityKeysStayDistinctAndReuseCandidatesDespiteCollidingTripHashes() {
        var calls = new AtomicInteger();
        var cache = new CachedTripEstimator((person, mode, trip, previous) -> {
            calls.incrementAndGet();
            return mock(TripCandidate.class);
        }, List.of("pt"));
        var person = PopulationUtils.getFactory().createPerson(Id.createPersonId("one"));
        var trips = new ArrayList<DiscreteModeChoiceTrip>();
        var candidates = new ArrayList<TripCandidate>();
        for (int i = 0; i < 1000; i++) {
            var trip = trip(i);
            trip.setDepartureTime(100.1);
            trips.add(trip);
            candidates.add(cache.estimateTrip(person, "pt", trip, List.of()));
        }
        for (int i = 0; i < trips.size(); i++) {
            trips.get(i).setDepartureTime(100.9);
            assertSame(candidates.get(i), cache.estimateTrip(person, "pt", trips.get(i), List.of()));
        }
        assertEquals(1000, calls.get());
        trips.getFirst().setDepartureTime(101.1);
        assertNotSame(candidates.getFirst(), cache.estimateTrip(person, "pt", trips.getFirst(), List.of()));
        assertEquals(1001, calls.get());
    }

    @Test public void switchingPersonEvictsOldCandidatesAndUncachedModesAlwaysDelegate() {
        var calls = new AtomicInteger();
        var cache = new CachedTripEstimator((person, mode, trip, previous) -> {
            calls.incrementAndGet();
            return mock(TripCandidate.class);
        }, List.of("pt"));
        var first = PopulationUtils.getFactory().createPerson(Id.createPersonId("first"));
        var second = PopulationUtils.getFactory().createPerson(Id.createPersonId("second"));
        var trip = trip(0);
        trip.setDepartureTime(100);
        var old = cache.estimateTrip(first, "pt", trip, List.of());
        assertSame(old, cache.estimateTrip(first, "pt", trip, List.of()));
        assertNotSame(old, cache.estimateTrip(second, "pt", trip, List.of()));
        assertNotSame(old, cache.estimateTrip(first, "pt", trip, List.of()));
        cache.estimateTrip(first, "bike", trip, List.of());
        cache.estimateTrip(first, "bike", trip, List.of());
        assertEquals(5, calls.get());
    }

    private static DiscreteModeChoiceTrip trip(int i) {
        var origin = PopulationUtils.createActivityFromCoord("home", new Coord(i, 0));
        var destination = PopulationUtils.createActivityFromCoord("work", new Coord(i + 1, 1));
        return new DiscreteModeChoiceTrip(origin, destination, "walk", List.of(), i, i + 1, i, new AttributesImpl());
    }
}
