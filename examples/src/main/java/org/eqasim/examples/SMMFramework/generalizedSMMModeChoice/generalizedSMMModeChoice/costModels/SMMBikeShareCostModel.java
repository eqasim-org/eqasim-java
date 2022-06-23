package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.costModels;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class SMMBikeShareCostModel extends AbstractCostModel {
    private final SMMCostParameters parameters;

    public SMMBikeShareCostModel(String mode , SMMCostParameters parameters) {
        super("sharing:"+mode);
        this.parameters=parameters;
    }

    protected double getInVehicleTime_min(List<? extends PlanElement> elements){
        double inVehicleDistance=0.0;
        double inVehicleTime=0.0;

        for (PlanElement element :elements){
            if(element instanceof Leg){
                element=(Leg)element;
                // Need To Change to "BikeShare"
                if(((Leg) element).getMode().equals("Shared-Bike")){
                    inVehicleTime=+((Leg)element).getRoute().getTravelTime().seconds()/60;

                }
            }

        }

        return(inVehicleTime);
    }
    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        double tripTime_min=getInVehicleTime_min(elements);
        double cost=0;

        Double sharingBookingCost=parameters.sharingBookingCosts.get(super.getMode());
        Double sharingMinCost=parameters.sharingMinCosts.get(super.getMode());
        cost= sharingBookingCost+(sharingMinCost*tripTime_min);

        return (cost);
    }
}
