package org.matsim.contribs.discrete_mode_choice.model.estimation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * This trip estimator wraps another TripEstimator for which it caches results.
 * One can configure which modes should be cached. This way, for instance when a
 * trip would be estimates many times, e.g. in a tour-based model, one makes
 * sure that this only happens once.
 *
 * Local compatibility override for MATSim 2026.0-2025w33. Each estimator is
 * confined to one replanning worker, which processes one person's plan at a
 * time. Cache only that person's evaluation: trip keys use reference equality,
 * so keeping earlier people's alternatives cannot provide cross-person hits.
 * Recheck this override and the worker lifecycle when upgrading MATSim.
 *
 * @author sebhoerl
 */
public class CachedTripEstimator implements TripEstimator {
	final private Map<String, Map<DiscreteModeChoiceTrip, TripCandidate>> cache = new HashMap<>();
	final private TripEstimator delegate;
	private Person currentPerson;

	public CachedTripEstimator(TripEstimator delegate, Collection<String> cachedModes) {
		this.delegate = delegate;

		for (String mode : cachedModes) {
			cache.put(mode, new HashMap<>());
		}
	}

	@Override
	public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> preceedingTrips) {
		if (person != currentPerson) {
			// Keep reuse across all trips/mode alternatives for the current
			// person, but release earlier alternatives before any next-person
			// request, including one for an uncached mode. Clearing references
			// never mutates candidates already returned to the chosen plan.
			for (Map<DiscreteModeChoiceTrip, TripCandidate> modeCache : cache.values()) {
				modeCache.clear();
			}
			currentPerson = person;
		}
		Map<DiscreteModeChoiceTrip, TripCandidate> modeCache = cache.get(mode);

		if (modeCache != null) {
			TripCandidate candidate = modeCache.get(trip);

			if (candidate == null) {
				candidate = delegate.estimateTrip(person, mode, trip, preceedingTrips);
				modeCache.put(trip, candidate);
			}

			return candidate;
		} else {
			return delegate.estimateTrip(person, mode, trip, preceedingTrips);
		}
	}
}
