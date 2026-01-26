package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import java.util.List;

import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;

public class DefaultDistanceBasedCalculator implements PtStageCostCalculator {
    private final double pricePerKm;
    private final double flatRatePrice;
    private final double priceDecreasePerKm2;

    public DefaultDistanceBasedCalculator(double pricePerKm, double flatRatePrice, double priceDecreasePerKm2) {
        this.pricePerKm          = pricePerKm;
        this.flatRatePrice       = flatRatePrice;
        this.priceDecreasePerKm2 = priceDecreasePerKm2;
    }


    public double calculatePrice(List<SwissPtLegVariables> legs, boolean hasHalbtax, String authority){
        double distance = 0;

        for (SwissPtLegVariables leg : legs){
            distance = distance + leg.getNetworkDistance();
        }

        distance = distance / 1000.0; //convert in km

        double price = this.flatRatePrice;
        price += this.pricePerKm * distance;
        price += this.priceDecreasePerKm2 * distance * distance;

        if (! hasHalbtax){
            price = price * 2;
        }

        if (distance < 69) {
            price = Math.ceil(price * 5.0) / 5.0;
        } else {
            price = Math.ceil(price);
        }

        return Math.round(price * 100.0) / 100.0;

    }
    
}
