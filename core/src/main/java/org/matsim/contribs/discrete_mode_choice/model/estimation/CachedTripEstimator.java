/* Adapted from MATSim's CachedTripEstimator. Distributed under the GNU GPL v2
 * or later; see /LICENSE. Eqasim: identity keys, person-bounded lifetime and
 * primitive departure-time keys. Keep this override compatible with MATSim 2026.
 */
package org.matsim.contribs.discrete_mode_choice.model.estimation;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/** Worker-local cache. Trip equality is identity; the upstream trip hash is constant. */
public class CachedTripEstimator implements TripEstimator {
    private final Map<String, IdentityHashMap<DiscreteModeChoiceTrip, Long2ObjectOpenHashMap<TripCandidate>>> cache = new HashMap<>();
    private final TripEstimator delegate;
    private Person currentPerson;

    public CachedTripEstimator(TripEstimator delegate, Collection<String> cachedModes) {
        this.delegate = delegate;
        for (String mode : cachedModes) cache.put(mode, new IdentityHashMap<>());
    }

    @Override
    public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
            List<TripCandidate> previousCandidates) {
        if (person != currentPerson) {
            for (var trips : cache.values()) trips.clear();
            currentPerson = person;
        }
        var trips = cache.get(mode);
        if (trips == null) return delegate.estimateTrip(person, mode, trip, previousCandidates);
        var departures = trips.computeIfAbsent(trip, ignored -> new Long2ObjectOpenHashMap<>(2));
        long departure = (long) Math.ceil(trip.getDepartureTime());
        TripCandidate candidate = departures.get(departure);
        if (candidate == null) {
            candidate = delegate.estimateTrip(person, mode, trip, previousCandidates);
            departures.put(departure, candidate);
        }
        return candidate;
    }
}
