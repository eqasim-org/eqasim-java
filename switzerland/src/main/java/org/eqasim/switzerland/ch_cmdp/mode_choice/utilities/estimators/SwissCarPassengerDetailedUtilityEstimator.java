package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
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
    private final SwissCmdpModeParameters parameters;
    private final SwissPersonPredictor personPredictor;
    private final CarPassengerPredictor carPredictor;
    private final VariablesWriter variablesWriter;

    @Inject
    public SwissCarPassengerDetailedUtilityEstimator(SwissCmdpModeParameters parameters, CarPassengerPredictor carPredictor,
                                                     SwissPersonPredictor personPredictor, VariablesWriter variablesWriter) {
        this.personPredictor = personPredictor;
        this.parameters = parameters;
        this.carPredictor = carPredictor;
        this.variablesWriter = variablesWriter;
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

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrban(trip) ? parameters.cp.betaUrbanDestination_u : 0.0;
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.cp.betaAge_u * Math.max(0.0, personVariables.age_a - 18);
    }

    protected double estimateSexUtility(SwissPersonVariables personVariables) {
        return personVariables.sex==1 ? parameters.cp.betaSex_u :0.0;
    }

    protected double estimateHomeOriginUtility(DiscreteModeChoiceTrip trip) {
        return Utils.originIsHome(trip) ? parameters.cp.betaOriginHome_u : 0.0;
    }

    protected double estimateWorkDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsWork(trip) ? parameters.cp.betaDestinationWork_u : 0.0;
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
        return personVariables.drivingLicense==1 ? parameters.cp.betaDrivingLicense_u :0.0;
    }

    protected double estimateShortDistanceUtility(SwissCarPassengerVariables variables) {
        return Utils.isShortDistanceTrip(variables.euclideanDistance_km)? parameters.cp.betaShortDistance_u :0.0;
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

        if(variablesWriter.isInitiated()) {
            writeVariablesToCsv(person, trip, variables, personVariables, utility);
        }

        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, SwissCarPassengerVariables cpVariables,
                                     SwissPersonVariables personVariables, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();
        double euclideanDistance_km = cpVariables.euclideanDistance_km;

        Map<String, String> cpAttributes = new HashMap<>();

        cpAttributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));
        cpAttributes.put("age", String.valueOf(personVariables.age_a));
        cpAttributes.put("sex", String.valueOf(personVariables.sex));
        cpAttributes.put("region", String.valueOf(personVariables.cantonCluster));
        cpAttributes.put("originHome", Utils.originIsHome(trip) ? "1" : "0");
        cpAttributes.put("destinationWork", Utils.destinationIsWork(trip) ? "1" : "0");
        cpAttributes.put("urbanDestination", Utils.destinationIsUrban(trip) ? "1" : "0");
        cpAttributes.put("shortDistance", Utils.isShortDistanceTrip(trip) ? "1" : "0");

        cpAttributes.put("travelTime_min", String.valueOf(cpVariables.travelTime_min));

        cpAttributes.put("drivingLicense", String.valueOf(personVariables.drivingLicense));

        variablesWriter.writeVariables("car_passenger", personId, tripIndex, departureTime, utility, cpAttributes);
    }

}

