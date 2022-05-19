package org.eqasim.examples.corsica_drt.mode_choice.cost;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.parameters.KraussCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussPTCostModel extends AbstractCostModel {
    public final KraussCostParameters parameters;
    @Inject
    public KraussPTCostModel( KraussCostParameters parameters) {
        super("pt");
        this.parameters = parameters;
    }


    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        return (parameters.pTTicketCost);
    }
}
