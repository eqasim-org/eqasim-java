package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.ile_de_france.PolicyParameters;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class IDFCarPTUtilityEstimator extends CarUtilityEstimator{

    UtilityEstimator carUtilityEstimator;
    UtilityEstimator ptUtilityEstimator;

    public IDFCarPTUtilityEstimator(ModeParameters parameters, CarPredictor predictor, 
        UtilityEstimator carUtilityEstimator, UtilityEstimator ptUtilityEstimator){
        super(parameters, predictor);
        this.carUtilityEstimator = carUtilityEstimator;
        this.ptUtilityEstimator = ptUtilityEstimator;


    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double utility = 0.0;
        utility += PolicyParameters.carPtUtilityOffset;

        final String direction = trip.getTripAttributes().getAttribute("car_pt").toString();

        if (direction == "ACCESS"){
            int carPtInteractionIndex = -1;
            for (int i = 0; i < elements.size(); i++){
                PlanElement element = elements.get(i);
                if (element instanceof Activity && ((Activity) element).getType().equals("car-pt interaction")){
                    carPtInteractionIndex = i;
                    break;
                }
            }
    
            if (carPtInteractionIndex != -1){
                List<? extends PlanElement> carElements = elements.subList(0, carPtInteractionIndex);
                List<? extends PlanElement> ptElements = elements.subList(carPtInteractionIndex + 1, elements.size());
    
                utility += carUtilityEstimator.estimateUtility(person, trip, carElements);
                utility += ptUtilityEstimator.estimateUtility(person, trip, ptElements);
            }
            else {
                utility += carUtilityEstimator.estimateUtility(person, trip, elements);
            }

        }
        else if (direction == "EGRESS"){
            int carPtInteractionIndex = -1;
            for (int i = 0; i < elements.size(); i++){
                PlanElement element = elements.get(i);
                if (element instanceof Activity && ((Activity) element).getType().equals("car-pt interaction")){
                    carPtInteractionIndex = i;
                    break;
                }
            }

            if (carPtInteractionIndex != -1){
                List<? extends PlanElement> ptElements = elements.subList(0, carPtInteractionIndex);
                List<? extends PlanElement> carElements = elements.subList(carPtInteractionIndex + 1, elements.size());

                utility += ptUtilityEstimator.estimateUtility(person, trip, ptElements);
                utility += carUtilityEstimator.estimateUtility(person, trip, carElements);
            }
            else {
                utility += ptUtilityEstimator.estimateUtility(person, trip, elements);
            }
        }
        else{
            throw new IllegalArgumentException("Invalid direction specified");
        }




        return utility;
    }

}
