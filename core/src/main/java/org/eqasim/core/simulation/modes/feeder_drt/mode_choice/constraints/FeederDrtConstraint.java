package org.eqasim.core.simulation.modes.feeder_drt.mode_choice.constraints;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import java.util.*;
import java.util.stream.Collectors;

public class FeederDrtConstraint implements TripConstraint {
	public final static String NAME = "FeederConstraint";
	private final Map<String, String> ptModes;
	private final Map<String, String> drtModes;
	private final Map<String, List<String>> feedersUsingPtModes;
	public FeederDrtConstraint(Map<String, String> ptModes, Map<String, String> drtModes) {
		this.ptModes = ptModes;
		this.drtModes = drtModes;
		this.feedersUsingPtModes = this.ptModes.keySet()
				.stream()
				.collect(Collectors.toMap(this.ptModes::get, feederMode -> {
					List<String> modeList = new ArrayList<>();
					modeList.add(feederMode);
					return modeList;
				}, (rightList, leftList) -> {
					leftList.addAll(rightList);
					return leftList;
				}));
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		int numberOfOutsideBorders = (trip.getOriginActivity().getType().equals("outside") ? 1 : 0) + ((trip.getDestinationActivity().getType().equals("outside")) ? 1 : 0);
		if(numberOfOutsideBorders == 1) {
			// If only one of the previous or next activity is an outside activity, we ensure that only transitions between PT and Feeder are possible.
			if(trip.getInitialMode().equals(mode)) {
				// If the mode doesn't change, it's ok
				return true;
			}
			if(this.feedersUsingPtModes.containsKey(trip.getInitialMode())) {
				// If the initial mode is a pt mode, and the next mode is different, we make sure the next mode is a feeder one.
				return this.feedersUsingPtModes.get(trip.getInitialMode()).contains(mode);
			} else if (this.ptModes.containsKey(trip.getInitialMode())) {
				// if the initial mode is a feeder one, we make sure the next one is a pt mode.
				return this.ptModes.get(trip.getInitialMode()).equals(mode);
			}
		} else if (numberOfOutsideBorders == 2) {
			return trip.getInitialMode().equals(mode);
		}
		return true;
	}

	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		boolean forbiddingAccessDrt = trip.getOriginActivity().getType().equals("outside");
		boolean forbiddingEgressDrt = trip.getDestinationActivity().getType().equals("outside");
		if (this.ptModes.containsKey(candidate.getMode())) {
			RoutedTripCandidate routedTripCandidate = (RoutedTripCandidate) candidate;
			List<? extends PlanElement> elements = routedTripCandidate.getRoutedPlanElements();

			boolean foundPt = false;
			boolean foundDrt = false;

			for (PlanElement element : elements) {
				if (element instanceof Leg leg) {
					if (leg.getMode().equals(this.ptModes.get(candidate.getMode()))) {
						// if, when finding a PT, a DRT leg has already been found, that means there's an access drt. So we check this here
						if(forbiddingAccessDrt && foundDrt) {
							return false;
						}
						foundPt = true;
					} else if(leg.getMode().equals(this.drtModes.get(candidate.getMode()))) {
						// if, when finding a DRT, a PT leg has already been found, that means this is an egress drt. So we check this here
						if(forbiddingEgressDrt && foundPt) {
							return false;
						}
						foundDrt = true;
					}
					if(foundDrt && foundPt && !forbiddingEgressDrt) {
						// If we already found pt and drt and we do not forbid drt for egress, the constraint cannot be broken by further examining the plan, we can just stop here
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
