package org.eqasim.switzerland.ch.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwissWalkUtilityEstimator extends WalkUtilityEstimator {
    private final SwissModeParameters parameters;
    private final WalkPredictor predictor;
    private final VariablesWriter variablesWriter;

    @Inject
    public SwissWalkUtilityEstimator(SwissModeParameters parameters, WalkPredictor predictor, VariablesWriter variablesWriter) {
        super(parameters, predictor);

        this.predictor = predictor;
        this.parameters = parameters;
        this.variablesWriter = variablesWriter;
    }

    protected double estimateCantonUtility(Person person) {
        Object cantonObj = person.getAttributes().getAttribute("cantonName");
        if (cantonObj instanceof String canton) {
            return parameters.swissCanton.walk.getOrDefault(canton, 0.0);
        }
        return 0.0;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double utility = 0.0;

        utility += super.estimateUtility(person, trip, elements);
        utility += estimateCantonUtility(person);

        // I think we need to add age condition
        if(variablesWriter.isInitiated()) {
            WalkVariables variables = predictor.predictVariables(person, trip, elements);
            writeVariablesToCsv(person, trip, variables, utility);
        }
        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, WalkVariables variables, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        Map<String, String> walkAttributes = new HashMap<>();
        walkAttributes.put("travelTime_min", String.valueOf(variables.travelTime_min));
        walkAttributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));

        variablesWriter.writeVariables("walk", personId, tripIndex, departureTime, utility, walkAttributes);
    }
}
