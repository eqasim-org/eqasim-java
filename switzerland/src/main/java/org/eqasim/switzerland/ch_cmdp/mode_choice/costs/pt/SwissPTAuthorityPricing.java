package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.Zone;

public class SwissPTAuthorityPricing {
    
    public enum ZonalAuthorityPricing implements PtStageCostCalculator{
        ZVV(new double[]{4.60, 4.60, 7.00, 9.20, 11.20, 13.40, 15.60, 17.80}, 
            new double[]{3.20, 3.20, 3.50, 4.60, 5.60, 6.70, 7.80, 8.90},
            Set.of("110", "120")),

        TNW(new double[]{4.20, 5.10, 6.60, 9.40, 10.80, 12.40, 14.40, 16.00}, 
            new double[]{2.90, 3.40, 4.20, 4.70, 5.40, 5.70, 7.20, 8.00}, 
            Set.of()),

        OndeVerte(new double[]{4.40, 4.40, 6.60, 8.80, 11.00, 12.20}, 
            new double[]{2.30, 2.30, 3.30, 4.40, 5.50, 6.60}, 
            Set.of()),

        Frimobil(new double[]{3.00, 5.40, 7.80, 10.40, 12.80, 15.20, 17.40}, 
            new double[]{2.30, 3.20, 3.90, 5.20, 6.40, 7.60, 8.70}, 
            Set.of()),

        Passepartout(new double[]{4.00, 5.20, 7.80, 10.40, 13.00, 15.60, 18.20, 20.80, 23.40, 26.00, 31.20, 33.80}, 
            new double[]{3.00, 3.70, 4.30, 5.20, 6.50, 7.80, 9.10, 10.40, 11.70, 13.00, 14.30, 15.60, 16.90}, 
            Set.of("10")),

        ZVB(new double[]{3.30, 4.40, 6.00, 8.00, 8.80}, 
            new double[]{2.70, 3.20, 3.60, 4.00, 4.40}, 
            Set.of()),

        Libero(new double[]{5.20, 5.20, 8.00, 10.40, 13.20, 15.60, 18.40, 20.80, 23.60, 26.00, 28.80, 31.20, 34.00, 36.40, 39.20}, 
            new double[]{3.00, 3.00, 4.20, 5.20, 6.60, 7.80, 9.20, 10.40, 11.80, 13.00, 14.40, 15.60, 17.00, 18.20, 19.60}, 
            Set.of()),

        Ostwind(new double[]{3.30, 5.00, 7.60, 10.00, 12.60, 15.00, 17.40, 20.00, 22.40, 25.00, 27.20, 29.60, 31.80}, 
            new double[]{2.70, 3.00, 3.80, 5.00, 6.30, 7.50, 8.70, 10.00, 11.20, 12.50, 13.60, 14.80, 15.90}, 
            Set.of()),

        Arcobaleno(new double[]{2.60, 5.20, 7.80, 10.40, 13.00, 15.60, 18.20, 20.80}, 
            new double[]{2.00, 2.60, 3.90, 5.20, 6.50, 7.80, 9.10, 10.40}, 
            Set.of()),

        TVSZ(new double[]{3.70, 5.10, 7.00, 9.40, 11.50, 12.90}, 
            new double[]{2.80, 3.30, 3.70, 4.70, 5.80, 6.50}, 
            Set.of()),

        Awelle(new double[]{3.80, 5.30, 7.60, 9.80, 12.40, 14.60, 17.20, 19.60}, 
            new double[]{2.90, 3.60, 4.20, 4.90, 6.20, 7.30, 8.60, 9.80}, 
            Set.of()),

        Mobilis(new double[]{3.20, 3.90, 5.80, 7.80, 9.60, 11.60, 13.60, 15.40, 17.40, 19.20, 21.20, 23.20, 25.00, 27.00, 28.80}, 
            new double[]{2.40, 2.40, 2.90, 3.90, 4.80, 5.80, 6.80, 7.70, 8.70, 9.60, 10.60, 11.60, 12.50, 13.50, 14.40}, 
            Set.of()),

