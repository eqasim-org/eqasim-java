package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalModeChoice.costModels;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

/**
 * Class combines teh cost models of a SMM mode and PT
 */

public class SMMMultimodalCostModel extends AbstractCostModel {
    private SMMCostParameters parameters;
    private String mode;

    public SMMMultimodalCostModel(String sharingMode, SMMCostParameters parameters){
        super("Multimodal_PT" + sharingMode);
        this.parameters = parameters;
        this.mode=sharingMode;

    }

    /**
     * Calculates the cost of the PT trip
     * @param person
     * @param trip
     * @param elements
     * @return
     */
    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        return (parameters.pTTicketCost);
    }

    @Override
    protected double getInVehicleDistance_km(List<? extends PlanElement> elements){
        double inVehicleDistance=0.0;


        for (PlanElement element :elements){
            if(element instanceof Leg){
                element=(Leg)element;
                if(((Leg) element).getMode()== mode){
                    inVehicleDistance=+((Leg) element).getRoute().getDistance();
                }
            }

        }

        return (inVehicleDistance);
    }

    /**
     * Calculates the in vehicle time of a trip
     * @param elements
     * @return
     */
    protected double getInVehicleTime_min(List<? extends PlanElement> elements){
        double inVehicleTime=0.0;


        for (PlanElement element :elements){
            if(element instanceof Leg){
                element=(Leg)element;
                if(((Leg) element).getMode()== mode){
                    inVehicleTime=+((Leg) element).getRoute().getTravelTime().seconds()/60;
                }
            }

        }

        return (inVehicleTime);
    }

    /**
     * Calculates the cost of the sharing segment trip
     * @param person
     * @param trip
     * @param elements
     * @return
     */
    public double calculateCost_MU_Sharing(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double inVehicleTime = getInVehicleTime_min(elements);
        double cost=0;

        cost= parameters.sharingBookingCosts.get("sharing:"+mode)+ parameters.sharingMinCosts.get("sharing:"+mode)*inVehicleTime;

        return (cost);
    }

}
