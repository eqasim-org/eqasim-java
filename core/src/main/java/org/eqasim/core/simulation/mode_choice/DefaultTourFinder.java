package org.eqasim.core.simulation.mode_choice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ethz.matsim.discrete_mode_choice.components.tour_finder.TourFinder;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class DefaultTourFinder implements TourFinder {
	private final String HOME_ACTIVITY_TYPE = "home";
	private final String OUTSIDE_ACTIVITY_TYPE = "outside";

	@Override
	public List<List<DiscreteModeChoiceTrip>> findTours(List<DiscreteModeChoiceTrip> trips) {
		Set<Integer> relevantActivityIndices = new HashSet<>();

		for (int index = 0; index < trips.size(); index++) {
			DiscreteModeChoiceTrip trip = trips.get(index);

			if (trip.getOriginActivity().getType().equals(HOME_ACTIVITY_TYPE)
					|| trip.getOriginActivity().getType().equals(OUTSIDE_ACTIVITY_TYPE)) {
				relevantActivityIndices.add(index);
			}

			if (trip.getDestinationActivity().getType().equals(HOME_ACTIVITY_TYPE)
					|| trip.getDestinationActivity().getType().equals(OUTSIDE_ACTIVITY_TYPE)) {
				relevantActivityIndices.add(index + 1);
			}
		}

		List<Integer> orderedActivityIndices = new ArrayList<>(relevantActivityIndices);
		Collections.sort(orderedActivityIndices);

		List<List<DiscreteModeChoiceTrip>> tours = new LinkedList<>();

		int currentActivityIndex = 0;

		while (orderedActivityIndices.size() > 0) {
			int nextActivityIndex = orderedActivityIndices.remove(0);

			tours.add(trips.subList(currentActivityIndex, nextActivityIndex));
			currentActivityIndex = nextActivityIndex;
		}

		if (currentActivityIndex != trips.size()) {
			tours.add(trips.subList(currentActivityIndex, trips.size()));
		}

		tours = tours.stream().filter(t -> t.size() > 0).collect(Collectors.toList());

		return tours;
	}
}
