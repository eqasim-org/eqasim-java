package org.eqasim.core.simulation.mode_choice.epsilon;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class GumbelEpsilonProvider extends AbstractEpsilonProvider {
	private final double scale;

	public GumbelEpsilonProvider(long randomSeed, double scale) {
		super(randomSeed);
		this.scale = scale;
	}

	@Override
	public double getEpsilon(Id<Person> personId, int tripIndex, String mode) {
		double u = getUniformEpsilon(personId, tripIndex, mode);
		return -scale * Math.log(-Math.log(u));
	}
}
