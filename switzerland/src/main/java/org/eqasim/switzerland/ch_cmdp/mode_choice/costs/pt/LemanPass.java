package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;

/**
 * The Léman Pass single tickets: the tpg tickets for the trips across the border around Geneva (LemanPass in
 * eqasim-switzerland, utils/gtfs_router/lemanpass.py), read from lemanpass_products.csv and lemanpass_stations.csv
 * (LemanPassReader).
 *
 * A product is a set of places, "station:<name>" (a station of the stations file) or "zone:<number>" (a Unireso zone):
 * its pieces, the places each of its two ends holds, and, for a product of zones only, every place it covers. A journey
 * can take a product when one of its places is abroad (a French station, or a zone other than Zone 10) and the product
 * fits it (cheapest). singleTickets gives the tickets a journey can use, each for some of its legs; the other legs are
 * priced as usual and the cheaper of the two ways is kept (SwissPtStageCostCalculator.price).
 */
public class LemanPass {
    /** The network of the zones of the Léman Pass in gtfs_zones.csv. */
    public static final String NETWORK = "Unireso";
    /** The Unireso zone of Geneva: any other zone is abroad. */
    public static final String HOME_ZONE = "10";
    /** The transit mode of the trains. */
    public static final String RAIL = "rail";
    /** The travellers of the single tickets, in the order of the price columns (each with class 2, then class 1). */
    public static final List<String> PROFILES = List.of("adult", "child", "youth", "reduced");

    private static final String STATION = "station:";
    private static final String ZONE = "zone:";

    /** A product: prices[2 * profile + (class 2 ? 0 : 1)] in CHF, NaN when it has no ticket for them. */
    public record Product(int id, String label, Set<String> pieces, Set<String> endA, Set<String> endB, Set<String> cover,
            double[] prices) {
        /** The price for a traveller and a class (1 or 2), NaN if none. */
        public double price(String profile, int travelClass) {
            int index = PROFILES.indexOf(profile);
            return index < 0 ? Double.NaN : prices[2 * index + (travelClass == 1 ? 1 : 0)];
        }

        /** The lowest price of the product over some travellers in a class, NaN if none of them has one. */
        public double price(Collection<String> profiles, int travelClass) {
            double best = Double.NaN;
            for (String profile : profiles) {
                double price = price(profile, travelClass);
                if (!Double.isNaN(price) && !(price >= best)) {
                    best = price;
                }
            }
            return best;
        }
    }

    /** A station of the Léman Pass: its name, its Unireso zone (empty if none) and its country (FR or SW). */
    public record Station(String name, String zone, String country) {}

    /** A product that fits, with its price for the traveller. */
    public record Match(Product product, double price) {}

    /** A Léman Pass ticket for the legs start to end - 1 of a journey. */
    public record Ticket(int start, int end, Product product, double price) {}

    /** What a product must hold: the places of its two ends, and the zones its pieces must include. */
    record Need(int start, int end, Set<String> first, Set<String> last, Set<String> through) {}

    /** An SNCF stop id with the UIC code of its station and a check digit (StopPoint:OCETrain:::TER-87745000). */
    private static final Pattern SNCF_STOP = Pattern.compile("^StopPoint:OCE\\w*:::\\w*-(\\d{7})\\d$");

    private final List<Product> products;
    private final Map<String, Station> stationOfStop;
    private final Map<String, Station> stationOfKey = new HashMap<>();
    private final Map<String, String> countryOfStation = new HashMap<>();

    /** stationOfStop: the station of each stop id of lemanpass_stations.csv. */
    public LemanPass(List<Product> products, Map<String, Station> stationOfStop) {
        this.products = List.copyOf(products);
        this.stationOfStop = Map.copyOf(stationOfStop);
        for (Map.Entry<String, Station> entry : stationOfStop.entrySet()) {
            countryOfStation.put(entry.getValue().name(), entry.getValue().country());
            stationOfKey.putIfAbsent(stationKey(entry.getKey()), entry.getValue());
        }
    }

    /**
     * The station of a stop id, to match the stops of the schedule with those of the stations file when their ids
     * differ (a schedule with the SLOIDs of 2026, ch:1:sloid:1008:2:3, and a file with the DiDok ids, 8501008): the
     * DiDok id for the Swiss stops (SwissPtRoutePredictor.simplifyStopId, as for gtfs_zones.csv), and the UIC code
     * for the SNCF stops (StopPoint:OCETrain:::TER-87745000 -> 8774500).
     */
    static String stationKey(String stopId) {
        Matcher sncf = SNCF_STOP.matcher(stopId);
        if (sncf.find()) {
            return sncf.group(1);
        }
        // only a station number: the part before ':' of other ids (StopPoint:OCECar:...) would join unrelated stops
        String station = SwissPtRoutePredictor.simplifyStopId(stopId);
        return station.matches("\\d+") ? station : stopId;
    }

