package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables.KraussEScooterVariables;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussEScooterPredictor extends CachedVariablePredictor<KraussEScooterVariables> {
    private CostModel costModel;
    double sharedEscooterSpeed = 4;// Proxy of 22kph
    double travelTime_min = 0.0;
    double accessTime_min = 0.0;
    double egressTime_min = 0.0;
    double detour_min = 0.0;
    double cost_MU = 0.0;
    double parkingTime_min = 1;// Proxy
    @Inject
    public KraussEScooterPredictor(@Named("sharing:eScooter") CostModel costModel) {
        this.costModel = costModel;
    }
    @Override
    public KraussEScooterVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        int numberOfVehicularTrips = 0;
        boolean isFirstWaitingTime = true;

        // Track relevant variables
        double inVehicleTime_min = 0.0;
        double waitingTime_min = 0.0;
        double accessTime_min = 0.0;
        double egressTime_min = 0.0;

        for (int i = 0; i <elements.size(); i++) {
            PlanElement element = elements.get(i);

            if (element instanceof Leg) {
                Leg leg = (Leg) element;

                if (i < elements.size() - 1) {
                    PlanElement nextElement = elements.get(i + 1);
                    if (nextElement instanceof Activity) {
                        Activity nextActivity = (Activity) nextElement;
                        if (nextActivity.getType() == "sharing booking interaction") {
                            if (leg.getMode() == "walk") {
                                accessTime_min += leg.getTravelTime().seconds() / 60.0;
                            }
                            if (leg.getMode() == "eScooter") {
                                travelTime_min=+leg.getRoute().getTravelTime().seconds()/60;

                            }
                        }

                    }
                }
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


            }
        }
        // Calculate cost


        double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
        cost_MU=costModel.calculateCost_MU(person,trip,elements);
        return new KraussEScooterVariables(travelTime_min,accessTime_min,egressTime_min,0,0,0,0,cost_MU);
    }
}
