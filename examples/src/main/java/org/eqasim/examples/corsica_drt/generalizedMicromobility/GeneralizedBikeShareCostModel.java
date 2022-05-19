package org.eqasim.examples.corsica_drt.generalizedMicromobility;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class GeneralizedBikeShareCostModel extends AbstractCostModel {
    private final GeneralizedCostParameters parameters;

    public GeneralizedBikeShareCostModel(String mode ,GeneralizedCostParameters parameters) {
        super("sharing:"+mode);
        this.parameters=parameters;
    }
    @Override
    protected double getInVehicleDistance_km(List<? extends PlanElement> elements){
        double inVehicleDistance=0.0;


        for (PlanElement element :elements){
            if(element instanceof Leg){
                element=(Leg)element;
                // Need To Change to "BikeShare"
                if(((Leg) element).getMode().equals("Bike-Share")){
                    inVehicleDistance=+((Leg) element).getRoute().getDistance();

                }
            }

        }

        return (inVehicleDistance/1000);
    }
    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double tripDistance_km = getInVehicleDistance_km(elements);
        double cost=0;

        Double sharingBookingCost=parameters.sharingBookingCosts.get(super.getMode());
        Double sharingKmCost=parameters.sharingKMCosts.get(super.getMode());
        cost= parameters.sharingBookingCosts.get(this.getMode())+ parameters.sharingKMCosts.get(this.getMode())*tripDistance_km;

        return (cost);
    }
}
