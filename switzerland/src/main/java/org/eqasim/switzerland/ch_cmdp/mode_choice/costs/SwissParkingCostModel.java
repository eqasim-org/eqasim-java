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
        double parkingDuration = trip.getDestinationActivity().getEndTime().isDefined()
                ? Math.max(trip.getDestinationActivity().getEndTime().seconds() - tripArrivalTime, 0.0)
                : (trip.getDestinationActivity().getMaximumDuration().isDefined()
                    ? trip.getDestinationActivity().getMaximumDuration().seconds()
                    : 3600.0);

        double parking_duration_h = parkingDuration / 3600.0;

        if (destinationType.equals("urban") && parking_duration_h>1.0){
            return parameters.urbanParkingCost_CHF_h * parking_duration_h;
        } else if (destinationType.equals("suburban") && parking_duration_h>1.0){
            return parameters.suburbanParkingCost_CHF_h * parking_duration_h;
        }
        return 0.0;
    }
}
