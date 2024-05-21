package org.eqasim.ile_de_france.policies;

import org.matsim.api.core.v01.network.Link;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.facilities.Facility;

import com.google.inject.Inject;
 
public class MyMultiModalLinkChooser implements MultimodalLinkChooser {

    private final Network carNetwork;

    @Inject
    public MyMultiModalLinkChooser(Network network) {
        this.carNetwork = NetworkUtils.createNetwork();
        new TransportModeNetworkFilter(network).filter(carNetwork, Set.of("car"));
		new NetworkCleaner().run(carNetwork);
    }


    @Override
    public Link decideOnLink(Facility facility, Network network) {

        Link accessActLink = null;
        Id<Link> accessActLinkId = null;

        try {
            accessActLinkId = facility.getLinkId();
        } catch (Exception ee){
            // do nothing
        }

        if (accessActLinkId != null) {
            accessActLink = this.carNetwork.getLinks().get(accessActLinkId);
        }

        if (accessActLink != null) {
            return accessActLink;
        }
        else {
            if( facility.getCoord()==null ) {
                throw new RuntimeException("link for facility cannot be determined when neither facility link id nor facility coordinate given") ;
            }
            //accessActLink = network.getLinks().get(NetworkUtils.getNearestLink(carNetwork, facility.getCoord()).getId());
            accessActLink = NetworkUtils.getNearestLink(this.carNetwork, facility.getCoord());
            Gbl.assertNotNull(accessActLink);
        }

        return accessActLink;
    }

}
