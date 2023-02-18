package org.eqasim.core.simulation.mode_choice.epsilon;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class UniformEpsilonProvider extends AbstractEpsilonProvider {
	public UniformEpsilonProvider(long randomSeed) {
		super(randomSeed);
	}

	@Override
	public double getEpsilon(Id<Person> personId, int tripIndex, String mode) {
		return getUniformEpsilon(personId, tripIndex, mode);
	}
}
