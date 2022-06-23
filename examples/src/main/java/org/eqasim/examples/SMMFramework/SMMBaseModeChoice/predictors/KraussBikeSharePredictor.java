package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussBikeShareVariables;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussBikeSharePredictor extends CachedVariablePredictor<KraussBikeShareVariables> {
    private CostModel costModel;
    double sharedBikeSpeed = 6.11;// Proxy of 22kph
    double travelTime_min = 0.0;
    double accessTime_min = 0.0;
    double egressTime_min = 0.0;
    double detour_min = 0.0;
    double cost_MU = 0.0;
    double parkingTime_min = 1;// Proxy


    @Inject
    public KraussBikeSharePredictor(@Named("sharing:bikeShare") CostModel costModel) {
        this.costModel = costModel;
    }
    public KraussBikeSharePredictor(CostModel costModel,String name) {
        this.costModel = costModel;
    }
    @Override
    public KraussBikeShareVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        int numberOfVehicularTrips = 0;
        boolean isFirstWaitingTime = true;

        // Track relevant variables
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

        double cost=costModel.calculateCost_MU(person,trip,elements);
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        return new KraussBikeShareVariables(travelTime_min, accessTime_min, egressTime_min, 0, 0, 0, 0, 0,cost);
    }
}
