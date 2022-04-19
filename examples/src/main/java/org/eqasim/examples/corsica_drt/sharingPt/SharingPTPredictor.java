package org.eqasim.examples.corsica_drt.sharingPt;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;

import java.util.List;

public class SharingPTPredictor extends CachedVariablePredictor<SharingPTVariables> {
    @Inject
    public SharingPTPredictor(@Named("sharing:bikeShare")RoutingModule accessRoutingMode, @Named("pt")RoutingModule ptRoutingModule,
                              @Named("sharing:bikeShare")CostModel sharingCostModel,@Named("pt") CostModel ptCostModel, ModeParameters parameters,
                              Network network, PopulationFactory populationFactory, PTStationFinder ptStationFinder) {
        this.accessRoutingMode = accessRoutingMode;
        this.ptRoutingModule = ptRoutingModule;
        this.sharingCostModel = sharingCostModel;
        this.ptCostModel = ptCostModel;
        this.parameters = parameters;
        this.network = network;
        this.populationFactory = populationFactory;
        this.ptStationFinder = ptStationFinder;
    }

    private final RoutingModule accessRoutingMode;
    private final RoutingModule ptRoutingModule;
    private final CostModel sharingCostModel;
    private final CostModel ptCostModel;
    private final ModeParameters parameters;
    // private final List<Coord> parkRideCoords;
    private final Network network;
    private final PopulationFactory populationFactory;
    private final PTStationFinder ptStationFinder;

    @Override
    protected SharingPTVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        // Need to add to the finder based on activity
        Facility ptStop = ptStationFinder.getPTStation(person, trip.getOriginActivity() , network);

        // Createsn the  access trip
        Link fromLink = NetworkUtils.getNearestLink(network, trip.getOriginActivity().getCoord());
        Facility fromFacility = new LinkWrapperFacility(fromLink);
        List<? extends PlanElement> sharingAcces = accessRoutingMode.calcRoute(fromFacility, ptStop, trip.getDepartureTime(),
                null);

        // double vehicleDistance = Double.NaN;
        double accessTravelTime = 0.0;
        // double price = Double.NaN;
        // Iterates through the sharing access route and calculate at which time would be at the PT stop
        for(int i=0; i<sharingAcces.size();i++ ){
            PlanElement accessStage=sharingAcces.get(i);
            if(accessStage instanceof Activity){
                Activity activitySharing=(Activity)accessStage;
                accessTravelTime += (activitySharing.getMaximumDuration().seconds());
            }
            else{
                Leg leg=(Leg)accessStage;
                accessTravelTime+=leg.getRoute().getTravelTime().seconds();
            }
        }
        Leg travelLeg= (Leg) sharingAcces.get(sharingAcces.size()-1);
        Activity sharing_pt= (Activity) populationFactory.createActivityFromCoord("sharing_pt interaction",
                ptStop.getCoord());
        sharing_pt.setMaximumDuration(60);// 10 min
        sharing_pt.setLinkId(ptStop.getLinkId());
        // Given the request time, we can calculate the waiting time
        // asume that we take1 min to walk from the street to the parking
        double timeToAccessPt = 60; // We take 10 min to park the car and access to PT
        List<? extends PlanElement> initialElements=null;
        DiscreteModeChoiceTrip accessTrip = new DiscreteModeChoiceTrip(trip.getOriginActivity(),sharing_pt, "sharing:bikeShare",sharingAcces,person.hashCode(),trip.hashCode(),0);
        // predict for Bike Share:

        int numberOfVehicularTrips = 0;
        boolean isFirstWaitingTime = true;
        double travelTime_min=0;
        double inVehicleTime_min = 0.0;
        double waitingTime_min = 0.0;
        double accessTime_min = 0.0;
        double egressTime_min = 0.0;
        // iterate over all the tip elements
        for (int i = 0; i < elements.size(); i++) {
            PlanElement element = elements.get(i);
            // Grabs all legs of the plan
            if (element instanceof Leg) {
                Leg leg = (Leg) element;
                // In order to determine if it is access walk, it needs to know if the next activity is booking or pickup of vehicle
                if (i < elements.size() - 1) {
                    PlanElement nextElement = elements.get(i + 1);
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
                    PlanElement previous = elements.get(i - 1);
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
                if (leg.getMode() == "bike") {
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


        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(accessTrip);
        double cost_MU_bikeShare =sharingCostModel.calculateCost_MU(person,accessTrip,sharingAcces);
/// Predictor for PT
        double ptDepartureTime = trip.getDepartureTime() + accessTravelTime + timeToAccessPt;
        Link toLink = NetworkUtils.getNearestLink(network, trip.getDestinationActivity().getCoord());
        Facility toFacility = new LinkWrapperFacility(toLink);
        // Creation of a PT trip from the PR facility to the destination
        List<? extends PlanElement> ptElements = ptRoutingModule.calcRoute(ptStop, toFacility, ptDepartureTime,person);
        DiscreteModeChoiceTrip ptTrip = new DiscreteModeChoiceTrip(sharing_pt, trip.getDestinationActivity(), "pt",ptElements,person.hashCode(),trip.hashCode(),0);
        int numberOfVehicularTripsPT = 0;
        boolean isFirstWaitingTimePT = true;

        // Track relevant variables
        double inVehicleTime_minPT = 0.0;
        double waitingTime_minPT = 0.0;
        double accessTime_minPT = 0.0;
        double egressTime_minPT=0.0;

        for (int i=0;i<ptElements.size();i++) {
            PlanElement element = ptElements.get(i);

            if (element instanceof Leg) {
                Leg leg = (Leg) element;

                if (i < ptElements.size()-1) {
                    PlanElement nextElement = ptElements.get(i + 1);
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
                    PlanElement previous = ptElements.get(i - 1);
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
        double cost_PT = ptCostModel.calculateCost_MU(person, ptTrip, elements);
        int numberOfLineSwitchesPT = Math.max(0, numberOfVehicularTripsPT - 1);
        double euclideanDistance_kmPT = PredictorUtils.calculateEuclideanDistance_km(ptTrip);
        SharingPTVariables sharingPTVariables=new SharingPTVariables(inVehicleTime_minPT,accessTime_minPT,egressTime_minPT,cost_PT,0.5,numberOfLineSwitchesPT,travelTime_min,accessTime_min,egressTime_min,0.5,0,cost_MU_bikeShare,0,0,0);
        return(sharingPTVariables);
    }
}
