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
 *  Class  tasked with  routing the sharing trips(SMM-PT-SMM); it uses two previously established routing modules
 *
 *  Based on Azizte Diallos car-pt implementation
 */
public class SMMSharingPTSharingRoutingModule implements RoutingModule {
    private final RoutingModule sharingRoutingModule;
    private final Network network;
    private final Map<Id<TransitStopFacility>,TransitStopFacility> ptStops;

    // Create an object of a ptRoutingModule
    private final RoutingModule ptRoutingModule;


    public SMMSharingPTSharingRoutingModule(RoutingModule sharingRoutingModule, RoutingModule ptRoutingModule, Network network, Map<Id<TransitStopFacility>,TransitStopFacility> ptStops){

        this.sharingRoutingModule = sharingRoutingModule;
        this.ptRoutingModule = ptRoutingModule;
        this.network = network;
        this.ptStops=ptStops;
    }

    @Override
    public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {

            PTStationFinder ptFinder = new PTStationFinder(ptStops);

            Facility ptStop = ptFinder.getPTStation(person, fromFacility, network);
            Facility ptDestination=ptFinder.getPTStation(person,toFacility,network);
            // Createsn the  access trip
            List<? extends PlanElement> sharingAcces = sharingRoutingModule.calcRoute(fromFacility, ptStop, departureTime,
                    null);

            // double vehicleDistance = Double.NaN;
            double accessTravelTime = 0.0;
            // double price = Double.NaN;
            // Iterates through the sharing access route and calculate at which time would be at the PT stop
            for(int i=0; i<sharingAcces.size();i++ ){
                PlanElement accessStage=sharingAcces.get(i);
                if(accessStage instanceof Activity ){
                    Activity activitySharing=(Activity)accessStage;
                    accessTravelTime += activitySharing.getMaximumDuration().seconds();
                }
                else{
                    Leg leg=(Leg)accessStage;
                    accessTravelTime+=leg.getRoute().getTravelTime().seconds();
                }
            }

            double timeToAccessPt = 60; // 1 min to park SMM and access to PT

            double ptDepartureTime = departureTime + accessTravelTime + timeToAccessPt;

            // Creation of a PT trip from the PT stop to the destination
            List<? extends PlanElement> ptElements = ptRoutingModule.calcRoute(ptStop, ptDestination, ptDepartureTime,
                    person);

            // Creation interaction between SMM and pt
            Link prLink = NetworkUtils.getNearestLink(network, ptStop.getCoord());
            Activity interactionActivtySharingPT = PopulationUtils.createActivityFromCoordAndLinkId("SharingPT interaction",
                   ptStop.getCoord(), prLink.getId());
            interactionActivtySharingPT.setMaximumDuration(300);// 5 min
            PlanElement finalElement=ptElements.get(ptElements.size()-1);
            Leg legFinal=(Leg)finalElement;
            double egressDeparture=legFinal.getDepartureTime().seconds()+legFinal.getTravelTime().seconds();
             List<? extends PlanElement> sharingEgress = sharingRoutingModule.calcRoute(ptDestination, toFacility,egressDeparture,
                null);
             // Interaction SMM PT of egress
            Link egressLink = NetworkUtils.getNearestLink(network, ptDestination.getCoord());
            Activity interactionActivtyPTSharing = PopulationUtils.createActivityFromCoordAndLinkId("PTSharing interaction",
                    ptDestination.getCoord(), egressLink.getId());
            interactionActivtyPTSharing.setMaximumDuration(300);// 5 min
         // Creation full trip
            List<PlanElement> allElements = new LinkedList<>();
            allElements.addAll(sharingAcces);
            allElements.add(interactionActivtySharingPT);
            allElements.addAll(ptElements);
            allElements.add(interactionActivtyPTSharing);
            allElements.addAll(sharingEgress);

            return allElements;

    }



}




