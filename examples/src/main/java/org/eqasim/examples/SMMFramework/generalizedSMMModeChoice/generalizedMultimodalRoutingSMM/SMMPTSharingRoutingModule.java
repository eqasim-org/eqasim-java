package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalRoutingSMM;

import org.eqasim.examples.corsica_drt.Drafts.DGeneralizedMultimodal.sharingPt.PTStationFinder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *  Class  tasked with  routing the sharing trips(PT-SMM); it uses two previously established routing modules
 *
 *  Based on Azizte Diallos car-pt implementation
 */
public class SMMPTSharingRoutingModule implements RoutingModule {
    private final RoutingModule sharingRoutingModule;
    private final Network network;
    private final Map<Id<TransitStopFacility>,TransitStopFacility> ptStops;

    // Create an object of a ptRoutingModule
    private final RoutingModule ptRoutingModule;


    public SMMPTSharingRoutingModule(RoutingModule sharingRoutingModule, RoutingModule ptRoutingModule, Network network, Map<Id<TransitStopFacility>,TransitStopFacility> ptStops){

        this.sharingRoutingModule = sharingRoutingModule;
        this.ptRoutingModule = ptRoutingModule;
        this.network = network;
        this.ptStops=ptStops;
    }

    @Override
    public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
        PTStationFinder ptFinder = new PTStationFinder(ptStops);
        Facility ptStop = ptFinder.getPTStation(person, toFacility, network);
//         Creation of a PT trip from the PR facility to the destination
        List<? extends PlanElement> ptElements = ptRoutingModule.calcRoute(fromFacility, ptStop, departureTime,
                    person);
        // Creation interaction between SMM and pt
            Link prLink = NetworkUtils.getNearestLink(network, ptStop.getCoord());
            Activity interactionActivtyPTSharing = PopulationUtils.createActivityFromCoordAndLinkId("PTSharing interaction",
                    ptStop.getCoord(), prLink.getId());
            interactionActivtyPTSharing.setMaximumDuration(300);// 5 min

//         departure egress
            double vehicleDistance = Double.NaN;
            double ptTravelTime = 0.0;
            // double price = Double.NaN;
            // Iterates through the sharing access route and calculate at which time would be at the PT stop
            for(int i=0; i<ptElements.size();i++ ){
                PlanElement ptStage=ptElements.get(i);
                if(ptStage instanceof Leg){
                    Leg leg=(Leg)ptStage;
                    ptTravelTime+=leg.getRoute().getTravelTime().seconds();
                }
            }
            double timeToAccessPt = 60; //  1 min to park the SMM and access to PT

            double egressDepartureTime = departureTime + ptTravelTime + timeToAccessPt;

            // Creates the  egress trip
            List<? extends PlanElement> sharingEgress = sharingRoutingModule.calcRoute(ptStop,toFacility, egressDepartureTime,
                    person);


        List<PlanElement> allElements = new LinkedList<>();

            allElements.addAll(ptElements);
            allElements.add(interactionActivtyPTSharing);
            allElements.addAll(sharingEgress);



            return allElements;

    }
    public int findPTInteraction( List<? extends PlanElement> ptElements){
        int index= 1000000000;
        for(int i=ptElements.size()-1;i>=0;i--){
            PlanElement element=ptElements.get(i);
            if(element instanceof Activity){
                Activity act=(Activity) element;
                if(act.getType().equals("pt interaction")){
                    index=i;
                    break;
                }
            }
        }
        return index;
    }


}




