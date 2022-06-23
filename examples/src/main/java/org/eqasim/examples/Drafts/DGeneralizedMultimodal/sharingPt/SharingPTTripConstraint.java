package org.eqasim.examples.Drafts.DGeneralizedMultimodal.sharingPt;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.facilities.Facility;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class
SharingPTTripConstraint extends AbstractTripConstraint {
    public static  PTStationFinder stationFinder;
    public static Scenario scenario;
    public static String name;
    public static String serviceScheme;
//        public void injectRoutingModule(   Map<String, Provider<RoutingModule>> routingModuleProviders) {
//
//
//           this.routingModules=routingModuleProviders.get("sharing:"+name).get();
//
//    }
    @Inject
    public SharingPTTripConstraint(PTStationFinder stationFinder, Scenario scenario,String name) {
        this.stationFinder=stationFinder;
        this.scenario=scenario;
        this.name=name;




    }
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
//        injectRoutingModule();
        Boolean validation=true;
        Facility stationInitial = null;
        Facility finalStation = null;
//          Facility originFacility=FacilitiesUtils.toFacility(trip.getOriginActivity(),scenario.getActivityFacilities());
//          Facility destinationFacility=FacilitiesUtils.toFacility(trip.getDestinationActivity(),scenario.getActivityFacilities());
//
//          if(mode.equals(mode.equals("PT_"+name)) ){
//              finalStation = stationFinder.getPTStation(trip.getDestinationActivity(), scenario.getNetwork());
//              if(interactionFinder.findDropoff(destinationFacility).isEmpty()||interactionFinder.findPickup(finalStation).isEmpty()){
//                  validation=false;
//              }
//
//          }
//        if(mode.equals(name+"_PT") ){
//            stationInitial = stationFinder.getPTStation(trip.getOriginActivity(), scenario.getNetwork());
//            if(interactionFinder.findDropoff(stationInitial).isEmpty()||interactionFinder.findPickup(originFacility).isEmpty()){
//                validation=false;
//            }
//
//        }
//         if(mode.equals(name+"_PT_"+name)){
//             finalStation = stationFinder.getPTStation(trip.getDestinationActivity(), scenario.getNetwork());
//             stationInitial = stationFinder.getPTStation(trip.getOriginActivity(), scenario.getNetwork());
//             if(interactionFinder.findDropoff(stationInitial).isEmpty()||interactionFinder.findPickup(originFacility).isEmpty()){
//                 validation=false;
//             }
//             if(interactionFinder.findDropoff(destinationFacility).isEmpty()||interactionFinder.findPickup(finalStation).isEmpty()){
//                 validation=false;
//             }
//         }

        if (mode.equals(name+"_PT") || mode.equals("PT_"+name)||mode.equals(name+"_PT_"+name)) {
            stationInitial = stationFinder.getPTStation(trip.getOriginActivity(), scenario.getNetwork());
            finalStation = stationFinder.getPTStation(trip.getDestinationActivity(), scenario.getNetwork());
            if (stationInitial.getCoord() == finalStation.getCoord()) {
                validation=false;
            }

        }

            return validation;
    }

    @Override
    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate, List<TripCandidate> previousCandidates){
        int counterPT=0;
        DefaultRoutedTripCandidate candidate2= (DefaultRoutedTripCandidate) candidate;
        if (candidate.getMode()==name+"_PT"||candidate.getMode()=="PT_"+name||candidate.getMode()==name+"_PT_"+name){
            List<? extends PlanElement>listLegs=candidate2.getRoutedPlanElements();
            Iterator phases=listLegs.iterator();
            while(phases.hasNext()){
                PlanElement element= (PlanElement) phases.next();
                if(element instanceof Leg){
                    Leg elementLeg=(Leg)element;
                    if(elementLeg.getMode()=="pt"){
                        counterPT++;
                    }

                }
            }
            if(counterPT>0){return true;}
            else{return false;}
        }
        else {
            return true;
        }
    }
    public static class Factory implements TripConstraintFactory {
        private final  PTStationFinder stationFinder;
       private final Scenario scenario;
       private final String name;
       //private final InteractionFinder interactionFinder;

        public Factory(PTStationFinder stationFinder, Scenario scenario, String name) {
            this.stationFinder = stationFinder;
            this.scenario = scenario;
            this.name=name;
           // this.interactionFinder = interactionFinder;
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new SharingPTTripConstraint(stationFinder,scenario,name);
        }
    }

}
