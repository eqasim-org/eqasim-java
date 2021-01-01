package org.eqasim.ile_de_france.mode_choice.epsilon;

import java.util.Optional;
import java.util.Random;

import ch.ethz.matsim.discrete_mode_choice.model.tour_based.TourCandidate;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import ch.ethz.matsim.discrete_mode_choice.model.utilities.UtilitySelector;
import ch.ethz.matsim.discrete_mode_choice.model.utilities.UtilitySelectorFactory;

public class EpsilonSelector implements UtilitySelector<TourCandidate> {
	static public final String NAME = "EpsilonSelectpr";
	
	private final EpsilonProvider epsilonProvider;

	private double maximumUtility = Double.NEGATIVE_INFINITY;
	private TourCandidate maximumCandidate = null;

	public EpsilonSelector(EpsilonProvider epsilonProvider) {
		this.epsilonProvider = epsilonProvider;
	}

	@Override
	public void addCandidate(TourCandidate candidate) {
		double epsilon = 0.0;

		for (TripCandidate trip : candidate.getTripCandidates()) {
			epsilon += epsilonProvider.getEpsilon(trip.hashCode());
		}

		double utility = candidate.getUtility() + epsilon;

		if (utility > maximumUtility) {
			maximumUtility = utility;
			maximumCandidate = candidate;
		}
	}

	@Override
	public Optional<TourCandidate> select(Random random) {
		return Optional.ofNullable(maximumCandidate);
	}

	static public class Factory implements UtilitySelectorFactory<TourCandidate> {
		private final EpsilonProvider epsilonProvider;

		public Factory(EpsilonProvider epsilonProvider) {
			this.epsilonProvider = epsilonProvider;
		}

		@Override
		public UtilitySelector<TourCandidate> createUtilitySelector() {
			return new EpsilonSelector(epsilonProvider);
		}
	}
}
