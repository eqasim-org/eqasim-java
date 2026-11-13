package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.population.Person;

/**
 * The zones of a regional PT subscription (Verbundabo), per fare network, as written in the population by
 * eqasim-switzerland (matsim/scenario/population.py, format_regional_products): the person attribute
 * RegionalSubscriptionProducts, e.g. "ZVV:110,121,151+Unireso:10". The networks may also be separated by ";" (as in
 * parse_subscriptions of utils/gtfs_router/pricing.py), and a network without zones, or with "all", covers all of
 * its zones. The trip is then priced on the zones it visits outside the subscription (see
 * SwissPtRoutePredictor.uncovered).
 */
public final class RegionalSubscription {
    public static final String ATTRIBUTE = "RegionalSubscriptionProducts";
    public static final RegionalSubscription NONE = new RegionalSubscription(Map.of());

    // The persons share few distinct attribute values, and the cost model asks for them for every PT trip
    private static final Map<String, RegionalSubscription> PARSED = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> zones; // network -> its zones, or null for all of them

    private RegionalSubscription(Map<String, Set<String>> zones) {
        this.zones = Collections.unmodifiableMap(zones);
    }

    public static RegionalSubscription parse(String text) {
        if (text == null || text.isBlank()) {
            return NONE;
        }

        Map<String, Set<String>> zones = new LinkedHashMap<>();
        for (String item : text.split("[+;]")) {
            item = item.trim();
            if (item.isEmpty()) {
                continue;
            }

            int colon = item.indexOf(':');
            String network = (colon < 0 ? item : item.substring(0, colon)).trim();
            Set<String> codes = new LinkedHashSet<>();
            if (colon >= 0) {
                for (String zone : item.substring(colon + 1).split(",")) {
                    if (!zone.isBlank()) {
                        codes.add(zone.trim());
                    }
                }
            }

            boolean all = codes.isEmpty() || codes.equals(Set.of("all"));
            if (all || (zones.containsKey(network) && zones.get(network) == null)) {
                zones.put(network, null);
            } else {
                zones.computeIfAbsent(network, n -> new LinkedHashSet<>()).addAll(codes);
            }
        }

        zones.replaceAll((network, codes) -> codes == null ? null : Collections.unmodifiableSet(codes));
        return zones.isEmpty() ? NONE : new RegionalSubscription(zones);
    }

    /** The subscription zones of a person (NONE without the attribute). */
    public static RegionalSubscription of(Person person) {
        Object value = person.getAttributes().getAttribute(ATTRIBUTE);
        return value == null ? NONE : PARSED.computeIfAbsent(value.toString(), RegionalSubscription::parse);
    }

    public boolean isEmpty() {
        return zones.isEmpty();
    }

    /** network -> its zones (null: all the zones of the network). */
    public Map<String, Set<String>> zones() {
        return zones;
    }

    /** Whether the subscription holds this zone of this network. */
    public boolean covers(String network, String zone) {
        if (!zones.containsKey(network)) {
            return false;
        }
        Set<String> codes = zones.get(network);
        return codes == null || codes.contains(zone);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : zones.entrySet()) {
            if (text.length() > 0) text.append('+');
            text.append(entry.getKey()).append(':').append(entry.getValue() == null ? "all" : String.join(",", entry.getValue()));
        }
        return text.toString();
    }
}
