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
        public double betaJunior_u = 0.0;
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
        public double betaJunior_u = 0.0;
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
        public double betaJunior_u = 0.0;
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
        public double betaJunior_u = 0.0;
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
        public double betaJunior_u = 0.0;
        public double betaLongDistance_u = 0.0;
        public double betaCarOwnershipRatio_u = 0.0;
        public double betaHasCar_u = 0.0;
        public double betaVeryLongDistance_u = 0.0;
        public double betaDistance_u_km = 0.0;
    }

    public static SwissCmdpModeParameters buildDefault() {
        SwissCmdpModeParameters parameters = new SwissCmdpModeParameters();
        // bike
        parameters.bike.alpha_u = 3.5927177629996963;
        parameters.bike.betaTravelTime_u_min = -3.381763673284838;
        parameters.bike.travelTimeExponent = 0.49643060880160594;
        parameters.bike.betaAge_u = 0.012469595848858433;
        parameters.bike.betaSex_u = -0.4315799671086237;
        parameters.bike.betaLowIncome_u = -0.19060846355206248;
        parameters.bike.betaRegion1_u = -1.0545718528375023;
        parameters.bike.betaRegion2_u = -0.43888452837940123;
        parameters.bike.betaOriginHome_u = 0.3777423561968097;
        parameters.bike.betaShortDistance_u = 0.1277838223450565;
        parameters.bike.betaDestinationWork_u = 0.0;
        parameters.bike.betaUrbanDestination_u = -0.4172492637463995;
        parameters.bike.betaUrbancoreDestination_u = -0.6543202737365967;
        parameters.bike.betaDestinationHome_u = 0.0;
        parameters.bike.betaDestinationEducation_u = -0.20973405680476825;
        parameters.bike.betaDestinationShopping_u = -0.2415414806417895;
        parameters.bike.betaDestinationLeisure_u = 0.25649030457789007;
        parameters.bike.betaDestinationOther_u = -0.3629626898614691;
        parameters.bike.betaRetired_u = -1.2144188553681627;
        parameters.bike.betaJunior_u = 0.0;
        parameters.bike.betaLongDistance_u = -0.743;
        // car
        parameters.car.alpha_u = 3.0672569334801194;
        parameters.car.betaTravelTime_u_min = -1.8576471340907157;
        parameters.car.travelTimeExponent = 0.6794651920480461;
        parameters.car.betaAge_u = 0.014388451605567003;
        parameters.car.betaSex_u = -0.5881326441376203;
        parameters.car.betaLowIncome_u = 0.1737115306545399;
        parameters.car.betaRegion1_u = 0.22426864341751757;
        parameters.car.betaRegion2_u = -0.4307350486435314;
        parameters.car.betaOriginHome_u = 0.0;
        parameters.car.betaDestinationWork_u = 1.0346532153520727;
        parameters.car.betaUrbanDestination_u = 0.03972924253714031;
        parameters.car.betaUrbancoreDestination_u = -0.7520313581859107;
        parameters.car.betaDestinationHome_u = 0.0;
        parameters.car.betaDestinationEducation_u = -0.10800133476839621;
        parameters.car.betaDestinationShopping_u = 0.760185997833131;
        parameters.car.betaDestinationLeisure_u = 0.5963611861439145;
        parameters.car.betaDestinationOther_u = 1.228910363566684;
        parameters.car.betaRetired_u = -0.6107293056688078;
        parameters.car.betaJunior_u = 0.0;
        parameters.car.betaCarOwnershipRatio_u = -2.203310895104914;
        parameters.car.betaShortDistance_u = -0.1772212965411382;
        parameters.car.betaLongDistance_u = -0.04205505781507571;
        parameters.car.additionalAccessEgressWalkTime_min = 0.0;
        parameters.car.constantParkingSearchPenalty_min = 0.0;
        // pt
        parameters.pt.alpha_u = 0.0;
        parameters.pt.betaLineSwitch_u = -0.5700042404869097;
        parameters.pt.betaInVehicleTime_u_min = -0.07826799515064778;
        parameters.pt.betaWaitingTime_u_min = -0.02;
        parameters.pt.betaAccessEgressTime_u_min = -0.7707734529912327;
        parameters.pt.betaDistance_u_km = -0.5;
        parameters.pt.inVehicleTimeExponent = 1.53182749180065;
        parameters.pt.waitingTimeExponent = 1.0;
        parameters.pt.accessEgressTimeExponent = 0.5927900666818301;
        parameters.pt.lineSwitchExponent = 0.9656414747841747;
        parameters.pt.distanceExponent = 1.2418217836284875;
        parameters.pt.betaAge_u = 0.0;
        parameters.pt.betaSex_u = 0.0;
        parameters.pt.betaLowIncome_u = 0.14322380968470677;
        parameters.pt.betaRegion1_u = 0.0;
        parameters.pt.betaRegion2_u = 0.0;
        parameters.pt.betaOriginHome_u = 0.0;
        parameters.pt.betaDestinationWork_u = 0.0;
        parameters.pt.betaUrbanDestination_u = 0.0;
        parameters.pt.betaUrbancoreDestination_u = 0.0;
        parameters.pt.betaDestinationHome_u = 0.0;
        parameters.pt.betaDestinationEducation_u = 0.0;
        parameters.pt.betaDestinationShopping_u = 0.0;
        parameters.pt.betaDestinationLeisure_u = 0.0;
        parameters.pt.betaDestinationOther_u = 0.0;
        parameters.pt.betaShortDistance_u = 0.0;
        parameters.pt.betaRetired_u = 0.0;
        parameters.pt.betaJunior_u = -0.29545862768485365;
        parameters.pt.betaLongDistance_u = 0.0;
        parameters.pt.betaGoodService_u = 0.5578317653721179;
        parameters.pt.betaMediumService_u = 0.14846956364547517;
        // walk
        parameters.walk.alpha_u = 10.661470169149224;
        parameters.walk.betaTravelTime_u_min = -9.566189215264982;
        parameters.walk.travelTimeExponent = 0.23506491541242683;
        parameters.walk.betaAge_u = 0.008808921690447119;
        parameters.walk.betaSex_u = -0.19667423257332575;
        parameters.walk.betaLowIncome_u = 0.0;
        parameters.walk.betaRegion1_u = 0.184189604471061;
        parameters.walk.betaRegion2_u = -0.23972946772878487;
        parameters.walk.betaOriginHome_u = 0.31428685184495697;
        parameters.walk.betaShortDistance_u = 0.36956944437856815;
        parameters.walk.betaDestinationWork_u = 0.0;
        parameters.walk.betaUrbanDestination_u = -0.20428472116556282;
        parameters.walk.betaUrbancoreDestination_u = -0.43955980687163954;
        parameters.walk.betaDestinationHome_u = 0.0;
        parameters.walk.betaDestinationEducation_u = 0.033642777279421755;
        parameters.walk.betaDestinationShopping_u = 0.0;
        parameters.walk.betaDestinationLeisure_u = 0.5703433911077829;
        parameters.walk.betaDestinationOther_u = 0.13289904481002526;
        parameters.walk.betaRetired_u = -0.34708363123990676;
        parameters.walk.betaJunior_u = -0.178097494756041;
        parameters.walk.betaLongDistance_u = 0.0;
        // cp
        parameters.cp.alpha_u = -0.23744025041491518;
        parameters.cp.betaTravelTime_u_min = -1.9297123597956438;
        parameters.cp.travelTimeExponent = 0.5748923473838079;
        parameters.cp.betaAge_u = 0.009784332322306907;
        parameters.cp.betaSex_u = 0.2905847791902105;
        parameters.cp.betaLowIncome_u = 0.0;
        parameters.cp.betaRegion1_u = 0.16338625263715648;
        parameters.cp.betaRegion2_u = -0.44359561907887796;
        parameters.cp.betaOriginHome_u = 0.0;
        parameters.cp.betaDestinationWork_u = -0.3059975523740487;
        parameters.cp.betaUrbanDestination_u = 0.02618044920526046;
        parameters.cp.betaUrbancoreDestination_u = -0.6878090666651031;
        parameters.cp.betaDestinationHome_u = 0.0;
        parameters.cp.betaDestinationEducation_u = -1.077905185382879;
        parameters.cp.betaDestinationShopping_u = 0.38953362177899853;
        parameters.cp.betaDestinationLeisure_u = 0.7465501603552759;
        parameters.cp.betaDestinationOther_u = 0.5344300317348695;
        parameters.cp.betaDrivingLicense_u = -0.4647771762370617;
        parameters.cp.betaShortDistance_u = -0.1958761328390998;
        parameters.cp.betaRetired_u = 0.05109530021118863;
        parameters.cp.betaJunior_u = 0.377244612930022;
        parameters.cp.betaLongDistance_u = -0.022981248064928746;
        parameters.cp.betaCarOwnershipRatio_u = -0.4394942652191965;
        parameters.cp.betaHasCar_u = 0.5539118732627121;
        parameters.cp.betaVeryLongDistance_u = -0.5197332032992993;
        parameters.cp.betaDistance_u_km = -0.12015592935907686;
        // parking
        parameters.parking.urbancoreParkingSearchDuration_min = 3.0;
        parameters.parking.urbanParkingSearchDuration_min = 2.5;
        parameters.parking.suburbanParkingSearchDuration_min = 1.5;
        // swissCanton
        parameters.swissCanton.car = new HashMap<String, Double>() {{
            put("solothurn", -0.2951613221601043);
            put("zurich", 0.13526431283199272);
            put("fribourg", -0.5879729521151588);
            put("luzern", -0.2957182622805824);
            put("geneva", 0.5102995624978554);
            put("appenzell_innerrhoden", 0.3994662051719874);
            put("jura", 0.7793873155580636);
            put("vaud", -0.19711769199309076);
            put("appenzell_ausserrhoden", -1.098873286467795);
            put("zug", -0.3259349985244642);
            put("bern", -0.350878004501517);
            put("basel_stadt", 0.005986013263120649);
            put("neuchatel", 0.40874648217301124);
            put("glarus", 0.1034287754447117);
            put("grisons", -0.31609983550076787);
            put("thurgau", 0.1644193486555161);
            put("aargau", -0.49411136483840956);
            put("basel_landschaft", -0.6256544366085317);
            put("schwyz", -0.6072917844452301);
            put("obwalden", -0.743516570055617);
            put("st_gallen", -0.19491925259277482);
            put("uri", -0.4401996594192067);
            put("valais", 0.38605241904288845);
            put("schaffhausen", -0.33355428770497714);
            put("ticino", 0.8339317192571863);
            put("nidwalden", -0.7178936393165481);
        }};

        parameters.swissCanton.pt = new HashMap<String, Double>() {{
            put("solothurn", 0.0);
            put("zurich", 0.0);
            put("fribourg", 0.0);
            put("luzern", 0.0);
            put("geneva", 0.0);
            put("appenzell_innerrhoden", 0.0);
            put("jura", 0.0);
            put("vaud", 0.0);
            put("appenzell_ausserrhoden", 0.0);
            put("zug", 0.0);
            put("bern", 0.0);
            put("basel_stadt", 0.0);
            put("neuchatel", 0.0);
            put("glarus", 0.0);
            put("grisons", 0.0);
            put("thurgau", 0.0);
            put("aargau", 0.0);
            put("basel_landschaft", 0.0);
            put("schwyz", 0.0);
            put("obwalden", 0.0);
            put("st_gallen", 0.0);
            put("uri", 0.0);
            put("valais", 0.0);
            put("schaffhausen", 0.0);
            put("ticino", 0.0);
            put("nidwalden", 0.0);
        }};
        parameters.swissCanton.bike = new HashMap<String, Double>() {{
            put("solothurn", -0.43117138456866394);
            put("zurich", 0.027964855825845562);
            put("fribourg", -0.3510797746378623);
            put("luzern", -0.28067313678988093);
            put("geneva", 0.17527403674276598);
            put("appenzell_innerrhoden", -0.07262747083582519);
            put("jura", -0.30560042823393024);
            put("vaud", 0.22893065832568965);
            put("appenzell_ausserrhoden", -0.9516065501584583);
            put("zug", 0.0018123423950064284);
            put("bern", -0.3123685996264425);
            put("basel_stadt", 0.19239397368673553);
            put("neuchatel", 0.34057767452107024);
            put("glarus", -0.18776141320570977);
            put("grisons", 0.824270439465248);
            put("thurgau", -0.10563648969693255);
            put("aargau", -0.3580533481615857);
            put("basel_landschaft", -0.6600494092956736);
            put("schwyz", 0.44964260857544763);
            put("obwalden", -0.5816336672072551);
            put("st_gallen", -0.20161745360323316);
            put("uri", -0.16390398806601086);
            put("valais", 1.022925871323701);
            put("schaffhausen", -0.542990676716562);
            put("ticino", 1.125156271859035);
            put("nidwalden", -0.222106241110165);
        }};
        parameters.swissCanton.walk = new HashMap<String, Double>() {{
            put("solothurn", -0.3572348316497482);
            put("zurich", -0.1505160676770903);
            put("fribourg", -0.5645044451550242);
            put("luzern", -0.2463752131560756);
            put("geneva", 0.6435928474985015);
            put("appenzell_innerrhoden", -0.45930130868101804);
            put("jura", 0.021263345619076994);
            put("vaud", -0.11063580703686476);
            put("appenzell_ausserrhoden", -1.3878209391319118);
            put("zug", 0.32795934597199833);
            put("bern", -0.368119582926722);
            put("basel_stadt", -0.2561079420013709);
            put("neuchatel", -0.0671431851035896);
            put("glarus", -0.2524493693172163);
            put("grisons", 0.2388942240885312);
            put("thurgau", 0.08948109485796532);
            put("aargau", -0.3429952895138346);
            put("basel_landschaft", -0.7239644065509325);
            put("schwyz", -0.3308285583883268);
            put("obwalden", -0.11196836372635413);
            put("st_gallen", -0.18058858654320345);
            put("uri", 0.038442325289403924);
            put("valais", 0.6889289277726824);
            put("schaffhausen", -0.5550598223292648);
            put("ticino", 0.6534054034558623);
            put("nidwalden", -0.577422380196774);
        }};
        parameters.swissCanton.cp = new HashMap<String, Double>() {{
            put("solothurn", -0.13281227308336324);
            put("zurich", 0.023309027630906767);
            put("fribourg", -0.1690937554005388);
            put("luzern", -0.35549123751823525);
            put("geneva", 0.20244330984171988);
            put("appenzell_innerrhoden", 0.308565523347489);
            put("jura", 0.3926875107598058);
            put("vaud", -0.22152650547883856);
            put("appenzell_ausserrhoden", -0.31022827026849137);
            put("zug", -0.5239947238496336);
            put("bern", -0.2583863046521364);
            put("basel_stadt", -0.6608263839226954);
            put("neuchatel", 0.2679205627293582);
            put("glarus", 0.01935972999058256);
            put("grisons", -0.649159392139144);
            put("thurgau", 0.28188489273309236);
            put("aargau", -0.3795487823586011);
            put("basel_landschaft", -0.7704762373473986);
            put("schwyz", -0.546017432109911);
            put("obwalden", -0.6948805167346832);
            put("st_gallen", -0.02205312805194748);
            put("uri", 0.14453512614033795);
            put("valais", 0.44964682041072296);
            put("schaffhausen", -0.640589108297238);
            put("ticino", 0.858983350149303);
            put("nidwalden", -0.47241660155153875);
        }};
        // global parameters
        parameters.lambdaCostIncome = -0.10925065017144632;
        parameters.referenceIncome = 6000.0;
        parameters.timeScale_min = 10.0;
        parameters.distanceScale_km = 10.0;
        parameters.lambdaCostEuclideanDistance = -0.32028229682408843;
        parameters.referenceEuclideanDistance_km = 15.0;
        parameters.betaCost_u_MU = -0.14290604883639313;
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
