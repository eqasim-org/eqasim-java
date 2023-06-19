package org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic;

import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictHandler;
import org.matsim.api.core.v01.population.Population;

public class NoopConflictLogic implements ConflictLogic {
	@Override
	public void run(Population population, ConflictHandler handler) {

	}
}
