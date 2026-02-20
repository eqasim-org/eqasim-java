package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import com.google.common.base.Verify;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SwissCarPredictor extends CarPredictor {
    private final CostModel costModel;

    @Inject
    public SwissCarPredictor(ModeParameters parameters, @Named("car") CostModel costModel) {
        super(parameters, costModel);
        this.costModel = costModel;
    }

    @Override
    public CarVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double carTravelTime_min = 0.0;
        double accessEgressTime_min = 0.0;

        boolean foundCar = false;

        for (Leg leg : TripStructureUtils.getLegs(elements)) {
            if (leg.getMode().equals(TransportMode.car)) {
                Verify.verify(!foundCar);
                carTravelTime_min += leg.getTravelTime().seconds() / 60.0;
            } else if (leg.getMode().equals(TransportMode.walk)) {
                accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
            } else {
                throw new IllegalStateException("Unexpected mode in car chain: " + leg.getMode());
            }
        }

        double cost_MU = costModel.calculateCost_MU(person, trip, elements);
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        return new CarVariables(carTravelTime_min, cost_MU, euclideanDistance_km, accessEgressTime_min);
    }
}