    public List<Product> products() {
        return products;
    }

    /** The station of a stop id (by its id, else by its station, see stationKey), or null. */
    public Station station(String stopId) {
        if (stopId == null) {
            return null;
        }
        Station station = stationOfStop.get(stopId);
        return station != null ? station : stationOfKey.get(stationKey(stopId));
    }

    public static String stationPlace(String name) {
        return STATION + name;
    }

    public static String zonePlace(String zone) {
        return ZONE + zone;
    }

    /** Whether a place is in the Léman Pass area abroad: a French station, or a zone other than Zone 10. */
    public boolean isAbroad(String place) {
        if (place.startsWith(ZONE)) {
            return !place.equals(ZONE + HOME_ZONE);
        }
        return place.startsWith(STATION) && "FR".equals(countryOfStation.get(place.substring(STATION.length())));
    }

    /**
     * Whether the ticket of a network for some legs is a Unireso ticket with a zone abroad (other than Zone 10): one the
     * Léman Pass sells, not Unireso.
     */
    public boolean isAbroadTicket(String authority, List<SwissPtLegVariables> legs) {
        if (!NETWORK.equals(authority)) {
            return false;
        }
        for (SwissPtLegVariables leg : legs) {
            for (Zone zone : leg.zones.getOrDefault(NETWORK, List.of())) {
                if (!HOME_ZONE.equals(zone.getZoneId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean anyAbroad(Set<String> places) {
        for (String place : places) {
            if (isAbroad(place)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The travellers whose single tickets someone can buy (single_profiles in lemanpass.py): adult, child from 6 to 15,
     * youth from 16 to 25, and reduced with the half-fare card.
     */
    public static List<String> profiles(int age, boolean halbtax) {
        List<String> profiles = new ArrayList<>(List.of("adult"));
        if (6 <= age && age <= 15) profiles.add("child");
        if (16 <= age && age <= 25) profiles.add("youth");
        if (halbtax) profiles.add("reduced");
        return profiles;
    }

    /**
     * The cheapest product whose pieces include through and whose ends hold first and last (in either direction), or a
     * product of zones only that covers all of them, with its lowest price over the travellers in a class; null if none
     * fits. On equal prices, the first label in string order.
     */
    public Match cheapest(Set<String> first, Set<String> last, Set<String> through, Collection<String> profiles, int travelClass) {
        Set<String> all = new HashSet<>(first);
        all.addAll(last);
        all.addAll(through);

        Match best = null;
        for (Product product : products) {
            double price = product.price(profiles, travelClass);
            if (Double.isNaN(price) || !product.pieces().containsAll(through)) {
                continue;
            }
            boolean fits = (product.endA().containsAll(first) && product.endB().containsAll(last))
                    || (product.endB().containsAll(first) && product.endA().containsAll(last))
                    || (product.cover() != null && product.cover().containsAll(all));
            if (fits && (best == null || price < best.price()
                    || (price == best.price() && product.label().compareTo(best.product().label()) < 0))) {
                best = new Match(product, price);
            }
        }
        return best;
    }

    /**
     * The Léman Pass single tickets a journey can use (single_tickets in lemanpass.py), each for some of its legs, the
     * others to be priced as usual; empty when none fits or the journey does not reach into the area abroad.
     *
     * legs: the whole legs of the journey (before any subscription). A leg is in the area when Unireso is among the
     * networks of all its stops; its zones are its Unireso zones. With a train part (from the first to the last rail
     * leg between two stations of the Léman Pass), a ticket covers it and the legs in the area right before and after
     * it, or only some of them, one ticket for each choice; the legs inside the train part that are not trains must be
     * in the area, and their zones be pieces of the product. Without a train part, a ticket covers all the legs if they
     * are all in the area.
     */
    public List<Ticket> singleTickets(List<SwissPtLegVariables> legs, int age, boolean halbtax, int travelClass) {
        List<Ticket> tickets = new ArrayList<>();
        List<String> profiles = profiles(age, halbtax);
        for (Need need : needs(legs)) {
            if (anyAbroad(need.first()) || anyAbroad(need.last()) || anyAbroad(need.through())) {
                Match match = cheapest(need.first(), need.last(), need.through(), profiles, travelClass);
                if (match != null) {
                    tickets.add(new Ticket(need.start(), need.end(), match.product(), match.price()));
                }
            }
        }
        return tickets;
    }

    /** The candidate tickets of a journey, abroad or not (see singleTickets). */
    List<Need> needs(List<SwissPtLegVariables> legs) {
        int n = legs.size();
        List<Need> needs = new ArrayList<>();
        if (n == 0) {
            return needs;
        }

        boolean[] inside = new boolean[n];
        boolean allInside = true;
        List<Integer> train = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            inside[k] = legs.get(k).zones != null && legs.get(k).zones.containsKey(NETWORK);
            allInside &= inside[k];
            if (isTrain(legs.get(k))) {
                train.add(k);
            }
        }

        if (!train.isEmpty()) {
            int a = train.get(0);
            int b = train.get(train.size() - 1) + 1;
            List<Integer> middle = new ArrayList<>();
            for (int k = a; k < b; k++) {
                if (!train.contains(k)) {
                    if (!inside[k]) {
                        return needs;
                    }
                    middle.add(k);
                }
            }
            Set<String> through = zones(legs, middle);

            int start = a;
            while (start > 0 && inside[start - 1]) start--;
            int end = b;
            while (end < n && inside[end]) end++;

            String board = stationPlace(station(firstStop(legs.get(a))).name());
            String alight = stationPlace(station(lastStop(legs.get(b - 1))).name());
            Set<List<Integer>> choices = new LinkedHashSet<>(List.of(List.of(start, end), List.of(a, end), List.of(start, b), List.of(a, b)));
            for (List<Integer> choice : choices) {
                int s = choice.get(0);
                int e = choice.get(1);
                Set<String> first = new HashSet<>(Set.of(board));
                first.addAll(zones(legs, range(s, a)));
                Set<String> last = new HashSet<>(Set.of(alight));
                last.addAll(zones(legs, range(b, e)));
                needs.add(new Need(s, e, first, last, through));
            }
        } else if (allInside) {
            String first = endZone(legs.get(0), true);
            String last = endZone(legs.get(n - 1), false);
            if (first != null && last != null) {
                needs.add(new Need(0, n, Set.of(first), Set.of(last), zones(legs, range(0, n))));
            }
        }
        return needs;
    }

    /** Whether a leg is a train between two stations of the Léman Pass. */
    boolean isTrain(SwissPtLegVariables leg) {
        return RAIL.equals(leg.mode) && station(firstStop(leg)) != null && station(lastStop(leg)) != null;
    }

    private static String firstStop(SwissPtLegVariables leg) {
        return leg.stopIds.isEmpty() ? leg.fromNode : leg.stopIds.get(0);
    }

    private static String lastStop(SwissPtLegVariables leg) {
        return leg.stopIds.isEmpty() ? leg.toNode : leg.stopIds.get(leg.stopIds.size() - 1);
    }

    private static List<Integer> range(int from, int to) {
        List<Integer> indexes = new ArrayList<>();
        for (int k = from; k < to; k++) indexes.add(k);
        return indexes;
    }

    /** The Unireso zones of some legs, as places. */
    private static Set<String> zones(List<SwissPtLegVariables> legs, List<Integer> indexes) {
        Set<String> zones = new HashSet<>();
        for (int k : indexes) {
            for (Zone zone : legs.get(k).zones.getOrDefault(NETWORK, List.of())) {
                zones.add(zonePlace(zone.getZoneId()));
            }
        }
        return zones;
    }

    /**
     * The Unireso zone of the first (or last) stop of a leg, as a place: the lowest zone number for a stop on the border
     * of two zones; null if the stop has none (or its zones are unknown).
     */
    private static String endZone(SwissPtLegVariables leg, boolean first) {
        if (leg.stopZones.isEmpty()) {
            return null;
        }
        String lowest = null;
        for (Zone zone : leg.stopZones.get(first ? 0 : leg.stopZones.size() - 1)) {
            if (NETWORK.equals(zone.getAuthority().getId()) && (lowest == null || compareZones(zone.getZoneId(), lowest) < 0)) {
                lowest = zone.getZoneId();
            }
        }
        return lowest == null ? null : zonePlace(lowest);
    }

    private static int compareZones(String a, String b) {
        try {
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }
}
