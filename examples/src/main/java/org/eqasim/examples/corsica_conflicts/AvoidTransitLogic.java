package org.eqasim.examples.corsica_conflicts;

import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictHandler;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;

public class AvoidTransitLogic implements ConflictLogic {
	@Override
	public void run(Population population, ConflictHandler handler) {
		for (Person person : population.getPersons().values()) {
			for (Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan())) {
				if (TripStructureUtils.getRoutingMode(leg).equals("pt")) {
					handler.addRejection(person.getId(), "pt");
				}
			}
		}
	}
}
