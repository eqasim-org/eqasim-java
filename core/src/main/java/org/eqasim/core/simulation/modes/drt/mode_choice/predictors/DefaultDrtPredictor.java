package org.eqasim.core.simulation.modes.drt.mode_choice.predictors;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.modes.drt.mode_choice.variables.DrtVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;
import java.util.Map;

public class DefaultDrtPredictor implements DrtPredictor {
    private Map<String, CostModel> costModels;

    public DefaultDrtPredictor(Map<String, CostModel> costModels) {
        this.costModels = costModels;
    }


    @Override
    public DrtVariables predictVariables(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double travelTime_min = 0.0;
        double accessEgressTime_min = 0.0;
        double cost_MU = 0.0;
        double waitingTime_min = 0.0;

        for (Leg leg : TripStructureUtils.getLegs(elements)) {
            if (costModels.containsKey(leg.getMode())) {
                DrtRoute route = (DrtRoute) leg.getRoute();

                // We use worst case here
                travelTime_min = route.getMaxTravelTime() / 60.0;
                waitingTime_min = route.getMaxWaitTime() / 60.0;

                cost_MU = costModels.get(leg.getMode()).calculateCost_MU(person, trip, elements);

            } else if (leg.getMode().equals(TransportMode.walk)) {
                accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
            } else {
                throw new IllegalStateException("Encountered unknown mode in DrtPredictor: " + leg.getMode());
            }
        }

        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        return new DrtVariables(travelTime_min, cost_MU, euclideanDistance_km, waitingTime_min, accessEgressTime_min);
    }
}
