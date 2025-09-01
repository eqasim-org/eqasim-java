package org.eqasim.switzerland.ch.mode_choice.costs.pt;

import java.util.List;

import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPtLegVariables;

public class SBBPtStageCostCalculator implements PtStageCostCalculator {
    private static final double MIN_PRICE     = 3.0;
    private static final double MIN_PRICE_HT  = 2.20;
    private static final double[][] DIST_BINS = {
        {1, 4, 0.4623},
        {5, 14, 0.4393},
        {15, 48, 0.3867},
        {49, 150, 0.2755},
        {151, 200, 0.2670},
        {201, 250, 0.2373},
        {251, 300, 0.2142},
        {301, 480, 0.2087},
        {481, 1500, 0.2061}
    };

    public SBBPtStageCostCalculator() {
    }

    private static double SBBDistanceRounding(double d) {
        if (d <= 8) {
            return (Math.ceil(d / 4.0) * 4);
        } else if (d <= 30) {
            return (Math.ceil(d / 2.0) * 2);
        } else if (d <= 60) {
            return (Math.ceil(d / 3.0) * 3);
        } else if (d <= 100) {
            return (Math.ceil(d / 4.0) * 4);
        } else if (d <= 150) {
            return (Math.ceil(d / 5.0) * 5);
        } else if (d <= 300) {
            return (Math.ceil(d / 10.0) * 10);
        } else {
            return (Math.ceil(d / 20.0) * 20);
        }
    }

    public double calculatePrice(List<SwissPtLegVariables> legs, boolean hasHalbtax){
        double distance = 0;
        double price    = 0;

        for (SwissPtLegVariables leg : legs){
            distance = distance + leg.getSbbDistance();
        }

        double roundedDistance = SBBDistanceRounding(distance);

        for (double[] bracket : DIST_BINS) {
            int lower = (int) bracket[0];
            int upper = (int) bracket[1];
            double rate = bracket[2];

            if (roundedDistance >= lower) {
                double usedKm = Math.min(roundedDistance, upper) - lower + 1;
                price += usedKm * rate;
            } else {
                break;
            }
        }

        if (roundedDistance < 69) {
            price = Math.ceil(price * 5.0) / 5.0;
        } else {
            price = Math.ceil(price);
        }

        if (hasHalbtax){
            price = price / 2.0;
            price = Math.max(price, MIN_PRICE_HT);
        }
        else{
            price = Math.max(price, MIN_PRICE);
        }

        return Math.round(price * 100.0) / 100.0;

    }
    
}
