package org.eqasim.switzerland.ch_cmdp.calibration;

import java.util.Map;
import org.matsim.api.core.v01.population.Person;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators.Utils;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/** Shared person and trip inputs for the five CMDP calibration exports. */
public final class CmdpCalibrationVariables {
    private CmdpCalibrationVariables() { }

    public static void addCommon(Map<String, String> attributes, Person person, DiscreteModeChoiceTrip trip,
            SwissPersonVariables personVariables, SwissCmdpModeParameters parameters, double euclideanDistance_km) {
        attributes.put("euclideanDistance_km", String.valueOf(euclideanDistance_km));
        attributes.put("age", String.valueOf(personVariables.age_a));
        attributes.put("sex", String.valueOf(personVariables.sex));
        attributes.put("region", String.valueOf(personVariables.cantonCluster));
        attributes.put("retired", Utils.isRetired(personVariables) ? "1" : "0");
        attributes.put("junior", Utils.isJunior(personVariables) ? "1" : "0");
        attributes.put("lowIncome", Utils.isLowIncome(personVariables, parameters) ? "1" : "0");
        attributes.put("highIncome", Utils.isHighIncome(personVariables, parameters) ? "1" : "0");
        attributes.put("income", String.valueOf(personVariables.income));
        attributes.put("carOwnershipRatio", String.valueOf(personVariables.carOwnershipRatio));
        Object canton = person.getAttributes().getAttribute("cantonName");
        attributes.put("canton", canton instanceof String value ? value : "");

        attributes.put("originHome", Utils.originIsHome(trip) ? "1" : "0");
        attributes.put("destinationWork", Utils.destinationIsWork(trip) ? "1" : "0");
        attributes.put("destinationHome", Utils.destinationIsHome(trip) ? "1" : "0");
        attributes.put("destinationEducation", Utils.destinationIsEducation(trip) ? "1" : "0");
        attributes.put("destinationShopping", Utils.destinationIsShopping(trip) ? "1" : "0");
        attributes.put("destinationLeisure", Utils.destinationIsLeisure(trip) ? "1" : "0");
        attributes.put("destinationOther", Utils.destinationIsOther(trip) ? "1" : "0");
        attributes.put("urbanDestination", Utils.destinationIsUrban(trip) ? "1" : "0");
        attributes.put("urbancoreDestination", Utils.destinationIsUrbanCore(trip) ? "1" : "0");
        attributes.put("shortDistance", Utils.isShortDistanceTrip(trip, parameters) ? "1" : "0");
        attributes.put("longDistance", Utils.isLongDistanceTrip(trip, parameters) ? "1" : "0");

        // Preserve raw densities and missingness so Python can reproduce their nonlinear effects.
        boolean available = true;
        for (String name : new String[] {"employeeDensity", "companiesDensity", "populationDensity"}) {
            Object value = trip.getDestinationActivity().getAttributes().getAttribute(name);
            available &= value instanceof Number;
            attributes.put(name, value instanceof Number number ? Double.toString(number.doubleValue()) : "0.0");
        }
        attributes.put("densitiesAvailable", available ? "1" : "0");
    }
}
