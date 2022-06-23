package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.parameters.KraussCostParameters;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussMicroMobilityCostModel extends AbstractCostModel {

    private final KraussCostParameters parameters;
        public final String proxyMode;
        @Inject
        public KraussMicroMobilityCostModel(KraussCostParameters parameters, String mode) {
            super(mode);
            this.parameters = parameters;
            this.proxyMode = mode;
        }

       @Override
        protected double getInVehicleDistance_km(List<? extends PlanElement> elements){
           double inVehicleDistance=0.0;
            if (this.proxyMode== "bike"){

                for (PlanElement element :elements){
                    if(element instanceof Leg){
                        element=(Leg)element;
                        if(((Leg) element).getMode()== TransportMode.bike){
                            inVehicleDistance=+((Leg) element).getRoute().getDistance();
                        }
                    }

                }
            }
            if(this.proxyMode=="eScooter") {

                for (PlanElement element :elements){
                    if(element instanceof Leg){
                        element=(Leg)element;
                        if(((Leg) element).getMode()== "eScooter"){
                            inVehicleDistance=+((Leg) element).getRoute().getDistance();
                        }
                    }

                }
            }
            return (inVehicleDistance);
        }

        @Override
        public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
            double tripDistance_km = getInVehicleDistance_km(elements);
            double cost=0;
            if(proxyMode==TransportMode.bike) {
                cost= parameters.bikeShareCost_Km* tripDistance_km+parameters.bookingCostBikeShare;
            }
            if(proxyMode=="eScooter") {
                cost=parameters.eScooterCost_km* tripDistance_km+parameters.bookingCostEScooter;
            }
            return (cost);
        }
    }