        Davos(new double[]{3.00, 5.40, 7.80, 10.20, 13.20}, 
            new double[]{2.20, 2.70, 3.90, 5.10, 6.60}, 
            Set.of()),

        EngadinMobil(new double[]{3.00, 5.80, 8.60, 11.40, 14.00, 16.80, 19.60}, 
            new double[]{2.20, 2.90, 4.30, 5.70, 7.00, 8.40, 9.80}, 
            Set.of()),

        Transreno(new double[]{3.00, 4.80, 6.60, 8.40, 10.20, 12.00}, 
            new double[]{2.20, 2.40, 3.30, 4.20, 5.10, 6.00}, 
            Set.of()),

        Klosters(new double[]{3.00, 5.40}, 
            new double[]{2.20, 2.70}, 
            Set.of()),

        SionPT(new double[]{2.80, 3.40, 5.00}, 
            new double[]{1.50, 2.20, 2.50}, 
            Set.of()),

        ZPassOstwind(new double[]{6.40, 6.4, 7.4, 10.20, 12.80, 15.00, 17.80, 21.20, 24.20, 26.60, 29.00},
            new double[]{3.20, 3.20, 3.70, 5.10, 6.40, 7.50, 8.90, 10.60, 12.10, 13.30, 14.50},
            Set.of("110", "120")),

        ZPassAwelle(new double[]{7.00, 7.00, 8.60, 11.20, 14.00, 16.80, 20.20, 23.60, 26.80, 29.60, 31.40},
            new double[]{3.50, 3.50, 4.30, 5.60, 7.00, 8.40, 10.10, 11.80, 13.40, 14.80, 15.70},
            Set.of("110", "120")),

        ZPassSchwyzZug(new double[]{6.60, 6.60, 7.20, 9.60, 12.20, 14.40, 18.00, 20.60, 22.80, 25.40, 28.00, 30.20, 32.80, 33.60},
            new double[]{3.30, 3.30, 3.60, 4.80, 6.10, 7.20, 9.00, 10.30, 11.40, 12.70, 14.00, 15.10, 16.40, 16.80},
            Set.of("110", "120")),

            
        // TODO these shouldn't be the correct prices for Unireso/LemanPass but I don't understand right now
        Unireso(new double[]{3.00, 4.60, 6.20, 7.80}, 
            new double[]{2.00, 3.60, 5.20, 6.80}, 
            Set.of());


            
        private final double[] fullPrices;
        private final double[] halbTaxPrices;
        private final Set<String> zonesCountingDouble;

        ZonalAuthorityPricing(double[] fullPrices, double[] HTPrices, Set<String> zonesCountingDouble) {
            this.fullPrices = fullPrices;
            this.halbTaxPrices = HTPrices;
            this.zonesCountingDouble = zonesCountingDouble;
        }

        @Override
        public double calculatePrice(List<SwissPtLegVariables> legs, boolean hasHalbtax, String authority) {
            double[] prices = this.fullPrices;
            if (hasHalbtax){
                prices = this.halbTaxPrices;
            }

            Set<String> visitedZoneIds = legs.stream()
                .flatMap(leg -> leg.zones.get(authority).stream())
                .map(Zone::getZoneId)
                .collect(Collectors.toSet());

            //System.out.println("  Visited zones for authority " + authority + ": " + visitedZoneIds.toString());

            if (visitedZoneIds == null || visitedZoneIds.isEmpty()) {
                throw new IllegalArgumentException("At least one zone must be visited");
            }

            int effectiveZones = 0;

            for (String zone : visitedZoneIds) {
                effectiveZones++; 
                if (zonesCountingDouble.contains(zone)) {
                    effectiveZones++; 
                }
            }

            if (effectiveZones <= 0) {
                throw new IllegalArgumentException("Effective zones must be >= 1");
            }

            if (effectiveZones > prices.length) {
                return Math.round(prices[prices.length - 1] * 100.0) / 100.0;
            }

            double price = Math.round(prices[effectiveZones - 1] * 100.0) / 100.0;

            System.out.println("Trip info: visited " + effectiveZones + "zones, price: " + price);

            return price;

        }

    }

}
