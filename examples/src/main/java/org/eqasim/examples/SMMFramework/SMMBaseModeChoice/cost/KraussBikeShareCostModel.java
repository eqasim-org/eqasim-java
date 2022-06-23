package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussBikeShareCostModel extends AbstractCostModel {

    private final SMMCostParameters parameters;

        public KraussBikeShareCostModel(SMMCostParameters parameters) {
            super("sharing:bikeShare");
            this.parameters = parameters;

        }

    public KraussBikeShareCostModel(SMMCostParameters parameters , String name) {
        super(name);
        this.parameters = parameters;

    }

       @Override
        protected double getInVehicleDistance_km(List<? extends PlanElement> elements){
           double inVehicleDistance=0.0;


                for (PlanElement element :elements){
                    if(element instanceof Leg){
                        element=(Leg)element;
                        if(((Leg) element).getMode()== "bike"){
                            inVehicleDistance=+((Leg) element).getRoute().getDistance();
                        }
                    }

                }

            return (inVehicleDistance);
        }

        @Override
        public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
            double tripDistance_km = getInVehicleDistance_km(elements);
            double cost=0;

                cost= parameters.bikeShareCost_Km* tripDistance_km+parameters.bookingCostBikeShare;

            return (cost);
        }
    }


