package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussPTVariables;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;

import java.util.List;

public class KraussPTPredictor extends CachedVariablePredictor<KraussPTVariables> {
    private CostModel costModel;
    @Inject
    public KraussPTPredictor(@Named("pt") CostModel costModel) {
        this.costModel = costModel;
    }


    @Override
    public KraussPTVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        int numberOfVehicularTrips = 0;
        boolean isFirstWaitingTime = true;

        // Track relevant variables
        double inVehicleTime_min = 0.0;
        double waitingTime_min = 0.0;
        double accessTime_min = 0.0;
        double egressTime_min=0.0;
        if(elements.size()>2){
            String u="W";
        }

        for (int i=0;i<elements.size();i++) {
            PlanElement element = elements.get(i);

            if (element instanceof Leg) {
                Leg leg = (Leg) element;

                if (i < elements.size()-1) {
                    PlanElement nextElement = elements.get(i + 1);
                    if (nextElement instanceof Activity) {
                        Activity nextActivity = (Activity) nextElement;
                        if (nextActivity.getType() == "pt interaction") {
                            if (leg.getMode() == "walk") {
                                accessTime_min += leg.getTravelTime().seconds() / 60.0;
                            }

                            if (leg.getMode() == "pt") {
                                TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();
                                double departureTime = leg.getDepartureTime().seconds();
                                double waitingTime = route.getBoardingTime().seconds() - departureTime;
                                double inVehicleTime = leg.getTravelTime().seconds() - waitingTime;
                                inVehicleTime_min += inVehicleTime / 60.0;
                                numberOfVehicularTrips++;

                            }
                        }

                    }
                }
                if (i > 0) {
                    PlanElement previous = elements.get(i - 1);
                    if (previous instanceof Activity) {
                        Activity nextActivity = (Activity) previous;
                        if (nextActivity.getType() == "pt interaction") {
                            if (leg.getMode() == "walk") {
                                egressTime_min += leg.getTravelTime().seconds() / 60.0;
                            }

                        }

                    }

                }



            }
        }
        // Calculate cost
        double cost_CHF = costModel.calculateCost_MU(person, trip, elements);
        int numberOfLineSwitches = Math.max(0, numberOfVehicularTrips - 1);
        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

        return new KraussPTVariables(inVehicleTime_min,accessTime_min,egressTime_min,cost_CHF,0.5,numberOfLineSwitches);
    }
}
