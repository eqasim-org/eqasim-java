package org.eqasim.switzerland.ch_cmdp.mode_choice.costs;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class SwissWeissCarCostModel extends AbstractCostModel {
    private final SwissCostParameters parameters;

    @Inject
    public SwissWeissCarCostModel(SwissCostParameters costParameters) {
        super("car");
        this.parameters = costParameters;
    }

    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double inVehicleDistance_km = getInVehicleDistance_km(elements);
        double carCost_CHF_km = Math.min(0.104 + 0.6 * Math.exp(-1.2*Math.pow(inVehicleDistance_km,0.33)),
                                         0.3);
        return carCost_CHF_km * inVehicleDistance_km;
    }
}
