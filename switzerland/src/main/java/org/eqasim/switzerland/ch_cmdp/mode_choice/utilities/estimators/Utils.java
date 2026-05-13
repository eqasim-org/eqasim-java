package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class Utils {
    // purposes
    public static String HOME = "home";
    public static String HOME_SECONDARY = "home_secondary";
    public static String WORK = "work";
    public static String WORK_SECONDARY = "work_secondary";
    public static String EDUCATION = "education";
    public static String EDUCATION_SECONDARY = "education_secondary";
    public static String SHOPPING = "shop";
    public static String LEISURE = "leisure";
    public static String OTHER = "other";
    // income groups
//    public static double LOW_INCOME_LIMIT_CHF = 3000.0;
    // municipality types
    public static String URBAN = "urban";
    public static String SUBURBAN = "suburban";
    public static String RURAL = "rural";
    public static String URBAN_CORE = "urbancore";
    // other
    public static String NONE = "none";
//    public static double SHORT_DISTANCE_TRIP_KM = 1.0;
//    public static double LONG_DISTANCE_TRIP_KM = 13.0;
//    public static double VERY_LONG_DISTANCE_TRIP_KM = 25.0;
    public static double RETIRED_AGE_THRESHOLD = 65.0;
    public static double JUNIOR_AGE_THRESHOLD = 16.0;

    public static boolean originIsHome(DiscreteModeChoiceTrip trip) {
        return HOME.equals(trip.getOriginActivity().getType()) || HOME_SECONDARY.equals(trip.getOriginActivity().getType());
    }

    public static boolean destinationIsHome(DiscreteModeChoiceTrip trip) {
        return HOME.equals(trip.getDestinationActivity().getType()) || HOME_SECONDARY.equals(trip.getDestinationActivity().getType());
    }

    public static boolean destinationIsWork(DiscreteModeChoiceTrip trip) {
        return WORK.equals(trip.getDestinationActivity().getType()) || WORK_SECONDARY.equals(trip.getDestinationActivity().getType());
    }

    public static boolean destinationIsEducation(DiscreteModeChoiceTrip trip) {
        return EDUCATION.equals(trip.getDestinationActivity().getType()) || EDUCATION_SECONDARY.equals(trip.getDestinationActivity().getType());
    }

    public static boolean destinationIsShopping(DiscreteModeChoiceTrip trip) {
        return SHOPPING.equals(trip.getDestinationActivity().getType());
    }

    public static boolean destinationIsLeisure(DiscreteModeChoiceTrip trip) {
        return LEISURE.equals(trip.getDestinationActivity().getType());
    }

    public static boolean destinationIsOther(DiscreteModeChoiceTrip trip) {
        return OTHER.equals(trip.getDestinationActivity().getType());
    }


    public static String getDestinationMunicipalityType(DiscreteModeChoiceTrip trip){
        Object objMunicipalityId = trip.getDestinationActivity().getAttributes().getAttribute("municipalityType");
        return (objMunicipalityId==null)? NONE : objMunicipalityId.toString().toLowerCase();
    }

    public static boolean destinationIsUrban(DiscreteModeChoiceTrip trip){
        return getDestinationMunicipalityType(trip).equals(URBAN);
    }

    public static boolean destinationIsSuburban(DiscreteModeChoiceTrip trip){
        return getDestinationMunicipalityType(trip).equals(SUBURBAN);
    }

    public static boolean destinationIsRural(DiscreteModeChoiceTrip trip){
        return getDestinationMunicipalityType(trip).equals(RURAL);
    }

    public static boolean destinationIsUrbanCore(DiscreteModeChoiceTrip trip){
        return getDestinationMunicipalityType(trip).equals(URBAN_CORE);
    }

    public static boolean destinationIsNone(DiscreteModeChoiceTrip trip){
        return getDestinationMunicipalityType(trip).equals(NONE);
    }

    public static boolean isShortDistanceTrip(double distance_km, SwissCmdpModeParameters parameters){
        return (distance_km<parameters.shortDistance_km);
    }

    public static boolean isShortDistanceTrip(DiscreteModeChoiceTrip trip, SwissCmdpModeParameters parameters){
        double distance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return isShortDistanceTrip(distance_km, parameters);
    }

    public static boolean isLongDistanceTrip(double distance_km, SwissCmdpModeParameters parameters){
        return (distance_km>parameters.longDistance_km);
    }

    public static boolean isLongDistanceTrip(DiscreteModeChoiceTrip trip, SwissCmdpModeParameters parameters){
        double distance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return isLongDistanceTrip(distance_km, parameters);
    }

    public static boolean isVeryLongDistanceTrip(DiscreteModeChoiceTrip trip, SwissCmdpModeParameters parameters){
        double distance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return (distance_km>parameters.veryLongDistance_km);
    }

    public static boolean isLowIncome(double income_CHF, SwissCmdpModeParameters parameters){
        return (income_CHF<=parameters.lowIncomeThreshold);
    }

    public static boolean isLowIncome(SwissPersonVariables person, SwissCmdpModeParameters parameters){
        return isLowIncome(person.income, parameters);
    }

    public static boolean isHighIncome(double income_CHF, SwissCmdpModeParameters parameters){
        return (income_CHF>=parameters.highIncomeThreshold);
    }
    public static boolean isHighIncome(SwissPersonVariables person, SwissCmdpModeParameters parameters){
        return isHighIncome(person.income, parameters);
    }

    public static boolean isRetired(double age){
        return (age>=RETIRED_AGE_THRESHOLD);
    }
    public static boolean isRetired(SwissPersonVariables person){
        return isRetired(person.age_a);
    }


    public static boolean isJunior(double age){
        return (age<JUNIOR_AGE_THRESHOLD);
    }
    public static boolean isJunior(SwissPersonVariables person){
        return isJunior(person.age_a);
    }

    public static boolean isGoodPtService(String ovgk){
        return (ovgk.equals("A")) || (ovgk.equals("B"));
    }

    public static boolean isMediumPtService(String ovgk){
        return (ovgk.equals("C") || (ovgk.equals("D")));
    }

    public static double interaction(double distance_km, double income_CHF, SwissCmdpModeParameters parameters){
        double interactionDistance = EstimatorUtils.interaction(distance_km,
                parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance);

        double interactionIncome = EstimatorUtils.interaction(income_CHF,
                parameters.referenceIncome, parameters.lambdaCostIncome);

        return interactionDistance * interactionIncome;
    }

    public static double getDensitiesAtDestination(DiscreteModeChoiceTrip trip, SwissCmdpModeParameters parameters) {
        Object empDensity = trip.getDestinationActivity().getAttributes().getAttribute("employeeDensity");
        Object compDensity = trip.getDestinationActivity().getAttributes().getAttribute("companiesDensity");
        Object popDensity = trip.getDestinationActivity().getAttributes().getAttribute("populationDensity");

        if (empDensity == null || compDensity == null || popDensity == null) {
            return 0.0;
        }

        // Accept Integer, Double, Float, Long... and avoid ClassCastException
        if (!(empDensity instanceof Number) || !(compDensity instanceof Number) || !(popDensity instanceof Number)) {
            throw new IllegalArgumentException("Density attributes must be of type Number (Integer, Double, Float, Long, etc.)");
        }

        double densityValue = 0.0;

        double empDensityValue = ((Number) empDensity).doubleValue();
        densityValue += parameters.betaDestinationEmployeeDensity_u
                * Math.pow(empDensityValue, parameters.employeesDensityExponent)
                / parameters.employeesDensityScale;

        double compDensityValue = ((Number) compDensity).doubleValue();
        densityValue += parameters.betaDestinationCompaniesDensity_u
                * Math.pow(compDensityValue, parameters.companiesDensityExponent)
                / parameters.companiesDensityScale;

        double popDensityValue = ((Number) popDensity).doubleValue();
        densityValue += parameters.betaDestinationPopulationDensity_u
                * Math.pow(popDensityValue, parameters.populationDensityExponent)
                / parameters.populationDensityScale;

        return densityValue;
    }

    public static boolean destinationIsGoodPtService(DiscreteModeChoiceTrip trip){
        Object ovgk = trip.getDestinationActivity().getAttributes().getAttribute("ovgk");
        if (!(ovgk instanceof String)) {
            return false;
        }
        return isGoodPtService((String) ovgk);
    }

    public static boolean destinationIsMediumPtService(DiscreteModeChoiceTrip trip){
        Object ovgk = trip.getDestinationActivity().getAttributes().getAttribute("ovgk");
        if (!(ovgk instanceof String)) {
            return false;
        }
        return isMediumPtService((String) ovgk);
    }

    public static String ZURICH = "zurich";
    public static String GENEVA = "geneva";
    public static String BASEL = "basel";
    public static String LUZERN = "luzern";
    public static String LAUSANNE = "lausanne";
    public static String BERN = "BERN";

    public static String getMainCity(DiscreteModeChoiceTrip trip){
        Object objMunicipalityId = trip.getDestinationActivity().getAttributes().getAttribute("municipalityId");
        if  (objMunicipalityId != null){
            String municipalityId = objMunicipalityId.toString();
            switch (municipalityId) {
                case "261" -> {
                    return ZURICH;
                }
                case "6621" -> {
                    return GENEVA;
                }
                case "2701" -> {
                    return BASEL;
                }
                case "1061" -> {
                    return LUZERN;
                }
                case "5586" -> {
                    return LAUSANNE;
                }
                case "351" -> {
                    return BERN;
                }
            }
        }
        return NONE;
    }

}
