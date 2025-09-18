package org.eqasim.switzerland.ch_cmdp.mode_choice.parameters;

import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;

import java.util.HashMap;
import java.util.Map;

public class SwissCmdpModeParameters extends SwissModeParameters {

    public static class SwissBikeParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaAge_u = 0.0;
        public double betaSex_u = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome_u = 0.0;
        public double betaShortDistance_u = 0.0;
        public double betaDestinationWork_u = 0.0;
        public double betaUrbanDestination_u = 0.0;
    }

    public static class SwissCarParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaAge_u = 0.0;
        public double betaSex_u = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome_u = 0.0;
        public double betaDestinationWork_u = 0.0;
        public double betaUrbanDestination_u = 0.0;
        public double betaShortDistance_u = 0.0;
        // just because are used for calculating travel times
        public double additionalAccessEgressWalkTime_min = 0.0;
        public double constantParkingSearchPenalty_min = 0.0;
    }

    public static class SwissParking {
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
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome_u = 0.0;
        public double betaDestinationWork_u = 0.0;
        public double betaUrbanDestination_u = 0.0;
        public double betaShortDistance_u = 0.0;
    }

    public static class SwissWalkParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaAge_u = 0.0;
        public double betaSex_u = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome_u = 0.0;
        public double betaShortDistance_u = 0.0;
        public double betaDestinationWork_u = 0.0;
        public double betaUrbanDestination_u = 0.0;
    }

    public static class SwissCarPassengerParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double travelTimeExponent = 1.0;

        public double betaDistance_km = 0.0;
        public double distanceExponent = 1.0;

        public double betaAge_u = 0.0;
        public double betaSex_u = 0.0;
        public double betaRegion1_u = 0.0;
        public double betaRegion2_u = 0.0;
        public double betaOriginHome_u = 0.0;
        public double betaDestinationWork_u = 0.0;
        public double betaUrbanDestination_u = 0.0;
        public double betaDrivingLicense_u = 0.0;
        public double betaShortDistance_u = 0.0;
    }

    public final SwissBikeParameters bike = new SwissBikeParameters();
    public final SwissCarParameters car = new SwissCarParameters();
    public final SwissParking parking = new SwissParking();
    public final SwissPTParameters pt = new SwissPTParameters();
    public final SwissWalkParameters walk = new SwissWalkParameters();
    public final SwissCarPassengerParameters cp = new SwissCarPassengerParameters();


    public double lambdaCostIncome = 0.0;
    public double referenceIncome = 0.0;

    public static SwissCmdpModeParameters buildDefault() {
        SwissCmdpModeParameters parameters = new SwissCmdpModeParameters();
        // car
        parameters.car.betaAge_u= 0.005843132698379965;
        parameters.car.alpha_u= 4.388878461614387;
        parameters.car.betaUrbanDestination_u= 0.12257554593863972;
        parameters.car.betaOriginHome_u= 0.02667230603453566;
        parameters.car.betaRegion1_u= 0.3353933715913141;
        parameters.car.betaRegion2_u= -0.23052128373461575;
        parameters.car.betaSex_u= -0.21451229997242335;
        parameters.car.betaShortDistance_u=0.1640329905708794;
        parameters.car.betaTravelTime_u_min= -0.4820441650099471;
        parameters.car.betaDestinationWork_u= 0.6321633125509171;
        parameters.car.travelTimeExponent= 0.6352440313062614;
        // pt
        parameters.pt.betaAccessEgressTime_u_min= -0.07927832160307664;
        parameters.pt.betaAge_u= -0.009280814611073518;
        parameters.pt.betaUrbanDestination_u= 0.6442681340319618;
        parameters.pt.betaDistance_u_km= -0.4710239313912821;
        parameters.pt.betaInVehicleTime_u_min= -0.0002690868249466892;
        parameters.pt.betaOriginHome_u= 0.08737324796898062;
        parameters.pt.betaRegion1_u= 0.0535077906351463;
        parameters.pt.betaRegion2_u= 0.4715010320686716;
        parameters.pt.betaSex_u= 0.17436333545877167;
        parameters.pt.betaShortDistance_u= -0.26168319735630813;
        parameters.pt.betaWaitingTime_u_min= -0.00549623100258595;
        parameters.pt.betaLineSwitch_u= -0.5246334199105043;
        parameters.pt.betaDestinationWork_u= -0.04185347953499858;
        parameters.pt.accessEgressTimeExponent= 0.8823440501503473;
        parameters.pt.distanceExponent= 0.9717741738936286;
        parameters.pt.inVehicleTimeExponent= 2.036997651741124;
        parameters.pt.lineSwitchExponent= 1.2205090652997155;
        parameters.pt.alpha_u= 0.0;
        parameters.pt.waitingTimeExponent= 1.0;
        // bike
        parameters.bike.betaAge_u= 0.0021570481328154097;
        parameters.bike.alpha_u= 4.245501634155395;
        parameters.bike.betaUrbanDestination_u= -0.12782932799792296;
        parameters.bike.betaOriginHome_u= 0.17547690925600776;
        parameters.bike.betaRegion1_u= -0.9106865743470643;
        parameters.bike.betaRegion2_u= -0.12649313760038836;
        parameters.bike.betaSex_u= -0.2776051581043676;
        parameters.bike.betaShortDistance_u= 0.3340911163122343;
        parameters.bike.betaTravelTime_u_min= -0.8335205846829152;
        parameters.bike.betaDestinationWork_u= 0.1354165364474999;
        parameters.bike.travelTimeExponent= 0.5769124247052974;
        // walk
        parameters.walk.betaAge_u= -0.004076007748122504;
        parameters.walk.alpha_u= 10.743000550750368;
        parameters.walk.betaUrbanDestination_u= 0.11078919300628497;
        parameters.walk.betaOriginHome_u= 0.06260472607226988;
        parameters.walk.betaRegion1_u= 0.2604132158417492;
        parameters.walk.betaRegion2_u= 0.1461083030602035;
        parameters.walk.betaSex_u= -0.01858939346739711;
        parameters.walk.betaShortDistance_u= 0.4498401556947482;
        parameters.walk.betaTravelTime_u_min= -3.675671812776943;
        parameters.walk.betaDestinationWork_u= -0.18037591141011203;
        parameters.walk.travelTimeExponent= 0.31632568858133187;
        // cp
        parameters.cp.betaAge_u= 0.005356641525563564;
        parameters.cp.alpha_u= 0.6869643171335701;
        parameters.cp.betaUrbanDestination_u= -0.13350922489410588;
        parameters.cp.betaDrivingLicense_u= 0.6869643171335701;
        parameters.cp.betaOriginHome_u= -0.1333631229596035;
        parameters.cp.betaRegion1_u= 0.2613721962793551;
        parameters.cp.betaRegion2_u= -0.260594913794077;
        parameters.cp.betaSex_u= 0.43059316872010345;
        parameters.cp.betaShortDistance_u= -0.010368194830963478;
        parameters.cp.betaTravelTime_u_min= -0.4261028482324213;
        parameters.cp.betaDestinationWork_u= -0.48856786260645807;
        parameters.cp.travelTimeExponent= 0.6947587935230592;
        parameters.cp.betaDistance_km= 0.0;
        parameters.cp.distanceExponent= 1.0;
        // cost
        parameters.betaCost_u_MU= -0.1948729798610939;
        parameters.lambdaCostEuclideanDistance= -0.25041128831343196;
        parameters.lambdaCostIncome= -0.05798575236341841;
        parameters.referenceIncome= 3700;
        parameters.referenceEuclideanDistance_km= 8.0;
        // parking
        parameters.parking.urbanParkingSearchDuration_min= 2.0;
        parameters.parking.suburbanParkingSearchDuration_min= 1.0;
        return parameters;
    }

    @Override
    public Map<String, Double> getASCs() {
        Map<String, Double> alphas = new HashMap<>();
        alphas.put("car", car.alpha_u);
        alphas.put("pt", pt.alpha_u);
        alphas.put("walk", walk.alpha_u);
        alphas.put("bike", bike.alpha_u);
        alphas.put("car_passenger", cp.alpha_u);
        return alphas;
    }

    @Override
    public void setASCs(Map<String, Double> alphas) {
        car.alpha_u = alphas.getOrDefault("car", car.alpha_u);
        pt.alpha_u = alphas.getOrDefault("pt", pt.alpha_u);
        walk.alpha_u = alphas.getOrDefault("walk", walk.alpha_u);
        bike.alpha_u = alphas.getOrDefault("bike", bike.alpha_u);
        cp.alpha_u = alphas.getOrDefault("car_passenger", cp.alpha_u);
    }
}
