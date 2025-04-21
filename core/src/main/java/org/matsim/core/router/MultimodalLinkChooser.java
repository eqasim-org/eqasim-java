package org.matsim.core.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public interface MultimodalLinkChooser {
    public Link decideAccessLink(RoutingRequest request, Network network, String mode);
    public Link decideEgressLink(RoutingRequest request, Network network, String mode);
}
