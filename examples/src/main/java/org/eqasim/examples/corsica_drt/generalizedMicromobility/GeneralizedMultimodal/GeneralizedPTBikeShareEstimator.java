package org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedMultimodal;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.examples.corsica_drt.mode_choice.predictors.KraussPersonPredictor;
import org.eqasim.examples.corsica_drt.mode_choice.variables.KraussEqasimPersonVariables;
import org.eqasim.examples.corsica_drt.sharingPt.SharingPTParameters;
import org.eqasim.examples.corsica_drt.sharingPt.SharingPTVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class GeneralizedPTBikeShareEstimator implements UtilityEstimator {

    private final SharingPTParameters parameters;
    private final GeneralizedPTBikeSharePredictor sharingPTPredictor;
    private final String name;



    public GeneralizedPTBikeShareEstimator(SharingPTParameters parameters, GeneralizedPTBikeSharePredictor sharingPTPredictor, String mode) {
        this.parameters = parameters;
        this.sharingPTPredictor = sharingPTPredictor;
        this.name = mode;

    }



    protected double estimateConstantUtility() {
        return (parameters.bikeShare.alpha_u + parameters.pt.alpha_u);

    }
    protected double estimatePersonalUtilityPT(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements){
        KraussPersonPredictor personPredictor=new KraussPersonPredictor();
        KraussEqasimPersonVariables personVariables=personPredictor.predictVariables(person,trip,elements);
        double ageU=personVariables.age_a*parameters.pt.betaAge;
        double bikeAcc=personVariables.getBikeAcc()*parameters.pt.betaBikeAcc;
        double pTAcc=personVariables.getPtPass()*parameters.pt.betaPTPass;
        double carAcc=personVariables.getCarAccessibility()+parameters.pt.betaCarAcc;
        return(ageU+bikeAcc+pTAcc+carAcc);
    }
    protected double estimatePersonalUtilitySharing(Person person, DiscreteModeChoiceTrip trip,List<? extends PlanElement> elements){
        KraussPersonPredictor personPredictor=new KraussPersonPredictor();
        KraussEqasimPersonVariables personVariables=personPredictor.predictVariables(person,trip,elements);
        double ageU=personVariables.age_a*parameters.bikeShare.betaAge;
        double bikeAcc=personVariables.getBikeAcc()*parameters.bikeShare.betaBikeAcc;
        double carAcc=personVariables.getCarAccessibility()*parameters.bikeShare.betaCarAcc;
        double pTAcc=personVariables.getPtPass()*parameters.bikeShare.betaPTPass;
        return(ageU+bikeAcc+pTAcc+carAcc);
    }

    protected double estimateTravelTimeUtilitySharing(SharingPTVariables variables) {
        return parameters.bikeShare.betaTravelTime_u_min * variables.travelTime_u_min_Sharing;
    }

    protected double estimateAccessTimeUtilitySharing(SharingPTVariables variables) {
        return parameters.bikeShare.betaAccess_Time * variables.access_Time_Sharing;
    }

    protected double estimateMonetaryCostUtilitySharing(SharingPTVariables variables) {
        double utility=-Math.exp( parameters.betaCost_u_MU) * variables.cost;

        return utility;

    }

    protected double estimateEgressTimeUtilitySharing(SharingPTVariables variables) {
        return parameters.bikeShare.betaEgress_Time* variables.egress_Time_Sharing;
    }

    protected double estimateParkingTimeUtilitySharing(SharingPTVariables variables){
        return parameters.bikeShare.betaParkingTime_u_min*variables.parkingTime_u_min_Sharing;
    }

    protected double estimateTravelTimeUtilityPT(SharingPTVariables variables) {
        return parameters.pt.betaTravelTime_u_min * variables.travelTime_u_min;
    }

    protected double estimateAccessTimeUtilityPT(SharingPTVariables variables) {
        return parameters.pt.betaAccess_Time * variables.access_Time;
    }

    protected double estimateMonetaryCostUtilityPT(SharingPTVariables variables) {
        return -Math.exp(parameters.betaCost_u_MU) * variables.cost;
    }

    protected double estimateEgressTimeUtilityPT(SharingPTVariables variables) {
        return parameters.pt.betaEgress_Time* variables.egress_Time;
    }
    protected double estimateChangeLineUtilityPT(SharingPTVariables variables){
        return parameters.pt.betaTransfers*variables.transfers;
    }



    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        SharingPTVariables variables =sharingPTPredictor.predict(person, trip, elements);

        double utility = 0.0;

        utility += estimateConstantUtility();
        utility += estimateTravelTimeUtilitySharing(variables);
        utility += estimateAccessTimeUtilitySharing(variables);
        utility += estimateMonetaryCostUtilitySharing(variables);
        utility += estimateEgressTimeUtilitySharing(variables);
        utility+= estimateParkingTimeUtilitySharing(variables);

        utility+=estimatePersonalUtilitySharing(person,trip,elements);
        utility += estimateTravelTimeUtilityPT(variables);
        utility += estimateAccessTimeUtilityPT(variables);
        utility += estimateMonetaryCostUtilityPT(variables);
        utility += estimateEgressTimeUtilityPT(variables);
        utility +=estimateChangeLineUtilityPT(variables);
        utility+=estimatePersonalUtilityPT(person,trip,elements);
        return 100.0;
    }

}
