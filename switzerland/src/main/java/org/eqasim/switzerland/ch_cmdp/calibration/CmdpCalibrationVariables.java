package org.eqasim.switzerland.ch_cmdp.calibration;

import java.util.Map;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators.Utils;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/** Raw inputs keep density exponents and income thresholds reproducible in the optimizer. */
public final class CmdpCalibrationVariables {
    private CmdpCalibrationVariables() { }

    public static void addCommon(Map<String, String> attributes, DiscreteModeChoiceTrip trip,
            SwissPersonVariables person, SwissCmdpModeParameters parameters) {
        attributes.put("highIncome", Utils.isHighIncome(person, parameters) ? "1" : "0");
        boolean available = true;
        for (String name : new String[] {"employeeDensity", "companiesDensity", "populationDensity"}) {
            Object value = trip.getDestinationActivity().getAttributes().getAttribute(name);
            available &= value instanceof Number;
            attributes.put(name, value instanceof Number number ? Double.toString(number.doubleValue()) : "0.0");
        }
        attributes.put("densitiesAvailable", available ? "1" : "0");
    }
}
