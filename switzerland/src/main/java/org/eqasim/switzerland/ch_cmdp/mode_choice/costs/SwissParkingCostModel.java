package org.eqasim.switzerland.ch_cmdp.mode_choice.costs;

import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators.Utils;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SwissParkingCostModel {
    private final SwissCostParameters parameters;

    public SwissParkingCostModel(SwissCostParameters costParameters) {
        parameters = costParameters;
    }

    public double getParkingPrice_CFH(DiscreteModeChoiceTrip trip, CarVariables variables){
        if (Utils.destinationIsNone(trip) || Utils.destinationIsRural(trip) || Utils.destinationIsHome(trip)){
            return 0.0;
        }

        double travelTime = variables.travelTime_min * 60.0;
        double tripArrivalTime = trip.getDepartureTime() + travelTime;
        if (tripArrivalTime > 19.0 * 3600.0){
            return 0.0; // No parking costs after 7pm
        }

        double unpaidDuration = 0.0;
        if (tripArrivalTime<8.0 * 3600.0){
            unpaidDuration += (8.0 * 3600.0 - tripArrivalTime); // Assume arrival at 8am if arriving earlier
        }

        double parkingDuration;
        if (trip.getDestinationActivity().getEndTime().isDefined()) {
            parkingDuration = Math.max(trip.getDestinationActivity().getEndTime().seconds() - tripArrivalTime, 0.0);
        } else if (trip.getDestinationActivity().getMaximumDuration().isDefined()) {
            parkingDuration = trip.getDestinationActivity().getMaximumDuration().seconds();
        } else {
            parkingDuration = 3600.0; // Default to 1 hour if no time info is available
        }

        double departureTime = tripArrivalTime + parkingDuration;
        if (departureTime < 8.0 * 3600.0){
            return 0.0; // No parking costs if parked before 8am
        }
        if (departureTime > 19.0 * 3600.0){
            unpaidDuration += (departureTime - 19.0 * 3600.0); // don't pay for parking after 7pm
        }

        parkingDuration = Math.max(parkingDuration - unpaidDuration, 0.0);
        double parking_duration_h = Math.min(parkingDuration / 3600.0, 11.0) ; // Cap parking duration at 11 hours (from 8am to 7pm), this should never happen due to previous checks

        double parkingPrice_CHF;
        if (Utils.destinationIsUrban(trip) && parking_duration_h>1.0) {
            parkingPrice_CHF = Math.min(40, parameters.urbanParkingCost_CHF_h * (parking_duration_h - 1.0));
        } else if (Utils.destinationIsUrbanCore(trip) && parking_duration_h>1.0){
            parkingPrice_CHF = Math.min(40, parameters.urbancoreParkingCost_CHF_h * (parking_duration_h - 1.0));
        } else if (Utils.destinationIsSuburban(trip) && parking_duration_h>1.0){
            parkingPrice_CHF = Math.min(40, parameters.suburbanParkingCost_CHF_h * (parking_duration_h - 1.0));
        } else {
            parkingPrice_CHF = 0.0;
        }
        // if destination is work, we apply the reduction factor
        if (Utils.destinationIsWork(trip)) {
            parkingPrice_CHF *= parameters.parkingPriceReductionForWork;
        }

        return parkingPrice_CHF;
    }
}
