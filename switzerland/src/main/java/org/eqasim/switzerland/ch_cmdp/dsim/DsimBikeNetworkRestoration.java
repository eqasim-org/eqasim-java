package org.eqasim.switzerland.ch_cmdp.dsim;

import java.util.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.VehicleUtils;

/** Inverse of the isolated bike-layer conversion; it never keeps a second copy of the road network. */
public final class DsimBikeNetworkRestoration {
    private static final String MARKER = "eqasim:dsimBikeNetworkLayer";
    private final Object originalMarker;
    private final Map<Id<Link>, List<List<Id<Link>>>> bikeTurns = new HashMap<>();
    private final Set<Id<Link>> converted = new HashSet<>();
    private boolean restored;

    private DsimBikeNetworkRestoration(Network network) {
        originalMarker = network.getAttributes().getAttribute(MARKER);
        if (Boolean.parseBoolean(String.valueOf(originalMarker))) return;
        for (Link link : network.getLinks().values()) if (link.getAllowedModes().contains(TransportMode.bike)) {
            converted.add(link.getId());
            var turns = NetworkUtils.getDisallowedNextLinks(link);
            if (turns != null) {
                var sequences = turns.getDisallowedLinkSequences(TransportMode.bike);
                if (sequences != null && !sequences.isEmpty())
                    bikeTurns.put(link.getId(), sequences.stream().map(List::copyOf).toList());
            }
        }
    }

    /** Record affected road IDs and removed bike restrictions before conversion. */
    public static DsimBikeNetworkRestoration capture(Network network) {
        return new DsimBikeNetworkRestoration(network);
    }

    /** Restore surviving originals without undoing calibrated attributes or resurrecting deleted links. */
    public void restore(Scenario scenario) {
        if (restored) return;
        if (!Boolean.parseBoolean(String.valueOf(originalMarker))) {
            restoreReferences(scenario);
            restoreRoadModesAndTurns(scenario.getNetwork());
            removeTemporaryLayer(scenario.getNetwork());
        }
        converted.clear(); bikeTurns.clear(); restored = true;
    }

    /** Map only references to copies owned by this conversion; keep current plans and their timing. */
    private void restoreReferences(Scenario scenario) {
        for (var person : scenario.getPopulation().getPersons().values()) for (var plan : person.getPlans()) {
            for (var element : plan.getPlanElements()) {
                if (element instanceof Activity activity && owns(activity.getLinkId()))
                    activity.setLinkId(original(activity.getLinkId()));
                else if (element instanceof Leg leg && leg.getRoute() != null) {
                    var route = leg.getRoute();
                    if (route instanceof NetworkRoute nr && (owns(nr.getStartLinkId()) || owns(nr.getEndLinkId())
                            || nr.getLinkIds().stream().anyMatch(this::owns)))
                        nr.setLinkIds(original(nr.getStartLinkId()), nr.getLinkIds().stream().map(this::original).toList(),
                                original(nr.getEndLinkId()));
                    else {
                        if (owns(route.getStartLinkId())) route.setStartLinkId(original(route.getStartLinkId()));
                        if (owns(route.getEndLinkId())) route.setEndLinkId(original(route.getEndLinkId()));
                    }
                }
            }
        }
        for (var vehicle : scenario.getVehicles().getVehicles().values()) {
            var link = VehicleUtils.getInitialLinkId(vehicle);
            if (owns(link)) VehicleUtils.setInitialLinkId(vehicle, original(link));
        }
    }

    /** Reinsert the removed bike mode/restrictions, keeping all current modes and non-bike restrictions. */
    private void restoreRoadModesAndTurns(Network network) {
        for (Id<Link> id : converted) {
            Link road = network.getLinks().get(id);
            if (road == null) continue;
            var modes = new LinkedHashSet<>(road.getAllowedModes()); modes.add(TransportMode.bike);
            road.setAllowedModes(modes);
            var sequences = bikeTurns.get(id);
            if (sequences != null) {
                var turns = NetworkUtils.getDisallowedNextLinks(road);
                if (turns == null) { turns = new DisallowedNextLinks(); NetworkUtils.setDisallowedNextLinks(road, turns); }
                // A bike restriction introduced by the simulation wins over the removed original restrictions.
                var current = turns.getDisallowedLinkSequences(TransportMode.bike);
                if (current == null || current.isEmpty())
                    for (var sequence : sequences) turns.addDisallowedLinkSequence(TransportMode.bike, sequence);
            }
        }
    }

    /** Delete only links created for affected road IDs, then their unused temporary nodes. */
    private void removeTemporaryLayer(Network network) {
        var nodes = new HashSet<Id<Node>>();
        for (Id<Link> id : converted) {
            Link copy = network.removeLink(Id.createLinkId(DsimBikeNetworkAdapter.LINK_PREFIX + id));
            if (copy != null) { nodes.add(copy.getFromNode().getId()); nodes.add(copy.getToNode().getId()); }
        }
        for (Id<Node> id : nodes) {
            Node node = network.getNodes().get(id);
            if (node != null && node.getInLinks().isEmpty() && node.getOutLinks().isEmpty()) network.removeNode(id);
        }
        if (originalMarker == null) network.getAttributes().removeAttribute(MARKER);
        else network.getAttributes().putAttribute(MARKER, originalMarker);
    }

    private boolean owns(Id<Link> id) {
        return DsimBikeNetworkAdapter.isBikeLink(id) && converted.contains(
                Id.createLinkId(id.toString().substring(DsimBikeNetworkAdapter.LINK_PREFIX.length())));
    }

    private Id<Link> original(Id<Link> id) {
        return owns(id) ? Id.createLinkId(id.toString().substring(DsimBikeNetworkAdapter.LINK_PREFIX.length())) : id;
    }
}
