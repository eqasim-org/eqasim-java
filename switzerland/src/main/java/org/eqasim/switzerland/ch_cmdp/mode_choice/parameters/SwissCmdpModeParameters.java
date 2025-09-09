package org.eqasim.switzerland.ch_cmdp.mode_choice.parameters;

import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

    public class SwissCantonDummies {
        public Map<String, Double> car = getCantonDummies();
        public Map<String, Double> pt = getCantonDummies();
        public Map<String, Double> bike = getCantonDummies();
        public Map<String, Double> walk = getCantonDummies();
    }

    public static Map<String, Double> getCantonDummies() {
        // By default all dummies are zeros
        List<String> cantons = Arrays.asList(
                "Aargau", "Appenzell Ausserrhoden","Appenzell Innerrhoden","Basel-Landschaft",
                "Basel-Stadt","Bern","Fribourg","Genève","Glarus","Graubünden","Jura","Luzern",
                "Neuchâtel","Nidwalden","Obwalden","Schaffhausen","Schwyz","Solothurn","St. Gallen",
                "Thurgau","Ticino","Uri","Valais","Vaud","Zug","Zürich"
        );
        Map<String, Double> cantonDummies = new HashMap<>();
        for (String canton : cantons) {
            cantonDummies.put(canton, 0.0);
        }
        return cantonDummies;
    }

    public final SwissBikeParameters bike = new SwissBikeParameters();
    public final SwissCarParameters car = new SwissCarParameters();
    public final SwissParking parking = new SwissParking();
    public final SwissPTParameters pt = new SwissPTParameters();
    public final SwissWalkParameters walk = new SwissWalkParameters();
    public final SwissCarPassengerParameters cp = new SwissCarPassengerParameters();

    public final SwissCantonDummies swissCanton = new SwissCantonDummies();

    public double lambdaCostIncome = 0.0;
    public double referenceIncome = 0.0;

    public static SwissCmdpModeParameters buildDefault() {
        SwissCmdpModeParameters parameters = new SwissCmdpModeParameters();
        // car
        parameters.car.betaAge_u= 0.0054215004867201825;
        parameters.car.alpha_u= 4.500752236591399;
        parameters.car.betaUrbanDestination_u= 0.13005233050021825;
        parameters.car.betaOriginHome_u= 0.046896053773648104;
        parameters.car.betaRegion1_u= 0.3319119914462197;
        parameters.car.betaRegion2_u= -0.23392630546719964;
        parameters.car.betaSex_u= -0.22164457603391627;
        parameters.car.betaShortDistance_u= 0.16077895094394368;
        parameters.car.betaTravelTime_u_min= -0.48696884495358556;
        parameters.car.betaDestinationWork_u= 0.6260452824028707;
        parameters.car.travelTimeExponent= 0.6361151667296904;
        // pt
        parameters.pt.betaAccessEgressTime_u_min= -0.07554583326245376;
        parameters.pt.betaAge_u= -0.00896408773500069;
        parameters.pt.betaUrbanDestination_u= 0.6436952385896489;
        parameters.pt.betaDistance_u_km= -0.5430317338734312;
        parameters.pt.betaInVehicleTime_u_min= -0.00026132311997366104;
        parameters.pt.betaOriginHome_u= 0.050526371893232995;
        parameters.pt.betaRegion1_u= 0.05108189229132989;
        parameters.pt.betaRegion2_u= 0.4724768305292634;
        parameters.pt.betaSex_u= 0.17744678570510417;
        parameters.pt.betaShortDistance_u= -0.2735375479656389;
        parameters.pt.betaWaitingTime_u_min= -0.0045562570994452005;
        parameters.pt.betaLineSwitch_u= -0.5342617906465101;
        parameters.pt.betaDestinationWork_u= -0.01905183445413108;
        parameters.pt.accessEgressTimeExponent= 0.8956446260664328;
        parameters.pt.distanceExponent= 0.9522023923858368;
        parameters.pt.inVehicleTimeExponent= 2.045017490899073;
        parameters.pt.lineSwitchExponent= 1.2223446798096183;
        parameters.pt.alpha_u= 0.0;
        parameters.pt.waitingTimeExponent= 1.0;
        // bike
        parameters.bike.betaAge_u= 0.0024827298040012167;
        parameters.bike.alpha_u= 4.284390563718338;
        parameters.bike.betaUrbanDestination_u= -0.11961926967745469;
        parameters.bike.betaOriginHome_u= 0.14717039229495857;
        parameters.bike.betaRegion1_u= -0.9136805993994767;
        parameters.bike.betaRegion2_u= -0.12541366212160704;
        parameters.bike.betaSex_u= -0.2786301305645197;
        parameters.bike.betaShortDistance_u= 0.33406098860914707;
        parameters.bike.betaTravelTime_u_min= -0.8068346935507416;
        parameters.bike.betaDestinationWork_u= 0.15964180772062575;
        parameters.bike.travelTimeExponent= 0.5861675981847162;
        // walk
        parameters.walk.betaAge_u= -0.0034896542716708885;
        parameters.walk.alpha_u= 10.670208465521767;
        parameters.walk.betaUrbanDestination_u= 0.10020916130431032;
        parameters.walk.betaOriginHome_u= 0.05668720415889544;
        parameters.walk.betaRegion1_u= 0.26413964659784805;
        parameters.walk.betaRegion2_u= 0.1471764772159446;
        parameters.walk.betaSex_u= -0.009450069399480747;
        parameters.walk.betaShortDistance_u= 0.45803917665913396;
        parameters.walk.betaTravelTime_u_min= -3.5726223521479623;
        parameters.walk.betaDestinationWork_u= -0.15213357941990846;
        parameters.walk.travelTimeExponent= 0.3210572590426351;
        // cp
        parameters.cp.betaAge_u= 0.004549511716379108;
        parameters.cp.alpha_u= 0.7610196486456368;
        parameters.cp.betaUrbanDestination_u= -0.1380431406318976;
        parameters.cp.betaDrivingLicense_u= 0.7610196486456368;
        parameters.cp.betaOriginHome_u= -0.08251595574860865;
        parameters.cp.betaRegion1_u= 0.26654706906460224;
        parameters.cp.betaRegion2_u= -0.2603133401568398;
        parameters.cp.betaSex_u= 0.426527642927639;
        parameters.cp.betaShortDistance_u= -0.003428697856153015;
        parameters.cp.betaTravelTime_u_min= -0.42848476357716925;
        parameters.cp.betaDestinationWork_u= -0.5577190808026801;
        parameters.cp.travelTimeExponent= 0.6962170178554364;
        parameters.cp.betaDistance_km= 0.0;
        parameters.cp.distanceExponent= 1.0;
        // cost
        parameters.betaCost_u_MU= -0.18767356558283543;
        parameters.lambdaCostEuclideanDistance= -0.2430143714103748;
        parameters.lambdaCostIncome= -0.05040248536634442;
        parameters.referenceIncome= 3700;
        parameters.referenceEuclideanDistance_km= 8.0;
        
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
