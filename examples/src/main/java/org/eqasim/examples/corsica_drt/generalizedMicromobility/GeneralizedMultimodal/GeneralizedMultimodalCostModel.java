package org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedMultimodal;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedCostParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class GeneralizedMultimodalCostModel extends AbstractCostModel {
    private GeneralizedCostParameters parameters;
    private String mode;

    public GeneralizedMultimodalCostModel(String sharingMode, GeneralizedCostParameters parameters){
        super("Multimodal_PT" + sharingMode);
        this.parameters = parameters;
        this.mode=sharingMode;

    }
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

    public double calculateCost_MU_Sharing(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double tripDistance_km = getInVehicleDistance_km(elements);
        double cost=0;

        cost= parameters.sharingBookingCosts.get("sharing:"+mode)+ parameters.sharingMinCosts.get("sharing:"+mode)*tripDistance_km;

        return (cost);
    }

}
