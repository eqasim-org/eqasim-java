package org.eqasim.switzerland.ch_cmdp.calibration;

import java.util.List;
import org.eqasim.core.components.fast_calibration.AlphaCalibrationUtils;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

/** Shared trip eligibility for canton calibration and its initial-share targets. */
final class CantonCalibrationTrips {
    static final List<String> MODES = List.of("car", "pt", "walk", "bike", "car_passenger");

    static boolean isConsidered(DiscreteModeChoiceTrip trip) {
        if (!MODES.contains(trip.getInitialMode()) || !AlphaCalibrationUtils.isConsideredTrip(trip)) {
            return false;
        }
        var origin = trip.getOriginActivity().getCoord();
        var destination = trip.getDestinationActivity().getCoord();
        return origin != null && destination != null && !origin.equals(destination);
    }

    private CantonCalibrationTrips() { }
}
