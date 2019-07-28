package org.eqasim.sao_paulo.trb2020;

import org.eqasim.sao_paulo.mode_choice.parameters.ModeChoiceParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class UtilityShareItem {
    final public Id<Person> personId;
    final public String selectedMode;
    public double crowflyDistance;
    final public ModeChoiceParameters parameters;
    final public PersonVariables personVariables;

    // Utilities
    public class CarUtilities {
        public double alpha = 0.0;
        public double travelTime = 0.0;
        public double accessEgressTime = 0.0;
        public double cost = 0.0;
    }

    public class PtUtilities {
        public double alpha = 0.0;
        public double numberOfLineSwitches = 0.0;
        public double waitingTime = 0.0;
        public double inVehicleTime = 0.0;
        public double accessEgressTime = 0.0;
        public double cost = 0.0;
    }

    public class BikeUtilities {
        public double alpha = 0.0;
        public double travelTime = 0.0;
        public double ageOver18 = 0.0;
    }

    public class WalkUtilities {
        public double alpha = 0.0;
        public double travelTime = 0.0;
    }

    public final CarUtilities car = new CarUtilities();
    public final PtUtilities pt = new PtUtilities();
    public final BikeUtilities bike = new BikeUtilities();
    public final WalkUtilities walk = new WalkUtilities();


    // Constructor
    public UtilityShareItem(Id<Person> personId, ModeChoiceParameters parameters, PersonVariables personVariables, String selectedMode) {
        this.personId = personId;
        this.parameters = parameters;
        this.personVariables = personVariables;
        this.selectedMode = selectedMode;
    }

    public void estimateCarUtilities(CarVariables variables) {
        crowflyDistance = variables.crowflyDistance_km;

        car.alpha = parameters.car.alpha;
        car.travelTime = parameters.car.betaTravelTime * variables.travelTime_min;
        car.accessEgressTime = parameters.walk.betaTravelTime * variables.accessEgressTime_min;
        car.cost = parameters.betaCost //
                * Math.pow(Math.max(variables.crowflyDistance_km, 0.001) / parameters.referenceCrowflyDistance_km,
                parameters.lambdaCostCrowflyDistance) //
                * variables.cost_BRL;
    }

    public void estimatePtUtilities(PtVariables variables) {
        pt.alpha += parameters.pt.alpha;

        pt.accessEgressTime += parameters.pt.betaAccessEgressTime * variables.accessEgressTime_min;
        pt.inVehicleTime += parameters.pt.betaInVehicleTime * variables.inVehicleTime_min;
        pt.waitingTime += parameters.pt.betaWaitingTime * variables.waitingTime_min;
        pt.numberOfLineSwitches += parameters.pt.betaLineSwitch * variables.numberOfLineSwitches;

        pt.cost += parameters.betaCost //
                * Math.pow(Math.max(variables.crowflyDistance_km, 0.001) / parameters.referenceCrowflyDistance_km,
                parameters.lambdaCostCrowflyDistance) //
                * variables.cost_BRL;
    }

    public void estimateBikeUtilities(BikeVariables variables) {
        bike.alpha += parameters.bike.alpha;
        bike.travelTime += parameters.bike.betaTravelTime * variables.travelTime_min;
        bike.ageOver18 += parameters.bike.betaAgeOver18 * Math.max(0.0, personVariables.age_a - 18);
    }

    public void estimateWalkUtilities(WalkVariables variables) {
        walk.alpha += parameters.walk.alpha;
        walk.travelTime += parameters.walk.betaTravelTime * variables.travelTime_min;
    }
}
