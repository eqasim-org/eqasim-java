package org.eqasim.core.simulation.modes.feeder_drt.mode_choice.constraints;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FeederDrtConstraint implements TripConstraint {
	public final static String NAME = "FeederConstraint";

	private final Map<String, String> ptModes;
	private final Map<String, String> drtModes;
	public FeederDrtConstraint(Map<String, String> ptModes, Map<String, String> drtModes) {
		this.ptModes = ptModes;
		this.drtModes = drtModes;
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		return true;
	}

	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		if (this.ptModes.containsKey(candidate.getMode())) {
			RoutedTripCandidate routedTripCandidate = (RoutedTripCandidate) candidate;
			List<? extends PlanElement> elements = routedTripCandidate.getRoutedPlanElements();

			boolean foundPt = false;
			boolean foundDrt = false;

			for (PlanElement element : elements) {
				if (element instanceof Leg leg) {
					if (leg.getMode().equals(this.ptModes.get(candidate.getMode()))) {
						foundPt = true;
					} else if(leg.getMode().equals(this.drtModes.get(candidate.getMode()))) {
						foundDrt = true;
					}
					if(foundDrt && foundPt) {
						break;
					}
				}
			}
			return foundPt && foundDrt;
		}
		return true;
	}

	static public class Factory implements TripConstraintFactory {

		private final Map<String, String> ptModes;
		private final Map<String, String> drtModes;

		public Factory(Map<String, String> ptModes, Map<String, String> drtModes) {
			this.ptModes = ptModes;
			this.drtModes = drtModes;
		}

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new FeederDrtConstraint(this.ptModes, this.drtModes);
		}
	}
}
