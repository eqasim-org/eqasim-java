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
    public static double LOW_INCOME_LIMIT_CHF = 3000.0;
    // municipality types
    public static String URBAN = "urban";
    public static String SUBURBAN = "suburban";
    public static String RURAL = "rural";
    public static String URBAN_CORE = "urbancore";
    // other
    public static String NONE = "none";
    public static double SHORT_DISTANCE_TRIP_KM = 1.0;
    public static double LONG_DISTANCE_TRIP_KM = 13.0;
    public static double RETIRED_AGE_THRESHOLD = 65.0;

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

    public static boolean isShortDistanceTrip(double distance_km){
        return (distance_km<SHORT_DISTANCE_TRIP_KM);
    }

    public static boolean isShortDistanceTrip(DiscreteModeChoiceTrip trip){
        double distance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return isShortDistanceTrip(distance_km);
    }

    public static boolean isLongDistanceTrip(double distance_km){
        return (distance_km>LONG_DISTANCE_TRIP_KM);
    }

    public static boolean isLongDistanceTrip(DiscreteModeChoiceTrip trip){
        double distance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return isLongDistanceTrip(distance_km);
    }

    public static boolean isLowIncome(double income_CHF){
        return (income_CHF<LOW_INCOME_LIMIT_CHF);
    }

    public static boolean isLowIncome(SwissPersonVariables person){
        return isLowIncome(person.income);
    }

    public static boolean isRetired(double age){
        return (age>=RETIRED_AGE_THRESHOLD);
    }
    public static boolean isRetired(SwissPersonVariables person){
        return isRetired(person.age_a);
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

}
