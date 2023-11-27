package org.eqasim.ile_de_france.mode_choice.costs;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class IDFDrtCostModel extends AbstractCostModel {
    public IDFDrtCostModel(String mode) {
        super(mode);
    }

    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        return 0;
    }
}
