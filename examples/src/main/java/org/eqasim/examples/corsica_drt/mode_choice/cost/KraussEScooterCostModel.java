package org.eqasim.examples.corsica_drt.mode_choice.cost;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.corsica_drt.mode_choice.parameters.KraussCostParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussEScooterCostModel extends AbstractCostModel {

    private final KraussCostParameters parameters;
        @Inject
        public KraussEScooterCostModel(KraussCostParameters parameters) {
            super("sharing:eScooter");
            this.parameters = parameters;

        }

       @Override
        protected double getInVehicleDistance_km(List<? extends PlanElement> elements){
           double inVehicleDistance=0.0;


                for (PlanElement element :elements){
                    if(element instanceof Leg){
                        element=(Leg)element;
                        if(((Leg) element).getMode()== "eScooter"){
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

                cost=parameters.eScooterCost_km* tripDistance_km+parameters.bookingCostEScooter;

            return (cost);
        }
    }


