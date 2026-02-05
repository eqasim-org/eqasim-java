package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.Zone;

public class ZoneBasedPricing implements PtStageCostCalculator {

    private double[] fullPrices;
    private double[] halbTaxPrices;
    private Set<String> zonesCountingDouble;
    private String authority;
    private String centerZoneID;
    private double priceInCenterZone;
    private double priceInAdditionalZone;

    public ZoneBasedPricing(){}

    public ZoneBasedPricing(String name){
        this.authority = name;
    }

    public String getAuthority(){
        return this.authority;
    }

    public void setFullPriceList(double[] priceList){
        this.fullPrices = priceList;
    }

    public void setHalbTaxPriceList(double[] priceList){
        this.halbTaxPrices = priceList;
    }

    public void setDoubleCountingZones(Set<String> zones){
        this.zonesCountingDouble = zones;
    }

    public void setCenterZoneID(String zoneid){
        this.centerZoneID = zoneid;
    }

    public void setPriceInCenterZone(double price){
        this.priceInCenterZone = price;
    }

    public void setPriceInAdditionalZone(double price){
        this.priceInAdditionalZone = price;
    }


    @Override
    public double calculatePrice(List<SwissPtLegVariables> legs, boolean hasHalbtax, String authorityId) {

        double price = 0;

        Set<String> visitedZoneIds = legs.stream()
            .flatMap(leg -> leg.zones.get(authority).stream())
            .map(Zone::getZoneId)
            .collect(Collectors.toSet());        

        System.out.println("  Visited zones for authority " + authority + ": " + visitedZoneIds.toString());

        if (visitedZoneIds == null || visitedZoneIds.isEmpty()) {
            throw new IllegalArgumentException("At least one zone must be visited");
        }

        if (this.fullPrices != null && this.halbTaxPrices != null) {
            double[] prices = this.fullPrices;
            if (hasHalbtax){
                prices = this.halbTaxPrices;
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

            price = Math.round(prices[effectiveZones - 1] * 100.0) / 100.0;

            //System.out.println("Trip info: visited " + effectiveZones + " zones, price: " + price);

        }
        
        else {
            if (this.centerZoneID != null && !Double.isNaN(this.priceInCenterZone) && !Double.isNaN(this.priceInAdditionalZone)) {
                if (visitedZoneIds.contains(this.centerZoneID)) {
                    price = this.priceInCenterZone + this.priceInAdditionalZone * (visitedZoneIds.size() - 1);
                }
                else{
                    // Apply default pricing
                    DefaultDistanceBasedCalculator ddbc = new DefaultDistanceBasedCalculator(0.38, 4.08, -0.00022);
                    price = ddbc.calculatePrice(legs, hasHalbtax, authorityId);
                }
                //System.out.println("Trip info: price: " + price + " for the visited zones " + visitedZoneIds.toString());
            }
            else {
                throw new IllegalArgumentException("Something is wrong");
            }
        }   
        return price;
    }
    
}
