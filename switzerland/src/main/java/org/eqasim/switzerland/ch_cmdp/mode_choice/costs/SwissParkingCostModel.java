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
        String destinationType = Utils.getDestinationMunicipalityType(trip);
        if (destinationType.equals("none") || destinationType.equals("rural") || Utils.destinationIsHome(trip)){
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
        double parking_duration_h = Math.min(parkingDuration / 3600.0, 11.0) ; // Cap parking duration at 11 hours (from 8am to 7pm)

        if (destinationType.equals("urban") && parking_duration_h>1.0){
            return parameters.urbanParkingCost_CHF_h * parking_duration_h;
        } else if (destinationType.equals("suburban") && parking_duration_h>1.0){
            return parameters.suburbanParkingCost_CHF_h * parking_duration_h;
        }
        return 0.0;
    }
}
