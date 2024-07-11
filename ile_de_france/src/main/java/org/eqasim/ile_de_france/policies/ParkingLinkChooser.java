package org.eqasim.ile_de_france.policies;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.facilities.Facility;
import com.google.inject.Inject;
/**
 * MultimodalLinkChooser implementation that takes
 * into account parking link choice
 * A parking network is created from the car network
 * Agents look for parking in car network and choose
 * a random parking link from closest parking links
 * 
 * @author akramelb
 */
public class ParkingLinkChooser implements MultimodalLinkChooser{

    private final ParkingAssignment parkingAssignment;

    @Inject
    public ParkingLinkChooser(ParkingAssignment parkingAssignment){
        this.parkingAssignment = parkingAssignment;
    }


    @Override
    public Link decideOnLink(Facility facility, Network network) {

        Link accessActLink = null;
        
        if(facility.getCoord()==null ) {
                throw new RuntimeException("link for facility cannot be determined when neither facility link id nor facility coordinate given") ;
        }

        accessActLink = parkingAssignment.getNearestParkingLink(facility.getCoord());
        Gbl.assertNotNull(accessActLink);

        return accessActLink;
    }

    @Override
    public Link decideOnLink(Facility facility, Network network, Person person) {

        Link accessActLink = null;
        
        if(facility.getCoord()==null ) {
                throw new RuntimeException("link for facility cannot be determined when neither facility link id nor facility coordinate given") ;
        }

        accessActLink = parkingAssignment.getNearestParkingLinkforPerson(person, facility.getCoord());
        Gbl.assertNotNull(accessActLink);

        return accessActLink;
    }

    public Link decideOnLink(Facility facility, Person person){
        Link accessActLink = null;

        if(facility.getCoord()==null ) {
                throw new RuntimeException("link for facility cannot be determined when neither facility link id nor facility coordinate given") ;
        }

        accessActLink = parkingAssignment.getNearestParkingLinkforPerson(person, facility.getCoord());
        Gbl.assertNotNull(accessActLink);

        return accessActLink;
    }
}
