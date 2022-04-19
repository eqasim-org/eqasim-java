package org.eqasim.examples.corsica_drt.sharingPt;

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

public class SharingPTTripConstraint extends AbstractTripConstraint {
    public static  PTStationFinder stationFinder;
    public static Scenario scenario;

    public SharingPTTripConstraint(PTStationFinder stationFinder, Scenario scenario) {
        this.stationFinder=stationFinder;
        this.scenario=scenario;
    }
    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {

        Facility stationInitial = null;
        Facility finalStation = null;
        if (mode.equals("Sharing_PT") || mode.equals("PT_Sharing")) {
            stationInitial = stationFinder.getPTStation(trip.getOriginActivity(), scenario.getNetwork());
            finalStation = stationFinder.getPTStation(trip.getDestinationActivity(), scenario.getNetwork());
            if (stationInitial.getCoord() == finalStation.getCoord()) {
                return false;
            }

        }

            return true;
    }

    @Override
    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate, List<TripCandidate> previousCandidates){
        int counterPT=0;
        DefaultRoutedTripCandidate candidate2= (DefaultRoutedTripCandidate) candidate;
        if (candidate.getMode()=="PT_Sharing"||candidate.getMode()=="Sharing_PT"||candidate.getMode()=="Sharing_PT_Sharing"){
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

        public Factory( PTStationFinder stationFinder, Scenario scenario) {
            this.stationFinder = stationFinder;
            this.scenario = scenario;
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new SharingPTTripConstraint(stationFinder,scenario);
        }
    }

}
