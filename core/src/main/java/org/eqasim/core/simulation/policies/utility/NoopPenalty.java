package org.eqasim.core.simulation.policies.utility;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class NoopPenalty implements UtilityPenalty {
    static public NoopPenalty INSTANCE = new NoopPenalty();

    @Override
    public double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
            List<? extends PlanElement> elements) {
        return 0.0;
    }
}
