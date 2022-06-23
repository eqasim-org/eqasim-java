package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.costModels;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class SMMEScooterCostModel extends AbstractCostModel {

    private final SMMCostParameters parameters;


        public SMMEScooterCostModel(String mode, SMMCostParameters parameters) {
            super("sharing:"+mode);
            this.parameters = parameters;

        }

       @Override
        protected double getInVehicleDistance_km(List<? extends PlanElement> elements){
           double inVehicleDistance=0.0;
           double inVehicleTime=0.0;


                for (PlanElement element :elements){
                    if(element instanceof Leg){
                        element=(Leg)element;
                        if(((Leg) element).getMode()== "eScooter"){
                            inVehicleTime=+((Leg)element).getRoute().getTravelTime().seconds()/60;
                            inVehicleDistance=+((Leg) element).getRoute().getDistance();
                        }
                    }

                }

//            return (inVehicleDistance/1000);
           return(inVehicleTime);
        }

        @Override
        public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
            double tripDistance_km = getInVehicleDistance_km(elements);
            double tripTime_min=getInVehicleDistance_km(elements);
            double cost=0;

            Double sharingBookingCost=parameters.sharingBookingCosts.get(super.getMode());
            //Double sharingKmCost=parameters.sharingKMCosts.get(super.getMode());
            Double sharingMinCost=parameters.sharingMinCosts.get(super.getMode());
            cost= sharingBookingCost+(sharingMinCost*tripTime_min);

            return (cost);
        }
    }


