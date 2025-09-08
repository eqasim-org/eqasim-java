package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.detailed_estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.writer.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissModeDetailedParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwissWalkDetailedUtilityEstimator extends WalkUtilityEstimator {
    private final SwissModeDetailedParameters parameters;
    private final WalkPredictor walkPredictor;
    private final SwissPersonPredictor personPredictor;

    @Inject
    public SwissWalkDetailedUtilityEstimator(SwissModeDetailedParameters parameters, WalkPredictor predictor,
                                             SwissPersonPredictor personPredictor) {
        super(parameters, predictor);

        this.walkPredictor = predictor;
        this.parameters = parameters;
        this.personPredictor = personPredictor;
    }

    protected double estimateConstantUtility() {
        return parameters.walk.alpha_u;
    }

    protected double estimateTravelTimeUtility(WalkVariables variables) {
        return parameters.walk.betaTravelTime_u_min * Math.pow(variables.travelTime_min, parameters.walk.travelTimeExponent);
    }

    protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
        if (personVariables.cantonCluster == 1) {
            return parameters.walk.betaRegion1_u;
        } else if (personVariables.cantonCluster == 2) {
            return parameters.walk.betaRegion2_u;
        } else {
            return 0.0;
        }
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.walk.betaAge * Math.max(0.0, personVariables.age_a - 18);
    }

    protected double estimateSexUtility(SwissPersonVariables personVariables) {
        return personVariables.sex==1 ? parameters.walk.betaSex:0.0;
    }

    protected double estimateShortDistanceUtility(DiscreteModeChoiceTrip trip) {
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return (euclideanDistance_km>1.0)? 0.0 : parameters.walk.betaShortDistance;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        WalkVariables variables = walkPredictor.predictVariables(person, trip, elements);
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;

        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(variables);
        utility += estimateAgeUtility(personVariables);
        utility += estimateSexUtility(personVariables);
        utility += estimateRegionalUtility(personVariables);
        utility += estimateShortDistanceUtility(trip);

//        if(VariablesWriter.isInitiated()) {
//            writeVariablesToCsv(person, trip, variables, utility);
//        }
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

        VariablesWriter.writeVariables("walk", personId, tripIndex, departureTime, utility, walkAttributes);
    }
}
