package org.eqasim.core.components.traffic_light.delays.webster;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.ArrayList;
import java.util.List;

import java.util.*;

public class PropagateDelays {
    private final Network network;

    public PropagateDelays(Network network) {
        this.network = network;
    }

    public void propagateDelays(IdMap<Link, List<Double>> delays) {
        for (Link link : network.getLinks().values()) {
            Id<Link> linkId = link.getId();

            List<? extends Link> incomingCarLinks = getIncomingCarLinks(link.getFromNode());
            if (incomingCarLinks.size() == 1 && delays.containsKey(linkId)) {
                propagateLinkDelay(link, incomingCarLinks.get(0), delays);
            }
        }
    }

    private void propagateLinkDelay(Link currentLink, Link previousLink, IdMap<Link, List<Double>> delays) {
        List<Double> currentDelays = delays.get(currentLink.getId());

        if (currentDelays == null || currentDelays.isEmpty()) return;

        int size = currentDelays.size();
        List<Double> newCurrentDelays = new ArrayList<>(size);
        List<Double> newPreviousDelays = new ArrayList<>(size);

        double totalLength = previousLink.getLength() + currentLink.getLength();
        double previousLength = previousLink.getLength();
        double currentLength = currentLink.getLength();

        for (double delay : currentDelays) {
            double delayPerMeter = delay / totalLength;
            newPreviousDelays.add(delayPerMeter * previousLength);
            newCurrentDelays.add(delayPerMeter * currentLength);
        }

        delays.put(currentLink.getId(), newCurrentDelays);
        delays.put(previousLink.getId(), newPreviousDelays);
    }

    private List<? extends Link> getIncomingCarLinks(Node node) {
        return node.getInLinks().values().stream()
                .filter(link -> link.getAllowedModes().contains("car"))
                .toList();
    }
}

