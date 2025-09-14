package org.eqasim.switzerland.ch.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwissZeroUtilityEstimator implements UtilityEstimator {
    private final VariablesWriter variablesWriter;

    @Inject
    public SwissZeroUtilityEstimator(VariablesWriter variablesWriter) {
        this.variablesWriter = variablesWriter;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double utility = 0.0;
        if(variablesWriter.isInitiated()) {
            writeVariablesToCsv(person, trip, utility);
        }
        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        Map<String, String> zeroUtilityAttributes = new HashMap<>();
        zeroUtilityAttributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));

        variablesWriter.writeVariables("car_passenger", personId, tripIndex, departureTime, utility, zeroUtilityAttributes);
    }
}

