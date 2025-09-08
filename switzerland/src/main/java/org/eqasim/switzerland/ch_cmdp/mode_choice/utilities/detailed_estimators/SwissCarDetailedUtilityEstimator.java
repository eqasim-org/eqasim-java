package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.detailed_estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.writer.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissModeDetailedParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwissCarDetailedUtilityEstimator extends CarUtilityEstimator {
    private final SwissModeDetailedParameters parameters;
    private final SwissPersonPredictor personPredictor;
    private final CarPredictor carPredictor;

    @Inject
    public SwissCarDetailedUtilityEstimator(SwissModeDetailedParameters parameters, CarPredictor carPredictor,
                                            SwissPersonPredictor personPredictor) {
        super(parameters, carPredictor);

        this.personPredictor = personPredictor;
        this.parameters = parameters;
        this.carPredictor = carPredictor;
    }

    protected double estimateConstantUtility() {
        return parameters.car.alpha_u;
    }

    protected double estimateTravelTimeUtility(DiscreteModeChoiceTrip trip, CarVariables variables) {
        double travelTime = variables.travelTime_min + getParkingSearchDuration(trip);
        return parameters.car.betaTravelTime_u_min * Math.pow(travelTime, parameters.car.travelTimeExponent);
    }

    protected String getDestinationType(DiscreteModeChoiceTrip trip){
        Object objMunicipalityId = trip.getDestinationActivity().getAttributes().getAttribute("municipalityType");
        return (objMunicipalityId==null)? "none" : objMunicipalityId.toString().toLowerCase();
    }

    protected double getParkingSearchDuration(DiscreteModeChoiceTrip trip){
        if (getDestinationType(trip).equals("urban")){
            return parameters.parking.urbanParkingSearchDuration_min;
        } else if (getDestinationType(trip).equals("suburban")){
            return parameters.parking.suburbanParkingSearchDuration_min;
        }
        return 0.0;
    }

    protected double getParkingPrice(DiscreteModeChoiceTrip trip, CarVariables variables){
        double travelTime = variables.travelTime_min * 60.0;
        double tripArrivalTime = trip.getDepartureTime() + travelTime;
        double parkingDuration = trip.getDestinationActivity().getEndTime().isDefined()
            ? Math.max(trip.getDestinationActivity().getEndTime().seconds() - tripArrivalTime, 0.0)
            : (trip.getDestinationActivity().getMaximumDuration().isDefined() ? trip.getDestinationActivity().getMaximumDuration().seconds():3600.0);

        double parking_duration_h = parkingDuration / 3600.0;

        if (getDestinationType(trip).equals("urban") && parking_duration_h>1.0){
            return parameters.parking.urbanParkingCostPerHour * parking_duration_h;
        } else if (getDestinationType(trip).equals("suburban") && parking_duration_h>1.0){
            return parameters.parking.suburbanParkingCostPerHour * parking_duration_h;
        }
        return 0.0;
    }

    protected double estimateUrbanDestinationUtility(DiscreteModeChoiceTrip trip) {
        String destinationType = getDestinationType(trip);
        return destinationType.equals("urban") ? parameters.car.betaUrbanDestination : 0.0;
    }

    protected double estimateMonetaryCostUtility(DiscreteModeChoiceTrip trip, CarVariables variables, SwissPersonVariables personVariables) {
        double costParking = getParkingPrice(trip, variables);
        double totalCost = variables.cost_MU + costParking;

        double interactionDistance = EstimatorUtils.interaction(variables.euclideanDistance_km,
                parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance);
        double interactionIncome = EstimatorUtils.interaction(personVariables.income,
                parameters.referenceIncome, parameters.lambdaCostIncome);
        double interaction = interactionDistance * interactionIncome;

        return parameters.betaCost_u_MU * totalCost * interaction;
    }

    protected double estimateAgeUtility(SwissPersonVariables personVariables) {
        return parameters.car.betaAge * Math.max(0.0, personVariables.age_a - 18);
    }

    protected double estimateSexUtility(SwissPersonVariables personVariables) {
        return personVariables.sex==1 ? parameters.car.betaSex:0.0;
    }

    protected double estimateHomeOriginUtility(DiscreteModeChoiceTrip trip) {
        String originActivity = trip.getOriginActivity().getType();
        return "home".equals(originActivity) ? parameters.car.betaOriginHome : 0.0;
    }

    protected double estimateWorkDestinationUtility(DiscreteModeChoiceTrip trip) {
        String destinationActivity = trip.getDestinationActivity().getType();
        return "work".equals(destinationActivity) ? parameters.car.betaDestinationWork : 0.0;
    }

    protected double estimateRegionalUtility(SwissPersonVariables personVariables) {
        if (personVariables.cantonCluster == 1) {
            return parameters.car.betaRegion1_u;
        } else if (personVariables.cantonCluster == 2) {
            return parameters.car.betaRegion2_u;
        } else {
            return 0.0;
        }
    }

    protected double estimateShortDistanceUtility(CarVariables variables) {
        return (variables.euclideanDistance_km>1.0)? 0.0 : parameters.car.betaShortDistance;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SwissPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
        CarVariables variables = carPredictor.predictVariables(person, trip, elements);

        double utility = 0.0;

        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtility(trip, variables);
        utility += estimateMonetaryCostUtility(trip, variables, personVariables);
        utility += estimateWorkDestinationUtility(trip);
        utility += estimateUrbanDestinationUtility(trip);
        utility += estimateSexUtility(personVariables);
        utility += estimateAgeUtility(personVariables);
        utility += estimateRegionalUtility(personVariables);
        utility += estimateHomeOriginUtility(trip);
        utility += estimateShortDistanceUtility(variables);

//        if(VariablesWriter.isInitiated()) {
//            CarVariables carVariable = carPredictor.predictVariables(person, trip, elements);
//            writeVariablesToCsv(person, trip, carVariable, personVariables, utility);
//        }

        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, CarVariables carVariable,
                                     SwissPersonVariables personVariables, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();

        Map<String, String> carAttributes = new HashMap<>();
        carAttributes.put("travelTime_min", String.valueOf(carVariable.travelTime_min));
        carAttributes.put("accessEgressTime_min", String.valueOf(carVariable.accessEgressTime_min));
        carAttributes.put("euclideanDistance_km", String.valueOf(carVariable.euclideanDistance_km));
        carAttributes.put("cost_MU", String.valueOf(carVariable.cost_MU));
        carAttributes.put("statedPreferenceRegion", String.valueOf(personVariables.statedPreferenceRegion));

        VariablesWriter.writeVariables("car", personId, tripIndex, departureTime, utility, carAttributes);
    }
}