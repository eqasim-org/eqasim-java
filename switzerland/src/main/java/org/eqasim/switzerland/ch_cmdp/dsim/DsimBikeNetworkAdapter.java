package org.eqasim.switzerland.ch_cmdp.dsim;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;

/** Builds an isolated, bike-only copy of the bicycle network inside the scenario network. */
public final class DsimBikeNetworkAdapter {
    private static final Logger log = LogManager.getLogger(DsimBikeNetworkAdapter.class);
    static final String NODE_PREFIX = "__dsim_bike_node__";
    static final String LINK_PREFIX = "__dsim_bike_link__";
    private static final String MARKER = "eqasim:dsimBikeNetworkLayer";
    private static final Set<String> ROUTING_ATTRIBUTES = Set.of(
            "osm:way:highway", "osm:way:name", "traffic_light", "crossingPenalty");

    private DsimBikeNetworkAdapter() { }

    public record Result(int nodes, int links, boolean reused) { }

    public static Result adapt(Network network) {
        boolean marked = Boolean.parseBoolean(String.valueOf(network.getAttributes().getAttribute(MARKER)));
        var existingLinks = network.getLinks().values().stream()
                .filter(link -> link.getId().toString().startsWith(LINK_PREFIX)).map(link -> (Link) link).toList();
        var existingNodes = network.getNodes().values().stream()
                .filter(node -> node.getId().toString().startsWith(NODE_PREFIX)).map(node -> (Node) node).toList();
        if (marked) {
            // Existing layers belong to the input. Attribute cleanup has its own reversible archive.
            validateExistingLayer(network, existingNodes, existingLinks);
            log.info("Reusing existing DSim bicycle layer with {} nodes and {} links", existingNodes.size(), existingLinks.size());
            return new Result(existingNodes.size(), existingLinks.size(), true);
        }
        if (!existingLinks.isEmpty() || !existingNodes.isEmpty()) {
            throw new IllegalArgumentException("Network contains reserved DSim bicycle IDs but has no " + MARKER
                    + " marker; reserved prefixes are " + NODE_PREFIX + " and " + LINK_PREFIX);
        }

        List<Link> sources = network.getLinks().values().stream()
                .filter(link -> link.getAllowedModes().contains(TransportMode.bike)).map(link -> (Link) link).toList();
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("--" + DsimIntegration.SIMULATE_BIKES_OPTION
                    + " was requested, but the input network has no links allowing bike");
        }

        var nodeCopies = new LinkedHashMap<Id<Node>, Node>();
        for (Link source : sources) {
            copyNode(network, source.getFromNode(), nodeCopies);
            copyNode(network, source.getToNode(), nodeCopies);
        }

        var linkCopies = new LinkedHashMap<Id<Link>, Link>();
        for (Link source : sources) {
            Id<Link> copyId = bikeLinkId(source.getId());
            Link copy = network.getFactory().createLink(copyId,
                    nodeCopies.get(source.getFromNode().getId()), nodeCopies.get(source.getToNode().getId()));
            copy.setAllowedModes(Set.of(TransportMode.bike));
            copy.setCapacity(source.getCapacity());
            copy.setFreespeed(source.getFreespeed());
            copy.setLength(source.getLength());
            copy.setNumberOfLanes(source.getNumberOfLanes());
            copyRoutingAttributes(source, copy);
            network.addLink(copy);
            linkCopies.put(source.getId(), copy);
        }

        for (Link source : sources) {
            var modes = new LinkedHashSet<>(source.getAllowedModes());
            modes.remove(TransportMode.bike);
            source.setAllowedModes(modes);
            removeOriginalBikeRestrictions(source);
        }
        network.getAttributes().putAttribute(MARKER, Boolean.TRUE.toString());
        log.warn("Created isolated DSim bicycle layer: {} nodes and {} links, without turn restrictions. "
                + "Bike vehicles do not share links or nodes with road vehicles.", nodeCopies.size(), linkCopies.size());
        return new Result(nodeCopies.size(), linkCopies.size(), false);
    }

    public static boolean isBikeLink(Id<Link> id) {
        return id != null && id.toString().startsWith(LINK_PREFIX);
    }

    private static void copyNode(Network network, Node source, Map<Id<Node>, Node> copies) {
        copies.computeIfAbsent(source.getId(), ignored -> {
            Node copy = network.getFactory().createNode(bikeNodeId(source.getId()), source.getCoord());
            network.addNode(copy);
            return copy;
        });
    }

    private static void copyRoutingAttributes(Link source, Link copy) {
        for (String key : ROUTING_ATTRIBUTES) {
            Object value = source.getAttributes().getAttribute(key);
            if (value != null) copy.getAttributes().putAttribute(key, value);
        }
    }

    private static void removeOriginalBikeRestrictions(Link link) {
        DisallowedNextLinks restrictions = NetworkUtils.getDisallowedNextLinks(link);
        if (restrictions == null) return;
        restrictions.removeDisallowedLinkSequences(TransportMode.bike);
        if (restrictions.isEmpty()) NetworkUtils.removeDisallowedNextLinks(link);
    }

    private static void validateExistingLayer(Network network, List<Node> nodes, List<Link> links) {
        if (nodes.isEmpty() || links.isEmpty()) {
            throw new IllegalArgumentException("Incomplete existing DSim bicycle layer");
        }
        for (Link link : links) {
            if (!link.getAllowedModes().equals(Set.of(TransportMode.bike))
                    || !link.getFromNode().getId().toString().startsWith(NODE_PREFIX)
                    || !link.getToNode().getId().toString().startsWith(NODE_PREFIX)) {
                throw new IllegalArgumentException("Invalid existing DSim bicycle link " + link.getId());
            }
            if (NetworkUtils.getDisallowedNextLinks(link) != null) {
                throw new IllegalArgumentException("DSim bicycle link has a turn restriction " + link.getId());
            }
        }
        boolean bikeOnRoad = network.getLinks().values().stream()
                .anyMatch(link -> !isBikeLink(link.getId()) && link.getAllowedModes().contains(TransportMode.bike));
        if (bikeOnRoad) throw new IllegalArgumentException("Existing DSim bicycle layer is marked, but road links still allow bike");
    }

    private static Id<Node> bikeNodeId(Id<Node> source) { return Id.createNodeId(NODE_PREFIX + source); }
    private static Id<Link> bikeLinkId(Id<Link> source) { return Id.createLinkId(LINK_PREFIX + source); }
}
