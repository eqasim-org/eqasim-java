package org.eqasim.examples.Drafts.DGeneralizedMultimodal.sharingPt;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SharingPTVariables;
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

public class PTSharingPredictor extends CachedVariablePredictor<SharingPTVariables> {
    @Inject
    public PTSharingPredictor(@Named("sharing:bikeShare")RoutingModule accessRoutingMode, @Named("pt")RoutingModule ptRoutingModule,
                              @Named("sharing:bikeShare")CostModel sharingCostModel, @Named("pt") CostModel ptCostModel, ModeParameters parameters,
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
        Facility ptStop = ptStationFinder.getPTStation(person,trip.getDestinationActivity(), network);

        // Createsn the  access trip
        Link fromLink = NetworkUtils.getNearestLink(network, trip.getDestinationActivity().getCoord());
        Facility fromFacility = new LinkWrapperFacility(fromLink);
        // reeplace DepTime
        List<? extends PlanElement> ptElements = ptRoutingModule.calcRoute(fromFacility, ptStop, trip.getDepartureTime(),
                person);
        Activity sharing_pt= (Activity) populationFactory.createActivityFromCoord("sharing_pt interaction",
                ptStop.getCoord());
        List<? extends PlanElement> initialElements=null;
        DiscreteModeChoiceTrip ptTrip = new DiscreteModeChoiceTrip(trip.getOriginActivity(), sharing_pt, "pt",ptElements,person.hashCode(),trip.hashCode(),0);
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
        double cost_PT = ptCostModel.calculateCost_MU(person, ptTrip, elements);
        int numberOfLineSwitchesPT = Math.max(0, numberOfVehicularTripsPT - 1);
        double euclideanDistance_kmPT = PredictorUtils.calculateEuclideanDistance_km(ptTrip);
/// For sharing
        Link toLink = NetworkUtils.getNearestLink(network, trip.getDestinationActivity().getCoord());
        Facility toFacility = new LinkWrapperFacility(fromLink);

        Double sharingTime=trip.getDepartureTime()+accessTime_minPT+inVehicleTime_minPT+waitingTime_minPT;
        List<? extends PlanElement> sharingEgress = accessRoutingMode.calcRoute(ptStop,toFacility,sharingTime,
                null);
        DiscreteModeChoiceTrip egressTrip = new DiscreteModeChoiceTrip(sharing_pt,trip.getDestinationActivity(), "sharing:bikeShare",sharingEgress,person.hashCode(),trip.hashCode(),0);
        // predict for Bike Share:
        int numberOfVehicularTrips = 0;
        boolean isFirstWaitingTime = true;
        double travelTime_min=0;
        double inVehicleTime_min = 0.0;
        double waitingTime_min = 0.0;
        double accessTime_min = 0.0;
        double egressTime_min = 0.0;
        // iterate over all the tip sharingEgress
        for (int i = 0; i < sharingEgress.size(); i++) {
            PlanElement element = sharingEgress.get(i);
            // Grabs all legs of the plan
            if (element instanceof Leg) {
                Leg leg = (Leg) element;
                // In order to determine if it is access walk, it needs to know if the next activity is booking or pickup of vehicle
                if (i < sharingEgress.size() - 1) {
                    PlanElement nextElement = sharingEgress.get(i + 1);
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
                    PlanElement previous = sharingEgress.get(i - 1);
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


        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(egressTrip);
        double cost_MU_bikeShare =sharingCostModel.calculateCost_MU(person,egressTrip,sharingEgress);


        SharingPTVariables sharingPTVariables=new SharingPTVariables(inVehicleTime_minPT,accessTime_minPT,egressTime_minPT,cost_PT,0.5,numberOfLineSwitchesPT,travelTime_min,accessTime_min,egressTime_min,0.5,0,cost_MU_bikeShare,0,0,0);
        return(sharingPTVariables);
    }
}
