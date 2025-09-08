package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.detailed_estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.writer.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissModeDetailedParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.CarPassengerPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissCarPassengerVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class SwissCarPassengerDetailedUtilityEstimator implements UtilityEstimator {
    private final SwissModeDetailedParameters parameters;
    private final SwissPersonPredictor personPredictor;
    private final CarPassengerPredictor carPredictor;

    @Inject
    public SwissCarPassengerDetailedUtilityEstimator(SwissModeDetailedParameters parameters, CarPassengerPredictor carPredictor,
                                                     SwissPersonPredictor personPredictor) {
        this.personPredictor = personPredictor;
        this.parameters = parameters;
        this.carPredictor = carPredictor;
    }

    protected double estimateConstantUtility() {
        return parameters.cp.alpha_u;
    }

    protected double estimateTravelTimeUtility(DiscreteModeChoiceTrip trip, SwissCarPassengerVariables variables) {
        return parameters.cp.betaTravelTime_u_min * Math.pow(variables.travelTime_min, parameters.cp.travelTimeExponent);
    }

    protected double estimateDistanceUtility(DiscreteModeChoiceTrip trip, SwissCarPassengerVariables variables) {
        return parameters.cp.betaDistance_km * Math.pow(variables.euclideanDistance_km, parameters.cp.distanceExponent);
    }

    protected String getDestinationType(DiscreteModeChoiceTrip trip){
        Object objMunicipalityId = trip.getDestinationActivity().getAttributes().getAttribute("municipalityType");
        return (objMunicipalityId==null)? "none" : objMunicipalityId.toString().toLowerCase();
    }

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        String destinationType = getDestinationType(trip);
        return destinationType.equals("urban") ? parameters.cp.betaUrbanDestination : 0.0;
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.cp.betaAge * Math.max(0.0, personVariables.age_a - 18);
    }

    protected double estimateSexUtility(SwissPersonVariables personVariables) {
        return personVariables.sex==1 ? parameters.cp.betaSex:0.0;
    }

    protected double estimateHomeOriginUtility(DiscreteModeChoiceTrip trip) {
        String originActivity = trip.getOriginActivity().getType();
        return "home".equals(originActivity) ? parameters.cp.betaOriginHome : 0.0;
    }

    protected double estimateWorkDestinationUtility(DiscreteModeChoiceTrip trip) {
        String destinationActivity = trip.getDestinationActivity().getType();
        return "work".equals(destinationActivity) ? parameters.cp.betaDestinationWork : 0.0;
    }

    protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
        if (personVariables.cantonCluster == 1) {
            return parameters.cp.betaRegion1_u;
        } else if (personVariables.cantonCluster == 2) {
            return parameters.cp.betaRegion2_u;
        } else {
            return 0.0;
        }
    }

    protected double estimateDrivingLicenseUtility(SwissPersonVariables personVariables) {
        return personVariables.drivingLicense==1 ? parameters.cp.betaDrivingLicense:0.0;
    }

    protected double estimateShortDistanceUtility(SwissCarPassengerVariables variables) {
        return (variables.euclideanDistance_km>1.0)? 0.0 : parameters.cp.betaShortDistance;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
        SwissCarPassengerVariables variables = carPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;
        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(trip, variables);
        utility += estimateDistanceUtility(trip, variables);
        utility += estimateDrivingLicenseUtility(personVariables);
        utility += estimateWorkDestinationUtility(trip);
        utility += estimateAgeUtility(personVariables);
        utility += estimateSexUtility(personVariables);
        utility += estimateUrbanDestinationUtility(trip);
        utility += estimateRegionalUtility(personVariables);
        utility += estimateHomeOriginUtility(trip);
        utility += estimateShortDistanceUtility(variables);

//        if(VariablesWriter.isInitiated()) {
//            writeVariablesToCsv(person, trip, utility);
//        }
        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        Map<String, String> zeroUtilityAttributes = new HashMap<>();
        zeroUtilityAttributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));

        VariablesWriter.writeVariables("car_passenger", personId, tripIndex, departureTime, utility, zeroUtilityAttributes);
    }
}

