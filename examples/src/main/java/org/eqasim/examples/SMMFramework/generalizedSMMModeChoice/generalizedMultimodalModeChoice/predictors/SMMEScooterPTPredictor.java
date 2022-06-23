package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.predictors;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.costModels.SMMMultimodalCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SharingPTVariables;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;

import java.util.List;
import java.util.ListIterator;

public class SMMEScooterPTPredictor extends CachedVariablePredictor<SharingPTVariables> {
    private final SMMMultimodalCostModel multimodalCostModel;

    private final SMMCostParameters parameters;
    private final String mode;
    private final String underlyingMode;
    public SMMEScooterPTPredictor(SMMMultimodalCostModel multimodalCostModel, SMMCostParameters parameters, String name, String underlyingMode) {
        this.multimodalCostModel=multimodalCostModel;
        this.parameters = parameters;
        this.mode = name;
        this.underlyingMode = underlyingMode;
    }




    @Override
    public SharingPTVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        // Find the index in which changes modes by  interactions
        Integer accessIndex=findIndex(elements,"SharingPT interaction");

        // Splits the multimodal trip into its access,egress, pt segments
        List<? extends PlanElement> accessSplit=elements.subList(0,accessIndex);
        List<? extends PlanElement> ptSplit=elements.subList(accessIndex+1,elements.size());

        // Creates the discrete choice trip for each segment
        DiscreteModeChoiceTrip accessTrip = new DiscreteModeChoiceTrip(trip.getOriginActivity(), (Activity) elements.get(accessIndex), "sharing:"+mode,accessSplit,person.hashCode(),trip.hashCode(),0);
        DiscreteModeChoiceTrip ptTrip = new DiscreteModeChoiceTrip((Activity) elements.get(accessIndex),trip.getDestinationActivity(), "pt",ptSplit,person.hashCode(),trip.hashCode(),0);

        // Initializes the mode variables
        SharingPTVariables sharingPTVariables=new SharingPTVariables(0,0,0,0
                ,0.5,0,
                0,0,0,
                0.5,0,0,0,0,0);

        // Prediction of variables
        predictSharingAcces(sharingPTVariables,accessSplit,person,accessTrip);
        predictPTSplit(sharingPTVariables,ptSplit,person,ptTrip);

