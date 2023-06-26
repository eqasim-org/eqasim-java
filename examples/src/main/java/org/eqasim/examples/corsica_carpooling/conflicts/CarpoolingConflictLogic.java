package org.eqasim.examples.corsica_carpooling.conflicts;

import java.util.Random;

import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictHandler;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

public class CarpoolingConflictLogic implements ConflictLogic {
	private final Random random = new Random(0);

	@Override
	public void run(Population population, ConflictHandler handler) {
		for (Person person : population.getPersons().values()) {
			int tripIndex = 0;

			for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				String routingMode = TripStructureUtils.getRoutingMode(trip.getLegsOnly().get(0));

				if (routingMode.equals("carpooling")) {
					/*-
					 * This is a carpooling leg. Do we find a matching "car" leg from another person?
					 * If not, we can reject the plan and let the agent replan again.
					 */

					if (random.nextDouble() < 0.01) {
						// As an example, here we just reject 1% of carpooling trips
						handler.addRejection(person.getId(), tripIndex, routingMode);
					}
				}

				tripIndex++;
			}
		}
	}
}
