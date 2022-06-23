package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussPTCostModel extends AbstractCostModel {
    public final SMMCostParameters parameters;
    @Inject
    public KraussPTCostModel(SMMCostParameters parameters) {
        super("pt");
        this.parameters = parameters;
    }


    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        return (parameters.pTTicketCost);
    }
}
