package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters.SMMCostParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class KraussCarCostModel extends AbstractCostModel {
//    private final KraussCostParameters parameters;
    private final SMMCostParameters parameters;

//    @Inject
//    public KraussCarCostModel(KraussCostParameters parameters) {
//        super("car");
//        this.parameters = parameters;
//
//    }
    @Inject
    public KraussCarCostModel(SMMCostParameters parameters) {
        super("car");
        this.parameters = parameters;

    }
//    public KraussCarCostModel(KraussCostParameters parameters,String mode) {
//        super(mode);
//        this.parameters = parameters;
//
//    }



    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
       return (getInVehicleDistance_km(elements)*parameters.carCost_Km);
    }
}
