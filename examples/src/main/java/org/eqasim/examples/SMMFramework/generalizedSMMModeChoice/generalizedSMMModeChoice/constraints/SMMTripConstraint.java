package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.constraints;


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
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SMMTripConstraint extends AbstractTripConstraint {
    public static SMMPTStationFinder stationFinder;
    public static Scenario scenario;
    public static String name;
    public static String serviceScheme;

    public TransitRouter transitRouter;




    public SMMTripConstraint(SMMPTStationFinder stationFinder, Scenario scenario, String name) {
        this.stationFinder=stationFinder;
        this.scenario=scenario;
        this.name=name;




    }
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {

        Boolean validation=true;
        Facility stationInitial = null;
        Facility finalStation = null;


        if (mode.equals(name+"_PT") || mode.equals("PT_"+name)||mode.equals(name+"_PT_"+name)) {
            stationInitial = stationFinder.getPTStation(trip.getOriginActivity(), scenario.getNetwork());
            finalStation = stationFinder.getPTStation(trip.getDestinationActivity(), scenario.getNetwork());
            if (CoordUtils.calcEuclideanDistance(stationInitial.getCoord(), finalStation.getCoord())<1000) {
                validation=false;
            }

        }


        return validation;
    }

    @Override
    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate, List<TripCandidate> previousCandidates){
        int counterPT=0;
        String mode="";
        DefaultRoutedTripCandidate candidate2= (DefaultRoutedTripCandidate) candidate;
        Double distanceTrip=0.0;
        if (candidate.getMode().equals(name+"_PT")||candidate.getMode().equals("PT_"+name)||candidate.getMode().equals(name+"_PT_"+name)){
            List<? extends PlanElement>listLegs=candidate2.getRoutedPlanElements();
            Iterator phases=listLegs.iterator();
            while(phases.hasNext()){
                PlanElement element= (PlanElement) phases.next();
                if(element instanceof Leg){
                    Leg elementLeg=(Leg)element;
                    if(elementLeg.getMode()=="pt"){
                        counterPT++;
                    }else if(elementLeg.getMode().equals("eScooter")||elementLeg.getMode().equals("Shared-Bike")){
                        distanceTrip= elementLeg.getRoute().getDistance();
                        mode=elementLeg.getMode();
                    }

                }
            }

        }
        if(counterPT==0&&(candidate.getMode().equals(name+"_PT")||candidate.getMode().equals("PT_"+name)||candidate.getMode().equals(name+"_PT_"+name))) {
            if (mode == "eScooter") {
                if (distanceTrip > 3000) {

                    return false;
                } else {
                    return true;
                }
            } else {
                if (distanceTrip > 25000) {

                    return false;
                } else {
                    return true;
                }
            }
        }
        else{
            return true;}

    }
    public static class Factory implements TripConstraintFactory {
        private final SMMPTStationFinder stationFinder;
        private final Scenario scenario;
        private final String name;
        //private final InteractionFinder interactionFinder;

        public Factory(SMMPTStationFinder stationFinder, Scenario scenario, String name) {
            this.stationFinder = stationFinder;
            this.scenario = scenario;
            this.name=name;

        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new SMMTripConstraint(stationFinder,scenario,name);
        }
    }

}