        return(sharingPTVariables);
    }
    public Integer findIndex( List<? extends PlanElement> elements, String actName){
       elements=(List<PlanElement>)elements;
       Integer index=100000;
        ListIterator<? extends PlanElement> iterator = elements.listIterator();
        while (iterator.hasNext()) {
            PlanElement element =(PlanElement)iterator.next();
            if(element instanceof Activity){
                if((((Activity) element).getType().equals(actName))){
                    index= elements.indexOf(element);
                    break;
                }
            }
        }
        return index;
    }

    public void predictSharingAcces(SharingPTVariables variables,List<? extends PlanElement> accessSplit, Person person,DiscreteModeChoiceTrip accessTrip){
        // double vehicleDistance = Double.NaN;
        double accessTravelTime = 0.0;
        // double price = Double.NaN;
        // Iterates through the sharing access route and calculate at which time would be at the PT stop
        for(int i=0; i<accessSplit.size();i++ ){
            PlanElement accessStage=accessSplit.get(i);
            if(accessStage instanceof Activity){
                Activity activitySharing=(Activity)accessStage;
                //Verify Type of ACtivity
                accessTravelTime += (activitySharing.getMaximumDuration().seconds());
            }
            else{
                Leg leg=(Leg)accessStage;
                // verify the mode
                accessTravelTime+=leg.getRoute().getTravelTime().seconds();
            }
        }
        int numberOfVehicularTrips = 0;
        boolean isFirstWaitingTime = true;
        double travelTime_min=0;
        // Track relevant variables
        double inVehicleTime_min = 0.0;
        double waitingTime_min = 0.0;
        double accessTime_min = 0.0;
        double egressTime_min = 0.0;

        for (int i = 0; i < accessSplit.size(); i++) {
            PlanElement element = accessSplit.get(i);

            if (element instanceof Leg) {
                Leg leg = (Leg) element;

                if (i < accessSplit.size() - 1) {
                    PlanElement nextElement = accessSplit.get(i + 1);
                    if (nextElement instanceof Activity) {
                        Activity nextActivity = (Activity) nextElement;
                        if (nextActivity.getType() .equals("sharing booking interaction")||nextActivity.getType() .equals("sharing pickup interaction")) {
                            if (leg.getMode() == "walk") {
                                accessTime_min += leg.getTravelTime().seconds() / 60.0;
                            }

                        }

                    }
                }
                if (leg.getMode().equals(underlyingMode)) {
                    travelTime_min=+leg.getRoute().getTravelTime().seconds()/60;

                }
                if (i > 0) {
                    PlanElement previous = accessSplit.get(i - 1);
                    if (previous instanceof Activity) {
                        Activity nextActivity = (Activity) previous;
                        if (nextActivity.getType() == "sharing dropoff interaction") {
                            if (leg.getMode() == "walk") {
                                egressTime_min += leg.getTravelTime().seconds() / 60.0;
                            }

                        }

                    }

                }


            }
        }
        // Calculate cost


        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(accessTrip);
        double cost_MU_bikeShare = multimodalCostModel.calculateCost_MU_Sharing(person,accessTrip,accessSplit);
        variables.setTravelTime_u_min(travelTime_min);
        variables.setAccess_Time_Sharing(accessTime_min);
        variables.setEgress_Time(egressTime_min);
        variables.setParkingTime_u_min_Sharing(2);
        variables.setCost_Sharing(cost_MU_bikeShare);
        variables.setFreeFloating(0);
        variables.setPedelec(0);
        variables.setBattery(100);
        variables.setAvailability(100);

    }
    public void predictPTSplit(SharingPTVariables variables,List<? extends PlanElement> ptSplit, Person person,DiscreteModeChoiceTrip ptTrip){
        int numberOfVehicularTripsPT = 0;
        boolean isFirstWaitingTimePT = true;

        // Track relevant variables
        double inVehicleTime_minPT = 0.0;
        double waitingTime_minPT = 0.0;
        double accessTime_minPT = 0.0;
        double egressTime_minPT=0.0;

        for (int i=0;i<ptSplit.size();i++) {
            PlanElement element = ptSplit.get(i);

            if (element instanceof Leg) {
                Leg leg = (Leg) element;

                if (i < ptSplit.size()-1) {
                    PlanElement nextElement = ptSplit.get(i + 1);
                    if (nextElement instanceof Activity) {
                        Activity nextActivity = (Activity) nextElement;
                        if (nextActivity.getType() == "pt interaction") {
                            if (leg.getMode() == "walk") {
                                accessTime_minPT += leg.getTravelTime().seconds() / 60.0;
                            }
                            if (leg.getMode() == "pt") {
                                TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
                                double departureTimePT = leg.getDepartureTime().seconds();
                                double waitingTimePT = route.getBoardingTime().seconds() - departureTimePT;
                                double inVehicleTimePT = leg.getTravelTime().seconds() - waitingTimePT;
                                inVehicleTime_minPT += inVehicleTimePT / 60.0;
                                numberOfVehicularTripsPT++;

                            }
                        }

                    }
                }
                if (i > 0) {
                    PlanElement previous = ptSplit.get(i - 1);
                    if (previous instanceof Activity) {
                        Activity nextActivity = (Activity) previous;
                        if (nextActivity.getType() == "pt interaction") {
                            if (leg.getMode() == "walk") {
                                egressTime_minPT += leg.getTravelTime().seconds() / 60.0;
                            }

                        }

                    }

                }



            }
        }
        // Calculate cost
        double cost_PT = multimodalCostModel.calculateCost_MU(person, ptTrip, ptSplit);
        int numberOfLineSwitchesPT = Math.max(0, numberOfVehicularTripsPT - 1);
        double euclideanDistance_kmPT = PredictorUtils.calculateEuclideanDistance_km(ptTrip);
        variables.setTravelTime_u_min(variables.travelTime_u_min);
        variables.setAccess_Time(accessTime_minPT);
        variables.setEgress_Time(egressTime_minPT);
        variables.setCost(cost_PT);
        variables.setCrowding(0.0);
        variables.setTransfers(numberOfLineSwitchesPT);

    }

    public void predictSharingEgress(SharingPTVariables variables,List<? extends PlanElement> egressSplit, Person person,DiscreteModeChoiceTrip egressTrip){

        Double travelTime_min=variables.getTravelTime_u_min();
        Double accessTime_min=variables.getAccess_Time_Sharing();
        Double egressTime_min=variables.getEgress_Time();
        Double cost_MU_bikeShare=variables.getCost_Sharing();

        int numberOfVehicularTripsE= 0;

        // iterate over all the tip sharingEgress
        for (int i = 0; i < egressSplit.size(); i++) {
            PlanElement element = egressSplit.get(i);
            // Grabs all legs of the plan
            if (element instanceof Leg) {
                Leg leg = (Leg) element;
                // In order to determine if it is access walk, it needs to know if the next activity is booking or pickup of vehicle
                if (i < egressSplit.size() - 1) {
                    PlanElement nextElement = egressSplit.get(i + 1);
                    if (nextElement instanceof Activity) {
                        Activity nextActivity = (Activity) nextElement;
                        if (nextActivity.getType() == "sharing booking interaction"||nextActivity.getType() == "sharing pickup interaction") {
                            if (leg.getMode() == "walk") {
                                accessTime_min += leg.getTravelTime().seconds() / 60.0;
                            }

                        }

                    }
                }
                // In order to determine if it is egress walk, it needs to know if the next activity is dropoff of vehicle
                if (i > 0) {
                    PlanElement previous = egressSplit.get(i - 1);
                    if (previous instanceof Activity) {
                        Activity nextActivity = (Activity) previous;
                        if (nextActivity.getType() == "sharing dropoff interaction") {
                            if (leg.getMode() == "walk") {
                                egressTime_min += leg.getTravelTime().seconds() / 60.0;
                            }

                        }

                    }

                }
                // Checks the travel time in Shared Vehicle
                if (leg.getMode().equals(underlyingMode)) {
                    travelTime_min=+leg.getRoute().getTravelTime().seconds()/60;

                }

            }
            // Analizes the  activities and adds their duration to the  access or egress time
            if (element instanceof Activity){
                Activity activity=(Activity)element;
                if(activity.getType().equals("sharing booking interaction")|| activity.getType().equals("sharing pickup interaction")){
                    accessTime_min += activity.getMaximumDuration().seconds()/60;

                }else{
                    egressTime_min+=activity.getMaximumDuration().seconds()/60;
                }

            }
        }
        // Calculate cost

//        DiscreteModeChoiceTrip egressTrip = new DiscreteModeChoiceTrip(ptSharing,trip.getDestinationActivity(), "sharing:bikeShare",sharingEgress,person.getPlans().hashCode(),0,0);
//         double euclideanDistance_kmE = PredictorUtils.calculateEuclideanDistance_km(egressTrip);
        cost_MU_bikeShare+=multimodalCostModel.calculateCost_MU_Sharing(person,egressTrip,egressSplit);
        variables.setTravelTime_u_min(travelTime_min);
        variables.setAccess_Time_Sharing(accessTime_min);
        variables.setEgress_Time(egressTime_min);
        variables.setCost_Sharing(cost_MU_bikeShare);

    }
}
