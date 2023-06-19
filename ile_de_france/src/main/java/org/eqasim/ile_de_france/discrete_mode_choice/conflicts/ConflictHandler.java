package org.eqasim.ile_de_france.discrete_mode_choice.conflicts;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;

public class ConflictHandler {
	private final IdMap<Person, Set<ConflictItem>> items;

	public ConflictHandler(IdMap<Person, Set<ConflictItem>> items) {
		this.items = items;
	}

	public void addRejection(Id<Person> personId, int tripIndex, String mode) {
		items.computeIfAbsent(personId, id -> new HashSet<>()).add(new ConflictItem(tripIndex, mode));
	}

	public void addRejection(Id<Person> personId, String mode) {
		items.computeIfAbsent(personId, id -> new HashSet<>()).add(new ConflictItem(-1, mode));
	}
}
