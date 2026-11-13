package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtVariables;

/**
 * The Swiss PT fares: a calculator for each fare network ("None": the rides in no network, priced by distance), and,
 * optionally, the Léman Pass single tickets.
 */
public class SwissPtStageCostCalculator {

    public Map<String, PtStageCostCalculator> priceCalculators = new HashMap<>();

    /** The Léman Pass single tickets, or null: no Léman Pass. */
    private LemanPass lemanPass = null;

    public SwissPtStageCostCalculator(Map<String, PtStageCostCalculator> map){
        this.priceCalculators = map;
    }

    public SwissPtStageCostCalculator(){
    }

    public LemanPass getLemanPass() {
        return lemanPass;
    }

    public void setLemanPass(LemanPass lemanPass) {
        this.lemanPass = lemanPass;
    }

    /** The calculator of a network, else the distance-based one ("None"). */
    public PtStageCostCalculator calculator(String authority) {
        return priceCalculators.getOrDefault(authority, priceCalculators.get("None"));
    }

    /**
     * Whether a traveller pays half fare whatever the ticket: children from 6 until their 16th birthday (younger ones
     * travel free).
     */
    public static boolean hasChildFare(PersonVariables personVariables) {
        return personVariables.age_a < 16;
    }

    /**
     * Whether the ticket of a network for some legs has the Swiss fares, where the half-fare card holds (_swiss_ticket
     * in eqasim-switzerland, utils/gtfs_router/pricing.py): SBB always; the legs in no network ("None") if none has a
     * stop abroad; a zone network if one of the legs has a stop in Switzerland (tpg sells Unireso Zone 10 + Zone 210 at
     * the reduced fare, but a ticket all in France has no half fare).
     */
    public static boolean hasSwissFares(String authority, List<SwissPtLegVariables> legs) {
        if ("SBB".equals(authority)) {
            return true;
        }
        if ("None".equals(authority)) {
            return legs.stream().noneMatch(leg -> leg.abroad);
        }
        return legs.stream().anyMatch(leg -> leg.inSwitzerland);
    }

    /** The usual tickets of some legs: their price, and whether one is a Unireso ticket abroad (see price). */
    private record Tickets(double price, boolean uniresoAbroad) {}

    /**
     * The price of the usual tickets of some legs, one per network (getPricingStrategy): half fare under 16, or with
     * the half-fare card on the tickets with the Swiss fares (hasSwissFares), then the discounts of the calculators.
     * Rounded to the cent, not capped.
     */
    public double ticketsPrice(List<SwissPtLegVariables> legs, boolean hasHalbtax, PersonVariables personVariables) {
        return tickets(legs, hasHalbtax, personVariables).price();
    }

    private Tickets tickets(List<SwissPtLegVariables> legs, boolean hasHalbtax, PersonVariables personVariables) {
        if (legs.isEmpty()) {
            return new Tickets(0.0, false);
        }
        SwissPtVariables trip = new SwissPtVariables();
        legs.forEach(trip::addStage);

        boolean childFare = hasChildFare(personVariables);
        boolean uniresoAbroad = false;
        double price = 0.0;
        for (Map.Entry<String, List<SwissPtLegVariables>> entry : trip.getPricingStrategy().entrySet()) {
            String authority = entry.getKey();
            List<SwissPtLegVariables> group = entry.getValue();
            boolean halfFare = childFare || (hasHalbtax && hasSwissFares(authority, group));
            price += calculator(authority).calculatePrice(authority, group, halfFare, personVariables);
            uniresoAbroad |= lemanPass != null && lemanPass.isAbroadTicket(authority, group);
        }
        return new Tickets(PtFareRounding.roundToCents(price), uniresoAbroad);
    }

    /**
     * The price of a trip (the free travel already checked), rounded to the cent and not capped (Pricing.price in
     * eqasim-switzerland).
     *
     * legs: the whole legs of the trip; paid: for each of them, the part still to pay with the regional subscriptions
     * (SwissPtRoutePredictor.uncoveredLegs), null when they cover it. The usual tickets of the paid parts, or, with the
     * Léman Pass, a single ticket for some of the legs (bought whole, whatever the subscriptions cover) and the usual
     * tickets of the others, whichever is cheaper. The Léman Pass price is used as it is: it has its own prices for
     * children, youth and the half-fare card. 2nd class: the Java model has no 1st class.
     *
     * With the Léman Pass, Unireso only sells the tickets of Zone 10: its zones abroad (the French zones of
     * gtfs_zones.csv) are those of the Léman Pass, so a way of paying with a Unireso ticket abroad is not kept (nor
     * the Unireso discounts, e.g. free travel under 19, on those rides). Only if no Léman Pass ticket fits the trip is
     * the Unireso price kept.
     */
    public double price(List<SwissPtLegVariables> legs, List<SwissPtLegVariables> paid, boolean hasHalbtax,
            PersonVariables personVariables) {
        if (legs.size() != paid.size()) {
            throw new IllegalArgumentException("One paid part (or null) is needed for each leg");
        }
        List<SwissPtLegVariables> rest = paid.stream().filter(Objects::nonNull).toList();
        if (rest.isEmpty()) {
            return 0.0;
        }
        Tickets usual = tickets(rest, hasHalbtax, personVariables);
        if (lemanPass == null) {
            return usual.price();
        }

        double best = usual.uniresoAbroad() ? Double.POSITIVE_INFINITY : usual.price();
        for (LemanPass.Ticket ticket : lemanPass.singleTickets(legs, personVariables.age_a, hasHalbtax, 2)) {
            List<SwissPtLegVariables> outside = new ArrayList<>();
            for (int k = 0; k < paid.size(); k++) {
                if ((k < ticket.start() || k >= ticket.end()) && paid.get(k) != null) {
                    outside.add(paid.get(k));
                }
            }
            Tickets others = tickets(outside, hasHalbtax, personVariables);
            if (!others.uniresoAbroad()) {
                best = Math.min(best, PtFareRounding.roundToCents(ticket.price() + others.price()));
            }
        }
        return Double.isFinite(best) ? best : usual.price();
    }

    /** The price of a trip without regional subscriptions (see price). */
    public double price(List<SwissPtLegVariables> legs, boolean hasHalbtax, PersonVariables personVariables) {
        return price(legs, legs, hasHalbtax, personVariables);
    }
}
