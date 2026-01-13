package org.eqasim.switzerland.ch_cmdp.mode_choice.parameters;

import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;

import java.util.HashMap;
import java.util.Map;

public class SwissCmdpModeParameters extends SwissModeParameters {

    public final SwissBikeParameters bike = new SwissBikeParameters();
    public final SwissCarParameters car = new SwissCarParameters();
    public final SwissPTParameters pt = new SwissPTParameters();
    public final SwissWalkParameters walk = new SwissWalkParameters();
    public final SwissCarPassengerParameters cp = new SwissCarPassengerParameters();

    public final SwissParking parking = new SwissParking();

    public double lambdaCostIncome = 0.0;
    public double referenceIncome = 0.0;

    public double timeScale_min = 1.0;
    public double distanceScale_km = 1.0;

    public static class SwissBikeParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaAge_u = 0.0;
        public double betaSex_u = 0.0;
        public double betaLowIncome_u = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome_u = 0.0;
        public double betaShortDistance_u = 0.0;
        public double betaDestinationWork_u = 0.0;
        public double betaUrbanDestination_u = 0.0;
        public double betaUrbancoreDestination_u = 0.0;
        public double betaDestinationHome_u = 0.0;
        public double betaDestinationEducation_u = 0.0;
        public double betaDestinationShopping_u = 0.0;
        public double betaDestinationLeisure_u = 0.0;
        public double betaDestinationOther_u = 0.0;
        public double betaRetired_u = 0.0;
        public double betaLongDistance_u = 0.0;
    }

    public static class SwissCarParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaAge_u = 0.0;
        public double betaSex_u = 0.0;
        public double betaLowIncome_u = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome_u = 0.0;
        public double betaDestinationWork_u = 0.0;
        public double betaUrbanDestination_u = 0.0;
        public double betaUrbancoreDestination_u = 0.0;
        public double betaDestinationHome_u = 0.0;
        public double betaDestinationEducation_u = 0.0;
        public double betaDestinationShopping_u = 0.0;
        public double betaDestinationLeisure_u = 0.0;
        public double betaDestinationOther_u = 0.0;
        public double betaRetired_u = 0.0;
        public double betaCarOwnershipRatio_u = 0.0;
        public double betaShortDistance_u = 0.0;
        public double betaLongDistance_u = 0.0;
        // just because are used for calculating travel times in car predictor
        public double additionalAccessEgressWalkTime_min = 0.0;
        public double constantParkingSearchPenalty_min = 0.0;
    }

    public static class SwissParking {
        public double urbancoreParkingSearchDuration_min = 0.0;
        public double urbanParkingSearchDuration_min = 0.0;
        public double suburbanParkingSearchDuration_min = 0.0;
    }

    public static class SwissPTParameters {
        public double alpha_u = 0.0;

        public double betaLineSwitch_u = 0.0;
        public double betaInVehicleTime_u_min = 0.0;
        public double betaWaitingTime_u_min = 0.0;
        public double betaAccessEgressTime_u_min = 0.0;

        public double betaDistance_u_km = 0.0;

        public double inVehicleTimeExponent = 1.0;
        public double waitingTimeExponent = 1.0;
        public double accessEgressTimeExponent = 1.0;
        public double lineSwitchExponent = 1.0;
        public double distanceExponent = 1.0;

        public double betaAge_u = 0.0;
        public double betaSex_u = 0.0;
        public double betaLowIncome_u = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome_u = 0.0;
        public double betaDestinationWork_u = 0.0;
        public double betaUrbanDestination_u = 0.0;
        public double betaUrbancoreDestination_u = 0.0;
        public double betaDestinationHome_u = 0.0;
        public double betaDestinationEducation_u = 0.0;
        public double betaDestinationShopping_u = 0.0;
        public double betaDestinationLeisure_u = 0.0;
        public double betaDestinationOther_u = 0.0;
        public double betaShortDistance_u = 0.0;
        public double betaRetired_u = 0.0;
        public double betaLongDistance_u = 0.0;
        public double betaGoodService_u = 0.0;
        public double betaMediumService_u = 0.0;
    }

    public static class SwissWalkParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaAge_u = 0.0;
        public double betaSex_u = 0.0;
        public double betaLowIncome_u = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome_u = 0.0;
        public double betaShortDistance_u = 0.0;
        public double betaDestinationWork_u = 0.0;
        public double betaUrbanDestination_u = 0.0;
        public double betaUrbancoreDestination_u = 0.0;
        public double betaDestinationHome_u = 0.0;
        public double betaDestinationEducation_u = 0.0;
        public double betaDestinationShopping_u = 0.0;
        public double betaDestinationLeisure_u = 0.0;
        public double betaDestinationOther_u = 0.0;
        public double betaRetired_u = 0.0;
        public double betaLongDistance_u = 0.0;
    }

    public static class SwissCarPassengerParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaAge_u = 0.0;
        public double betaSex_u = 0.0;
        public double betaLowIncome_u = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome_u = 0.0;
        public double betaDestinationWork_u = 0.0;
        public double betaUrbanDestination_u = 0.0;
        public double betaUrbancoreDestination_u = 0.0;
        public double betaDestinationHome_u = 0.0;
        public double betaDestinationEducation_u = 0.0;
        public double betaDestinationShopping_u = 0.0;
        public double betaDestinationLeisure_u = 0.0;
        public double betaDestinationOther_u = 0.0;
        public double betaDrivingLicense_u = 0.0;
        public double betaShortDistance_u = 0.0;
        public double betaRetired_u = 0.0;
        public double betaLongDistance_u = 0.0;
    }

    public static SwissCmdpModeParameters buildDefault() {
        SwissCmdpModeParameters parameters = new SwissCmdpModeParameters();
        // bike
        parameters.bike.alpha_u= 4.49243634582002;
        parameters.bike.betaAge_u= 0.002887604600585483;
        parameters.bike.betaDestinationWork_u= 0.3089944496762585;
        parameters.bike.betaOriginHome_u= 0.039074702359104145;
        parameters.bike.betaRegion1_u= -1.0152934666383386;
        parameters.bike.betaRegion2_u= -0.5132123209894008;
        parameters.bike.betaSex_u= -0.21656357067399193;
        parameters.bike.betaShortDistance_u= 0.15435727710538502;
        parameters.bike.betaTravelTime_u_min= -0.8566142999818513;
        parameters.bike.betaUrbanDestination_u= -0.2517193568167992;
        parameters.bike.travelTimeExponent= 0.5618774653314881;
        // car
        parameters.car.additionalAccessEgressWalkTime_min= 0.0;
        parameters.car.alpha_u= 4.068909453602562;
        parameters.car.betaAge_u= 0.0035310067078649083;
        parameters.car.betaDestinationWork_u= 0.43520076806099467;
        parameters.car.betaOriginHome_u= 0.06229229422678974;
        parameters.car.betaRegion1_u= 0.16790919974349566;
        parameters.car.betaRegion2_u= -0.5042635872026866;
        parameters.car.betaSex_u= -0.3141860696195194;
        parameters.car.betaShortDistance_u= 0.0014894064151999055;
        parameters.car.betaTravelTime_u_min= -0.536791502986464;
        parameters.car.betaUrbanDestination_u= -0.11305683578193775;
        parameters.car.constantParkingSearchPenalty_min= 0.0;
        parameters.car.travelTimeExponent= 0.6433510105960759;
        //cost
        parameters.betaCost_u_MU= -0.1664802185519009;
        parameters.lambdaCostEuclideanDistance= -0.2568457559803551;
        parameters.lambdaCostIncome= -0.10073050052533078;
        parameters.referenceEuclideanDistance_km= 8.0;
        parameters.referenceIncome= 3700.0;
        // cp
        parameters.cp.alpha_u= 2.8383476435353088;
        parameters.cp.betaAge_u= 0.006326590364129841;
        parameters.cp.betaDestinationWork_u= -0.8289226427922092;
        parameters.cp.betaDrivingLicense_u= -1.870841635161752;
        parameters.cp.betaOriginHome_u= -0.0789571715409773;
        parameters.cp.betaRegion1_u= 0.05637768146838739;
        parameters.cp.betaRegion2_u= -0.4832722814719359;
        parameters.cp.betaSex_u= 0.31462004999324333;
        parameters.cp.betaShortDistance_u= -0.07890064348228691;
        parameters.cp.betaTravelTime_u_min= -0.4274200311527101;
        parameters.cp.betaUrbanDestination_u= -0.07929398616315597;
        parameters.cp.travelTimeExponent= 0.7039824608056751;
        // parking
        parameters.parking.urbancoreParkingSearchDuration_min= 3.0;
        parameters.parking.suburbanParkingSearchDuration_min= 1.0;
        parameters.parking.urbanParkingSearchDuration_min= 2.0;
        // pt
        parameters.pt.accessEgressTimeExponent= 0.8635888700054031;
        parameters.pt.alpha_u= 0.0;
        parameters.pt.betaAccessEgressTime_u_min= -0.07734382604678935;
        parameters.pt.betaAge_u= -0.008563561688468354;
        parameters.pt.betaDestinationWork_u= 0.10047425351694984;
        parameters.pt.betaDistance_u_km= -0.44037913563170084;
        parameters.pt.betaInVehicleTime_u_min= -0.00041198749011308846;
        parameters.pt.betaLineSwitch_u= -0.5505256814270415;
        parameters.pt.betaOriginHome_u= -0.01106249552173931;
        parameters.pt.betaRegion1_u= 0.0;
        parameters.pt.betaRegion2_u= 0.0;
        parameters.pt.betaSex_u= 0.20602019088679152;
        parameters.pt.betaShortDistance_u= -0.36299525852010806;
        parameters.pt.betaUrbanDestination_u= 0.5208463122475635;
        parameters.pt.betaWaitingTime_u_min= 0.0001796219382910962;
        parameters.pt.distanceExponent= 0.9789542267209345;
        parameters.pt.inVehicleTimeExponent= 1.9250586758953079;
        parameters.pt.lineSwitchExponent= 1.1074047623637953;
        parameters.pt.waitingTimeExponent= 1.0;
        // walk
        parameters.walk.alpha_u= 9.910109669379558;
        parameters.walk.betaAge_u= -0.004181639977603846;
        parameters.walk.betaDestinationWork_u= -0.015746828461997594;
        parameters.walk.betaOriginHome_u= 0.11323725893038335;
        parameters.walk.betaRegion1_u= 0.1626899433239513;
        parameters.walk.betaRegion2_u= -0.24732026303968818;
        parameters.walk.betaSex_u= 0.010109399413600487;
        parameters.walk.betaShortDistance_u= 0.28604921848184117;
        parameters.walk.betaTravelTime_u_min= -4.051627407199102;
        parameters.walk.betaUrbanDestination_u= -0.07677613348575689;
        parameters.walk.travelTimeExponent= 0.275689348069039;
        return parameters;
    }

    @Override
    public Map<String, Double> getASCs() {
        Map<String, Double> alphas = new HashMap<>();
        alphas.put("car", this.car.alpha_u);
        alphas.put("pt", this.pt.alpha_u);
        alphas.put("walk", this.walk.alpha_u);
        alphas.put("bike", this.bike.alpha_u);
        alphas.put("car_passenger", this.cp.alpha_u);
        return alphas;
    }

    @Override
    public void setASCs(Map<String, Double> alphas) {
        this.car.alpha_u = alphas.getOrDefault("car", this.car.alpha_u);
        this.pt.alpha_u = alphas.getOrDefault("pt", this.pt.alpha_u);
        this.walk.alpha_u = alphas.getOrDefault("walk", this.walk.alpha_u);
        this.bike.alpha_u = alphas.getOrDefault("bike", this.bike.alpha_u);
        this.cp.alpha_u = alphas.getOrDefault("car_passenger", this.cp.alpha_u);
    }

    @Override
    protected Object[][] getParameterObjects() {
        return new Object[][] {
                {car, "car"},
                {pt, "pt"},
                {bike, "bike"},
                {walk, "walk"},
                {cp, "cp"},
                {parking, "parking"},
                {swissCanton, "swissCanton"}
        };
    }
}
