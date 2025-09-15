package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissCarPassengerVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class Utils {
    public static String HOME = "home";
    public static String WORK = "work";
    public static String URBAN = "urban";
    public static String SUBURBAN = "suburban";
    public static String RURAL = "rural";
    public static String NONE = "none";
    public static double SHORT_DISTANCE_TRIP_KM = 1.0;

    public static boolean originIsHome(DiscreteModeChoiceTrip trip) {
        return HOME.equals(trip.getOriginActivity().getType());
    }

    public static boolean destinationIsHome(DiscreteModeChoiceTrip trip) {
        return HOME.equals(trip.getDestinationActivity().getType());
    }

    public static boolean originIsWork(DiscreteModeChoiceTrip trip) {
        return WORK.equals(trip.getOriginActivity().getType());
    }

    public static boolean destinationIsWork(DiscreteModeChoiceTrip trip) {
        return WORK.equals(trip.getDestinationActivity().getType());
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

    public static boolean isShortDistanceTrip(double distance_km){
        return (distance_km<SHORT_DISTANCE_TRIP_KM);
    }

    public static boolean isShortDistanceTrip(DiscreteModeChoiceTrip trip){
        double distance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        return isShortDistanceTrip(distance_km);
    }

    public static double interaction(double distance_km, double income_CHF, SwissCmdpModeParameters parameters){
        double interactionDistance = EstimatorUtils.interaction(distance_km,
                parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance);

        double interactionIncome = EstimatorUtils.interaction(income_CHF,
                parameters.referenceIncome, parameters.lambdaCostIncome);

        return interactionDistance * interactionIncome;
    }

}
