package org.matsim.core.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;

/**
 * New decideOnLink signature with person parameter
 * 
 * @author akramelb
 */
public interface MultimodalLinkChooser {

    public Link decideOnLink( final Facility facility, final Network network );

    default public Link decideOnLink( final Facility facility , final Network network, final Person person) {
        return decideOnLink( facility, network );
    }
}
