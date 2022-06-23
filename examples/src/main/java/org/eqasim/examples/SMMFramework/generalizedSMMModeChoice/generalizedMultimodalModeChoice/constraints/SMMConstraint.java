package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.constraints;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Class validates if a SMM trip is correct and is not overly long
 */
public class SMMConstraint extends AbstractTripConstraint {

    public static String mode;
   public static String name;

    @Inject
    public SMMConstraint(String mode, String name) {
        this.name=name;
        this.mode=mode;

    }


    @Override
    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate, List<TripCandidate> previousCandidates){
        int counterSMM=0;
        Double distanceTrip=0.0;
        // Calculates the number of SMM legs
        DefaultRoutedTripCandidate candidate2= (DefaultRoutedTripCandidate) candidate;
        if (candidate.getMode().equals("sharing:"+name)){
            List<? extends PlanElement>listLegs=candidate2.getRoutedPlanElements();
            Iterator phases=listLegs.iterator();
            while(phases.hasNext()){
                PlanElement element= (PlanElement) phases.next();
                if(element instanceof Leg){
                    Leg elementLeg=(Leg)element;
                    if(elementLeg.getMode().equals(mode)){
                        distanceTrip= elementLeg.getRoute().getDistance();
                        if(elementLeg.getTravelTime().seconds()>0){
                            counterSMM++;
                        }

                    }

                }
            }
        }
        // Limits eScooter trips to 3km

        if(mode=="eScooter"){
            if(counterSMM==0&&(candidate.getMode().equals("sharing:"+name))||distanceTrip>3000&&(candidate.getMode().equals("sharing:"+name))){

                return false;}
            else{
                return true;}
        }
        // Limits bikesharinh trips to 25km
        else{
        if(counterSMM==0&&(candidate.getMode().equals("sharing:"+name))||distanceTrip>25000&(candidate.getMode().equals("sharing:"+name))){

            return false;}
        else{
            return true;}
        }


    }
    public static class Factory implements TripConstraintFactory {
        private final  String mode;

       private final String name;


        public Factory(String mode,String name) {
            this.mode=mode;
            this.name=name;
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new SMMConstraint(mode,name);
        }
    }

}
