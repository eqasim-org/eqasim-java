package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.detailed_estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.writer.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissModeDetailedParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SwissBikeDetailedUtilityEstimator extends BikeUtilityEstimator {
    private final SwissModeDetailedParameters parameters;
    private final SwissPersonPredictor personPredictor;
    private final BikePredictor bikePredictor;

    @Inject
    public SwissBikeDetailedUtilityEstimator(SwissModeDetailedParameters parameters, SwissPersonPredictor personPredictor,
                                             BikePredictor bikePredictor) {
        super(parameters, personPredictor.delegate, bikePredictor);

        this.parameters = parameters;
        this.personPredictor = personPredictor;
        this.bikePredictor = bikePredictor;
    }

    protected double estimateConstantUtility() {
        return parameters.bike.alpha_u;
    }

    protected double estimateTravelTimeUtility(BikeVariables variables) {
        return parameters.bike.betaTravelTime_u_min * Math.pow(variables.travelTime_min, parameters.bike.travelTimeExponent);
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.bike.betaAge * Math.max(0.0, personVariables.age_a - 18);
    }

    protected double estimateSexUtility(SwissPersonVariables personVariables) {
        return personVariables.sex==1 ? parameters.bike.betaSex:0.0;
    }

    protected double estimateHomeOriginUtility(DiscreteModeChoiceTrip trip) {
        String originActivity = trip.getOriginActivity().getType();
        return "home".equals(originActivity) ? parameters.bike.betaOriginHome : 0.0;
    }

    protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
        if (personVariables.cantonCluster == 1) {
            return parameters.bike.betaRegion1_u;
        } else if (personVariables.cantonCluster == 2) {
            return parameters.bike.betaRegion2_u;
        } else {
            return 0.0;
        }
    }

    protected double estimateShortDistanceUtility(DiscreteModeChoiceTrip trip) {
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return (euclideanDistance_km>1.0)? 0.0 : parameters.bike.betaShortDistance;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
        BikeVariables bikeVariables = bikePredictor.predictVariables(person, trip, elements);

        double utility = 0.0;
        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(bikeVariables);
        utility += estimateAgeUtility(personVariables);
        utility += estimateSexUtility(personVariables);
        utility += estimateHomeOriginUtility(trip);
        utility += estimateRegionalUtility(personVariables);
        utility += estimateShortDistanceUtility(trip);

//        if(VariablesWriter.isInitiated()) {
//            writeVariablesToCsv(person, trip, bikeVariables, personVariables, utility);
//        }
        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, BikeVariables bikevariable,
                                     SwissPersonVariables personVariables, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        Map<String, String> bikeAttributes = new HashMap<>();
        bikeAttributes.put("statedPreferenceRegion", String.valueOf(personVariables.statedPreferenceRegion));
        bikeAttributes.put("travelTime_min", String.valueOf(bikevariable.travelTime_min));
        bikeAttributes.put("age_a", String.valueOf(personVariables.age_a));
        bikeAttributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));

        VariablesWriter.writeVariables("bike", personId, tripIndex, departureTime, utility, bikeAttributes);
    }
}