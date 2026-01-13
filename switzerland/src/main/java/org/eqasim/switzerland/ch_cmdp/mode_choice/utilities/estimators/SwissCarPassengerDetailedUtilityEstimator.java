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

    protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
        if (personVariables.cantonCluster == 1) {
            return parameters.cp.betaRegion1_u;
        } else if (personVariables.cantonCluster == 2) {
            return parameters.cp.betaRegion2_u;
        } else {
            return 0.0;
        }
    }

    protected double estimateConstantUtility() {
        return parameters.cp.alpha_u;
    }

    protected double estimateTravelTimeUtility(DiscreteModeChoiceTrip trip, SwissCarPassengerVariables variables) {
        double tt = variables.travelTime_min / parameters.timeScale_min;
        return parameters.cp.betaTravelTime_u_min * Math.pow(tt, parameters.cp.travelTimeExponent);
    }

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrban(trip) ? parameters.cp.betaUrbanDestination_u : 0.0;
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.cp.betaAge_u * Math.max(0.0, personVariables.age_a - 17);
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

    protected double estimateHomeDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsHome(trip) ? parameters.cp.betaDestinationHome_u : 0.0;
    }

    protected double estimateEducationDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsEducation(trip) ? parameters.cp.betaDestinationEducation_u : 0.0;
    }

    protected double estimateShoppingDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsShopping(trip) ? parameters.cp.betaDestinationShopping_u : 0.0;
    }

    protected double estimateLeisureDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsLeisure(trip) ? parameters.cp.betaDestinationLeisure_u : 0.0;
    }

    protected double estimateOtherDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsOther(trip) ? parameters.cp.betaDestinationOther_u : 0.0;
    }

    protected double estimateUrbancoreDestinationUtility(DiscreteModeChoiceTrip trip) {
        return Utils.destinationIsUrbanCore(trip) ? parameters.cp.betaUrbancoreDestination_u : 0.0;
    }

    protected double estimateRetiredUtility(SwissPersonVariables personVariables) {
        return Utils.isRetired(personVariables) ? parameters.cp.betaRetired_u : 0.0;
    }

    protected double estimateLowIncomeUtility(SwissPersonVariables personVariables) {
        return Utils.isLowIncome(personVariables) ? parameters.cp.betaLowIncome_u : 0.0;
    }

    protected double estimateDrivingLicenseUtility(SwissPersonVariables personVariables) {
        return personVariables.drivingLicense==1 ? parameters.cp.betaDrivingLicense_u :0.0;
    }

    protected double estimateShortDistanceUtility(DiscreteModeChoiceTrip trip) {
        return Utils.isShortDistanceTrip(trip)? parameters.cp.betaShortDistance_u :0.0;
    }

    protected double estimateLongDistanceUtility(DiscreteModeChoiceTrip trip) {
        return Utils.isLongDistanceTrip(trip)? parameters.cp.betaLongDistance_u :0.0;
    }

    protected double estimateCantonUtility(Person person) {
        Object cantonObj = person.getAttributes().getAttribute("cantonName");
        if (cantonObj instanceof String canton) {
            return parameters.swissCanton.cp.getOrDefault(canton, 0.0);
        }
        return 0.0;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
        SwissCarPassengerVariables variables = carPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;
        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(trip, variables);
        // person attributes
        utility += estimateAgeUtility(personVariables);
        utility += estimateSexUtility(personVariables);
        utility += estimateRetiredUtility(personVariables);
        utility += estimateLowIncomeUtility(personVariables);
        utility += estimateDrivingLicenseUtility(personVariables);
        // purposes
        utility += estimateHomeDestinationUtility(trip);
        utility += estimateWorkDestinationUtility(trip);
        utility += estimateEducationDestinationUtility(trip);
        utility += estimateShoppingDestinationUtility(trip);
        utility += estimateLeisureDestinationUtility(trip);
        utility += estimateOtherDestinationUtility(trip);
        // origin
        utility += estimateHomeOriginUtility(trip);
        // region
        utility += estimateRegionalUtility(personVariables);
        // distance
        utility += estimateShortDistanceUtility(trip);
        utility += estimateLongDistanceUtility(trip);
        // location
        utility += estimateUrbanDestinationUtility(trip);
        utility += estimateUrbancoreDestinationUtility(trip);
        // canton
        utility += estimateCantonUtility(person);

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

        // person attributes used in utility
        cpAttributes.put("age", String.valueOf(personVariables.age_a));
        cpAttributes.put("sex", String.valueOf(personVariables.sex));
        cpAttributes.put("region", String.valueOf(personVariables.cantonCluster));
        cpAttributes.put("retired", Utils.isRetired(personVariables) ? "1" : "0");
        cpAttributes.put("lowIncome", Utils.isLowIncome(personVariables) ? "1" : "0");
        cpAttributes.put("income", String.valueOf(personVariables.income));
        cpAttributes.put("drivingLicense", String.valueOf(personVariables.drivingLicense));

        // purposes used in utility
        cpAttributes.put("originHome", Utils.originIsHome(trip) ? "1" : "0");
        cpAttributes.put("destinationWork", Utils.destinationIsWork(trip) ? "1" : "0");
        cpAttributes.put("destinationHome", Utils.destinationIsHome(trip) ? "1" : "0");
        cpAttributes.put("destinationEducation", Utils.destinationIsEducation(trip) ? "1" : "0");
        cpAttributes.put("destinationShopping", Utils.destinationIsShopping(trip) ? "1" : "0");
        cpAttributes.put("destinationLeisure", Utils.destinationIsLeisure(trip) ? "1" : "0");
        cpAttributes.put("destinationOther", Utils.destinationIsOther(trip) ? "1" : "0");

        // location/distance used in utility
        cpAttributes.put("urbanDestination", Utils.destinationIsUrban(trip) ? "1" : "0");
        cpAttributes.put("urbancoreDestination", Utils.destinationIsUrbanCore(trip) ? "1" : "0");
        cpAttributes.put("shortDistance", Utils.isShortDistanceTrip(trip) ? "1" : "0");
        cpAttributes.put("longDistance", Utils.isLongDistanceTrip(trip) ? "1" : "0");

        // main level-of-service term used in utility
        cpAttributes.put("travelTime_min", String.valueOf(cpVariables.travelTime_min));

        variablesWriter.writeVariables("car_passenger", personId, tripIndex, departureTime, utility, cpAttributes);
    }

}
