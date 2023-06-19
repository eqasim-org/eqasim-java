package org.eqasim.ile_de_france.discrete_mode_choice.conflicts;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;

public class ConflictConstraint extends AbstractTripConstraint {
	static public final String NAME = "conflict";

	private final Set<ConflictItem> items;

	ConflictConstraint(Set<ConflictItem> items) {
		this.items = items;
	}

	@Override
	public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
		for (ConflictItem item : items) {
			if ((item.tripIndex == trip.getIndex() || item.tripIndex == -1) && mode.equals(item.mode)) {
				return false;
			}
		}

		return true;
	}

	@Singleton
	public static class ConflictConstraintFactory implements TripConstraintFactory {
		private IdMap<Person, Set<ConflictItem>> items = new IdMap<>(Person.class);

		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
				Collection<String> availableModes) {
			return new ConflictConstraint(items.getOrDefault(person.getId(), Collections.emptySet()));
		}

		public void setItems(IdMap<Person, Set<ConflictItem>> items) {
			this.items = items;
		}
	}
}
