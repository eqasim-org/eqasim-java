package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissCarPassengerVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.router.TripStructureUtils;

import java.util.List;


public class CarPassengerPredictor extends CachedVariablePredictor<SwissCarPassengerVariables> {
    @SuppressWarnings("unused")
    private final ModeParameters parameters;

    @Inject
    public CarPassengerPredictor(ModeParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public SwissCarPassengerVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double carTravelTime_min = 0.0;
        double accessEgressTime_min = 0.0;

        for (Leg leg : TripStructureUtils.getLegs(elements)) {
            if (leg.getMode().equals("car_passenger")) {
                carTravelTime_min += leg.getTravelTime().seconds() / 60.0;
            } else if (leg.getMode().equals(TransportMode.walk)) {
                accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
            } else {
                throw new IllegalStateException("Unexpected mode in car chain: " + leg.getMode());
            }
        }

        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        return new SwissCarPassengerVariables(carTravelTime_min, euclideanDistance_km, accessEgressTime_min);
    }
}
