package org.eqasim.core.simulation.mode_choice.epsilon;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class NoopEpsilonProvider implements EpsilonProvider {
    final static public NoopEpsilonProvider INSTANCE = new NoopEpsilonProvider();

    @Override
    public double getEpsilon(Id<Person> personId, int tripIndex, String mode) {
        return 0.0;
    }
}
