package org.eqasim.ile_de_france.mode_choice.costs;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.List;

public class IDFFeederDrtCostModel extends AbstractCostModel {

    private final IDFCostParameters costParameters;

    @Inject
    public IDFFeederDrtCostModel(IDFCostParameters costParameters) {
        super("feederDrt");
        this.costParameters = costParameters;
    }

    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double distance = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord()) / 1000;
        return this.costParameters.feederDrtCost_EUR_access + this.costParameters.drtCost_EUR_km * distance;
    }
}
