package org.eqasim.mode_choice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.estimation.CachedTripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultTripCandidate;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class TestDiscreteModeChoiceTripCache {
	private DiscreteModeChoiceTrip trip(int personHash, int tripHash) {
		var trip = new DiscreteModeChoiceTrip(null, null, "car", List.of(), personHash, tripHash, tripHash,
				new AttributesImpl());
		// MATSim 2026 caches alternatives separately by their departure time.
		trip.setDepartureTime(0.0);
		return trip;
	}

	@Test
	public void testTripHashesDistributeAcrossPeopleAndTrips() {
		Set<Integer> hashes = new HashSet<>();
		for (int person = 0; person < 1000; person++) {
			for (int index = 0; index < 10; index++) {
				hashes.add(trip(person, index).hashCode());
			}
		}
		// Guard against the dependency's constant hash without prescribing a
		// particular hash formula or requiring collisions to be impossible.
		assertTrue("Distinct trips must not collapse into the same cache bucket", hashes.size() > 9000);
	}

	@Test
	public void testCachedResultsRemainSpecificToTripIdentityAndMode() {
		AtomicInteger calls = new AtomicInteger();
		CachedTripEstimator cache = new CachedTripEstimator((person, mode, trip, previous) ->
				new DefaultTripCandidate(calls.incrementAndGet(), mode, 100.), List.of("car", "bike"));
		DiscreteModeChoiceTrip first = trip(42, 0);
		DiscreteModeChoiceTrip distinct = trip(42, 0);
		assertNotSame(first, distinct);
		assertTrue("Trip equality remains reference-based", !first.equals(distinct));

		var car = cache.estimateTrip(null, "car", first, List.of());
		assertSame(car, cache.estimateTrip(null, "car", first, List.of()));
		assertEquals(1, calls.get());
		var otherTrip = cache.estimateTrip(null, "car", distinct, List.of());
		assertNotSame(car, otherTrip);
		assertSame(otherTrip, cache.estimateTrip(null, "car", distinct, List.of()));
		var bike = cache.estimateTrip(null, "bike", first, List.of());
		assertNotSame(car, bike);
		assertSame(bike, cache.estimateTrip(null, "bike", first, List.of()));
		assertEquals(3, calls.get());
	}

	@Test
	public void testUncachedModesStillDelegateEveryTime() {
		AtomicInteger calls = new AtomicInteger();
		CachedTripEstimator cache = new CachedTripEstimator((person, mode, trip, previous) ->
				new DefaultTripCandidate(calls.incrementAndGet(), mode, 100.), List.of("car"));
		DiscreteModeChoiceTrip trip = trip(42, 0);
		var first = cache.estimateTrip(null, "walk", trip, List.of());
		assertNotSame(first, cache.estimateTrip(null, "walk", trip, List.of()));
		assertEquals(2, calls.get());
	}

	private Person person(String id) {
		return PopulationUtils.getFactory().createPerson(Id.createPersonId(id));
	}

	@Test
	public void testPersonTransitionEvictsAllModesEvenOnAnUncachedRequest() {
		AtomicInteger calls = new AtomicInteger();
		CachedTripEstimator cache = new CachedTripEstimator((person, mode, trip, previous) ->
				new DefaultTripCandidate(calls.incrementAndGet(), mode, 100.), List.of("car", "bike"));
		Person firstPerson = person("first");
		Person nextPerson = person("next");
		DiscreteModeChoiceTrip trip = trip(42, 0);
		var car = cache.estimateTrip(firstPerson, "car", trip, List.of());
		var bike = cache.estimateTrip(firstPerson, "bike", trip, List.of());
		assertSame(car, cache.estimateTrip(firstPerson, "car", trip, List.of()));
		assertSame(bike, cache.estimateTrip(firstPerson, "bike", trip, List.of()));
		assertEquals(2, calls.get());

		// An uncached first request must still release the previous person's
		// car AND bike alternatives, rather than waiting for a cached mode.
		cache.estimateTrip(nextPerson, "walk", trip(43, 0), List.of());
		assertEquals(3, calls.get());
		var carAgain = cache.estimateTrip(firstPerson, "car", trip, List.of());
		var bikeAgain = cache.estimateTrip(firstPerson, "bike", trip, List.of());
		assertNotSame(car, carAgain);
		assertNotSame(bike, bikeAgain);
		assertSame(carAgain, cache.estimateTrip(firstPerson, "car", trip, List.of()));
		assertEquals(5, calls.get());
		// Eviction releases map references; previously returned choices remain
		// usable by the model when inserting selected routes into the plan.
		assertEquals("car", car.getMode());
		assertEquals(100., car.getDuration(), 0.);
	}

	@Test
	public void testPersonIdentityIsNotConfusedWithARepeatedIdentifier() {
		AtomicInteger calls = new AtomicInteger();
		CachedTripEstimator cache = new CachedTripEstimator((person, mode, trip, previous) ->
				new DefaultTripCandidate(calls.incrementAndGet(), mode, 100.), List.of("car"));
		Person first = person("same_id");
		Person replacement = person("same_id");
		DiscreteModeChoiceTrip trip = trip(42, 0);
		var before = cache.estimateTrip(first, "car", trip, List.of());
		var after = cache.estimateTrip(replacement, "car", trip, List.of());
		assertNotSame(before, after);
		assertSame(after, cache.estimateTrip(replacement, "car", trip, List.of()));
		assertEquals(2, calls.get());
	}

	@Test
	public void testWorkersKeepIndependentPersonCaches() {
		AtomicInteger calls = new AtomicInteger();
		TripEstimator delegate = (person, mode, trip, previous) -> new DefaultTripCandidate(calls.incrementAndGet(), mode, 100.);
		CachedTripEstimator firstWorker = new CachedTripEstimator(delegate, List.of("car"));
		CachedTripEstimator otherWorker = new CachedTripEstimator(delegate, List.of("car"));
		Person first = person("first");
		Person other = person("other");
		DiscreteModeChoiceTrip trip = trip(42, 0);
		var firstChoice = firstWorker.estimateTrip(first, "car", trip, List.of());
		otherWorker.estimateTrip(other, "car", trip, List.of());
		assertSame(firstChoice, firstWorker.estimateTrip(first, "car", trip, List.of()));
		assertEquals(2, calls.get());
	}
}
