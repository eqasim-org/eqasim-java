package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;


import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;


public class SwissBikePredictor extends BikePredictor {

    @Override
    public BikeVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double travelTime_min = 0.0;

        for (Leg leg : TripStructureUtils.getLegs(elements)) {
            if (leg.getMode().equals(TransportMode.bike)) {
                travelTime_min += leg.getTravelTime().seconds() / 60.0;
            } else {
                if (!leg.getMode().equals(TransportMode.walk)) {
                    throw new IllegalStateException("Unexpected mode in bike chain: " + leg.getMode());
                }
            }
        }
        // what I can do here, is to get the distance, and then use the average speed to get the travel time.
        // I should cap the distance,
        // I should include the gradient impact, whether on the speed, or on the distance
        // maybe, I'll need to implement a cost penalty for the bikes' routing, penalizing using highways and high gradient roads

        return new BikeVariables(travelTime_min);
    }
}
/**
 * Literature / References:
 * - https://pubmed.ncbi.nlm.nih.gov/16195043/ : they found 1m vertically <=> 8m horizontally
 * - https://www.sciencedirect.com/science/article/pii/S0967070X10000399 : very nice and simple model,
 *   the speed of the vehicle depends on the gradient. This is the model I can fit from their data:
 *   Speed = 5.90 + -0.3191 * Gradient + -0.010237 * Gradient^2
 *   Gradient here is in percentage. I can clip these values by 2.5 and 7.5 m/s
 */