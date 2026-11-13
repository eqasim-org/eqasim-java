package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;

/**
 * The price of a ticket of a zone network (zone_price in eqasim-switzerland, utils/gtfs_router/pricing.py): a price list
 * by number of zones visited (the zones counting as two count twice; beyond the end of the list, its last price;
 * without the full or the half-fare list, the other one), or,
 * for a network around a centre zone (Unireso), the price of the centre zone plus a price for each other zone, both
 * with their own half-fare price, and the default price if the centre zone is not visited. Then the age discount.
 */
public class ZoneBasedPricing implements PtStageCostCalculator {
    private static final Logger LOG = LogManager.getLogger(ZoneBasedPricing.class);

    private double[] fullPrices;
    private double[] halbTaxPrices;
    private Set<String> zonesCountingDouble;
    private String authority;
    private String centerZoneID;
    private double priceInCenterZone = Double.NaN;
    private double priceInCenterZoneHalbtax = Double.NaN;
    private double priceInAdditionalZone = Double.NaN;
    private double priceInAdditionalZoneHalbtax = Double.NaN;
    private PtStageCostCalculator defaultCalculator = new DefaultDistanceBasedCalculator(0.38, 4.08, -0.00022);
    private Map<Integer, Double> ageDiscounts = new HashMap<>();

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

    /** The full price of the centre zone; also the half-fare one until setPriceInCenterZoneHalbtax is called. */
    public void setPriceInCenterZone(double price){
        this.priceInCenterZone = price;
    }

    public void setPriceInCenterZoneHalbtax(double price){
        this.priceInCenterZoneHalbtax = price;
    }

    /** The full price of each other zone; also the half-fare one until setPriceInAdditionalZoneHalbtax is called. */
    public void setPriceInAdditionalZone(double price){
        this.priceInAdditionalZone = price;
    }

    public void setPriceInAdditionalZoneHalbtax(double price){
        this.priceInAdditionalZoneHalbtax = price;
    }

    /** The price of a journey of a centre-based network that does not visit the centre zone. */
    public void setDefaultCalculator(PtStageCostCalculator calculator){
        this.defaultCalculator = calculator;
    }

    /** A discount (in %) for the ages from min_age to max_age; where several apply, the largest one is taken. */
    public void setAgeSpecificDiscount(int min_age, int max_age, double discount){
        for (int age = min_age; age <= max_age; age++){
            ageDiscounts.merge(age, discount, Math::max);
        }
    }

    private double getAgeDiscountFactor(PersonVariables personVariables) {
        if (personVariables == null || ageDiscounts.isEmpty()) {
            return 1.0;
        }

        Double discount = ageDiscounts.get(personVariables.age_a);
        // a discount of 100 % makes the ticket free
        return discount == null ? 1.0 : 1.0 - discount / 100.0;
    }

    /** The half-fare price (else the full one) with the half-fare card, the full price (else the half-fare one) without. */
    private static double fareOf(boolean hasHalbtax, double full, double halbtax) {
        double first = hasHalbtax ? halbtax : full;
        return Double.isNaN(first) ? (hasHalbtax ? full : halbtax) : first;
    }

    /**
     * Logs the prices of the network that are missing, once it is read: a missing price list (or centre zone price) is
     * replaced by the other one, with a warning. A missing half-fare price of the other zones is expected (Unireso:
     * the zones around Geneva are abroad, where the half-fare card does not hold): the full price, logged as info.
     * A network with no usable prices is a warning too: pricing a trip in it fails.
     */
    public void checkPrices() {
        boolean list = fullPrices != null || halbTaxPrices != null;
        boolean centre = centerZoneID != null
                && !(Double.isNaN(priceInCenterZone) && Double.isNaN(priceInCenterZoneHalbtax))
                && !(Double.isNaN(priceInAdditionalZone) && Double.isNaN(priceInAdditionalZoneHalbtax));

        if (list) {
            if (fullPrices == null) {
                LOG.warn("Zone network {}: no full price list, the half-fare one is used for everyone", authority);
            } else if (halbTaxPrices == null) {
                LOG.warn("Zone network {}: no half-fare price list, the full one is used with the half-fare card", authority);
            }
        } else if (centre) {
            if (Double.isNaN(priceInCenterZone)) {
                LOG.warn("Zone network {}: no full price for the centre zone, the half-fare one is used for everyone", authority);
            } else if (Double.isNaN(priceInCenterZoneHalbtax)) {
                LOG.warn("Zone network {}: no half-fare price for the centre zone, the full one is used with the half-fare card", authority);
            }
            if (Double.isNaN(priceInAdditionalZone)) {
                LOG.warn("Zone network {}: no full price for the other zones, the half-fare one is used for everyone", authority);
            } else if (Double.isNaN(priceInAdditionalZoneHalbtax)) {
                LOG.info("Zone network {}: no half-fare price for the other zones, the full one is used with the half-fare card", authority);
            }
        } else {
            LOG.warn("Zone network {}: no price list and no complete centre zone prices; its trips cannot be priced", authority);
        }
    }

    @Override
    public double calculatePrice(String authorityId, List<SwissPtLegVariables> legs, boolean hasHalbtax, PersonVariables personVariables) {
        Set<String> visitedZoneIds = legs.stream()
            .flatMap(leg -> leg.zones.getOrDefault(authority, List.of()).stream())
            .map(Zone::getZoneId)
            .collect(Collectors.toSet());

        if (visitedZoneIds.isEmpty()) {
            throw new IllegalArgumentException("At least one zone of " + authority + " must be visited");
        }

        double price;

        if (this.fullPrices != null || this.halbTaxPrices != null) {
            // without one of the two lists, the other one (see checkPrices)
            double[] prices = hasHalbtax
                ? (this.halbTaxPrices != null ? this.halbTaxPrices : this.fullPrices)
                : (this.fullPrices != null ? this.fullPrices : this.halbTaxPrices);

            int effectiveZones = 0;
            for (String zone : visitedZoneIds) {
                effectiveZones += this.zonesCountingDouble != null && this.zonesCountingDouble.contains(zone) ? 2 : 1;
            }

            price = PtFareRounding.roundToCents(prices[Math.min(effectiveZones, prices.length) - 1]);
        } else if (this.centerZoneID != null
                && !Double.isNaN(fareOf(hasHalbtax, this.priceInCenterZone, this.priceInCenterZoneHalbtax))
                && !Double.isNaN(fareOf(hasHalbtax, this.priceInAdditionalZone, this.priceInAdditionalZoneHalbtax))) {
            if (visitedZoneIds.contains(this.centerZoneID)) {
                double center = fareOf(hasHalbtax, this.priceInCenterZone, this.priceInCenterZoneHalbtax);
                double additional = fareOf(hasHalbtax, this.priceInAdditionalZone, this.priceInAdditionalZoneHalbtax);
                price = center + additional * (visitedZoneIds.size() - 1);
            } else {
                price = this.defaultCalculator.calculatePrice(authorityId, legs, hasHalbtax, personVariables);
            }
        } else {
            throw new IllegalArgumentException("No prices for the zone network " + authority);
        }

        return PtFareRounding.roundToCents(price * getAgeDiscountFactor(personVariables));
    }

}
