package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.constraints;

import com.google.inject.Inject;

import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.constraints.SMMPTStationFinder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.sharing.routing.InteractionFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Class verifies the validity of a SMM trip
 */

public class SMMStationBasedConstraint extends AbstractTripConstraint {
    public static SMMPTStationFinder stationFinder;
    public static Scenario scenario;
    public static String name;
    public static String serviceScheme;
    private final InteractionFinder interactionFinder;

    @Inject
    public SMMStationBasedConstraint(SMMPTStationFinder stationFinder, Scenario scenario, String name, InteractionFinder interactionFinder) {
        this.stationFinder=stationFinder;
        this.scenario=scenario;
        this.name=name;
        this.interactionFinder=interactionFinder;
    }

    /**
     * Method evaluates if a multimodal trip using a station based SMM is possible by finding the closest PT Stations to origin and destination and finding drop off,
     * pick up stations of SMM near
     * @param trip trip representation of origin an destination
     * @param mode mode of the trip
     * @param previousModes
     * @return Boolean indicating if it the validity of trip
     */

    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        Boolean validation=true;
        Facility stationInitial = null;
        Facility finalStation = null;
        Facility originFacility= FacilitiesUtils.toFacility(trip.getOriginActivity(),scenario.getActivityFacilities());
        Facility destinationFacility=FacilitiesUtils.toFacility(trip.getDestinationActivity(),scenario.getActivityFacilities());

        if(mode.equals("PT_"+name)){
            finalStation = stationFinder.getPTStation(trip.getDestinationActivity(), scenario.getNetwork());
            if(interactionFinder.findDropoff(destinationFacility).isEmpty()||interactionFinder.findPickup(finalStation).isEmpty()){
                validation=false;
            }

        }
        if(mode.equals(name+"_PT") ){
            stationInitial = stationFinder.getPTStation(trip.getOriginActivity(), scenario.getNetwork());
            if(interactionFinder.findDropoff(stationInitial).isEmpty()||interactionFinder.findPickup(originFacility).isEmpty()){
                validation=false;
            }

        }
        if(mode.equals(name+"_PT_"+name)){
            finalStation = stationFinder.getPTStation(trip.getDestinationActivity(), scenario.getNetwork());
            stationInitial = stationFinder.getPTStation(trip.getOriginActivity(), scenario.getNetwork());
            if(interactionFinder.findDropoff(stationInitial).isEmpty()||interactionFinder.findPickup(originFacility).isEmpty()){
                validation=false;
            }
            if(interactionFinder.findDropoff(destinationFacility).isEmpty()||interactionFinder.findPickup(finalStation).isEmpty()){
                validation=false;
            }
        }

        if (mode.equals(name+"_PT") || mode.equals("PT_"+name)||mode.equals(name+"_PT_"+name)) {
            stationInitial = stationFinder.getPTStation(trip.getOriginActivity(), scenario.getNetwork());
            finalStation = stationFinder.getPTStation(trip.getDestinationActivity(), scenario.getNetwork());
            if (stationInitial.getCoord() == finalStation.getCoord()) {
                validation=false;
            }

        }


        return validation;
    }

    /**
     * Method estimates the validity of a SMM -PT trip  by making sure that there are PT legs in the trip elements
     * @param trip trip representation of origin an destination
     * @param candidate plan elements of the trip
     * @param previousCandidates
     * @return Boolean of validation
     */
    @Override
    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate, List<TripCandidate> previousCandidates){
        int counterPT=0;
        Boolean validation=true;
        DefaultRoutedTripCandidate candidate2= (DefaultRoutedTripCandidate) candidate;
        if (candidate.getMode().equals(name+"_PT")||candidate.getMode().equals("PT_"+name)||candidate.getMode().equals(name+"_PT_"+name)){
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
        }
        if(counterPT==0&&(candidate.getMode().equals(name+"_PT")||candidate.getMode().equals("PT_"+name)||candidate.getMode().equals(name+"_PT_"+name))){

            validation= false;}
        else{
            validation=true;}


        return validation;
    }
    public static class Factory implements TripConstraintFactory {
        private final SMMPTStationFinder stationFinder;
        private final Scenario scenario;
        private final String name;
        private final InteractionFinder interactionFinder;

        public Factory(SMMPTStationFinder stationFinder, Scenario scenario, String name, InteractionFinder interactionFinder) {
            this.stationFinder = stationFinder;
            this.scenario = scenario;
            this.name=name;
            this.interactionFinder = interactionFinder;
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new SMMStationBasedConstraint(stationFinder,scenario,name,interactionFinder);
        }
    }

}
