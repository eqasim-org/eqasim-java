package org.eqasim.switzerland.ch_cmdp.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class SwissModeDetailedParameters extends ModeParameters {

    public static class SwissBikeParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaAge = 0.0;
        public double betaSex = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome = 0.0;
        public double betaShortDistance = 0.0;
        public double betaMediumDistance = 0.0;
    }

    public static class SwissCarParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaAge = 0.0;
        public double betaSex = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome = 0.0;
        public double betaDestinationWork = 0.0;
        public double betaUrbanDestination = 0.0;
        public double betaShortDistance = 0.0;
        public double betaMediumDistance = 0.0;

        public double additionalAccessEgressWalkTime_min = 0.0;
        public double constantParkingSearchPenalty_min = 0.0;
    }

    public static class SwissParking {
        public double urbanParkingCostPerHour = 0.0;
        public double urbanParkingSearchDuration_min = 0.0;

        public double suburbanParkingCostPerHour = 0.0;
        public double suburbanParkingSearchDuration_min = 0.0;
    }

    public static class SwissPTParameters {
        public double alpha_u = 0.0;

        public double betaLineSwitch_u = 0.0;
        public double betaInVehicleTime_u_min = 0.0;
        public double betaWaitingTime_u_min = 0.0;
        public double betaAccessEgressTime_u_min = 0.0;

        public double inVehicleTimeExponent = 1.0;
        public double waitingTimeExponent = 1.0;
        public double accessEgressTimeExponent = 1.0;
        public double lineSwitchExponent = 1.0;
        public double distanceExponent = 1.0;

        public double betaAge = 0.0;
        public double betaSex = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome = 0.0;
        public double betaDestinationWork = 0.0;
        public double betaUrbanDestination = 0.0;

        public double betaShortDistance = 0.0;
        public double betaMediumDistance = 0.0;
    }

    public static class SwissWalkParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaAge = 0.0;
        public double betaSex = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome = 0.0;
        public double betaShortDistance = 0.0;
        public double betaMediumDistance = 0.0;
    }

    public static class SwissCarPassengerParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaDistance_km = 0.0;
        public double distanceExponent = 1.0;

        public double betaAge = 0.0;
        public double betaSex = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome = 0.0;
        public double betaDestinationWork = 0.0;
        public double betaUrbanDestination = 0.0;
        public double betaDrivingLicense = 0.0;
        public double betaShortDistance = 0.0;
        public double betaMediumDistance = 0.0;
    }

    public final SwissBikeParameters bike = new SwissBikeParameters();
    public final SwissCarParameters car = new SwissCarParameters();
    public final SwissParking parking = new SwissParking();
    public final SwissPTParameters pt = new SwissPTParameters();
    public final SwissWalkParameters walk = new SwissWalkParameters();
    public final SwissCarPassengerParameters cp = new SwissCarPassengerParameters();

    public double lambdaCostIncome = 0.0;
    public double referenceIncome = 0.0;

    public static SwissModeDetailedParameters buildDefault() {
        SwissModeDetailedParameters parameters = new SwissModeDetailedParameters();
        // bike parameters
        parameters.bike.alpha_u = 4.246629786956503;
        parameters.bike.betaTravelTime_u_min = -0.8147649723046508;
        parameters.bike.travelTimeExponent = 0.5827142290071728;
        parameters.bike.betaAge = 0.0027926747913770617;
        parameters.bike.betaSex = -0.2872760033686839;
        parameters.bike.betaRegion1_u = -0.9127275599587426;
        parameters.bike.betaRegion2_u = -0.14244614114798254;
        parameters.bike.betaOriginHome = 0.1447788172488431;
        parameters.bike.betaShortDistance = 0.33360364911631607;
        parameters.bike.betaMediumDistance = 0.0;
        // car parameters
        parameters.car.alpha_u = 4.562389558207741;
        parameters.car.betaTravelTime_u_min = -0.5099008775071441;
        parameters.car.travelTimeExponent = 0.6260707505751207;
        parameters.car.betaAge = 0.005421209026286799;
        parameters.car.betaSex = -0.22016349540370544;
        parameters.car.betaRegion1_u = 0.33380109553550774;
        parameters.car.betaRegion2_u = -0.23207743104071657;
        parameters.car.betaOriginHome = 0.041627468198286985;
        parameters.car.betaDestinationWork = 0.6336766600163395;
        parameters.car.betaUrbanDestination = 0.12166994895495664;
        parameters.car.betaShortDistance = 0.16082276237059584;
        parameters.car.betaMediumDistance = 0.0;
        // parking parameters
        parameters.parking.urbanParkingCostPerHour = 1.0;
        parameters.parking.urbanParkingSearchDuration_min = 2.0;
        parameters.parking.suburbanParkingCostPerHour = 0.5;
        parameters.parking.suburbanParkingSearchDuration_min = 1.0;
        // pt parameters
        parameters.pt.alpha_u = 0.0;
        parameters.pt.betaLineSwitch_u = -0.5354462787763474;
        parameters.pt.betaInVehicleTime_u_min = -0.00025896058106833367;
        parameters.pt.betaWaitingTime_u_min = -0.004330518966022614;
        parameters.pt.betaAccessEgressTime_u_min = -0.0754569202344438;
        parameters.pt.inVehicleTimeExponent = 2.044183466473679;
        parameters.pt.waitingTimeExponent = 1.0;
        parameters.pt.accessEgressTimeExponent = 0.8953328836658916;
        parameters.pt.lineSwitchExponent = 1.2220187110863545;
        parameters.pt.betaAge = -0.009023071952701411;
        parameters.pt.betaSex = 0.1797530699381978;
        parameters.pt.betaRegion1_u = 0.04861026118010742;
        parameters.pt.betaRegion2_u = 0.4708356583055888;
        parameters.pt.betaOriginHome = 0.03235778087242491;
        parameters.pt.betaDestinationWork = 0.0;
        parameters.pt.betaUrbanDestination = 0.6488727071892483;
        parameters.pt.distanceExponent = 0.9573301347309126;
        parameters.pt.betaShortDistance = -0.27534520079668295;
        parameters.pt.betaMediumDistance = 0.0;
        // walk parameters
        parameters.walk.alpha_u = 10.619723062177538;
        parameters.walk.betaTravelTime_u_min = -3.4578500083931205;
        parameters.walk.travelTimeExponent = 0.3267760931924328;
        parameters.walk.betaAge = -0.003441247004592393;
        parameters.walk.betaSex = 0.0;
        parameters.walk.betaRegion1_u = 0.26104630030038883;
        parameters.walk.betaRegion2_u = 0.16038241705689887;
        parameters.walk.betaOriginHome = 0.0;
        parameters.walk.betaShortDistance = 0.4567832887326879;
        parameters.walk.betaMediumDistance = 0.0;
        // car passenger parameters
        parameters.cp.alpha_u = 0.7721638907379854;
        parameters.cp.betaTravelTime_u_min = -0.43998973060765384;
        parameters.cp.travelTimeExponent = 0.6908252223965073;
        parameters.cp.betaDistance_km = 0.0;
        parameters.cp.distanceExponent = 1.0;
        parameters.cp.betaAge = 0.004250435133252666;
        parameters.cp.betaSex = 0.42193608162286583;
        parameters.cp.betaRegion1_u = 0.26926990294329933;
        parameters.cp.betaRegion2_u = -0.2566945031742727;
        parameters.cp.betaOriginHome = 0.0;
        parameters.cp.betaDestinationWork = -0.5768940645800898;
        parameters.cp.betaUrbanDestination = -0.15424833585060732;
        parameters.cp.betaDrivingLicense = 0.7721638907379854;
        parameters.cp.betaShortDistance = 4.837096752092824e-05;
        parameters.cp.betaMediumDistance = 0.0;
        // cost parameters
        parameters.lambdaCostIncome = -0.050091259973945076;
        parameters.referenceIncome = 3700;

        parameters.lambdaCostEuclideanDistance = -0.2398221108268413;
        parameters.referenceEuclideanDistance_km = 8.0;

        parameters.betaCost_u_MU = -0.18752030808992587;

        return parameters;
    }
}
