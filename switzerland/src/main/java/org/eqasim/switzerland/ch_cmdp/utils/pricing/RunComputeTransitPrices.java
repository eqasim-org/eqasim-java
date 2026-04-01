package org.eqasim.switzerland.ch_cmdp.utils.pricing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.ch_cmdp.config.SwissPTZonesConfigGroup;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.PtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtVariables;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.PricingDescriptionReader;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.SBBDistanceReader;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.ZonalReader;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.Zone;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import ch.sbb.matsim.routing.pt.raptor.OccupancyData;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;

public class RunComputeTransitPrices {
    public static double ptRegionalRadius_km = 10.0;

    public static class CSVRequest{
        public final String requestId;
        public final double originX;
        public final double originY;
        public final double destinationX;
        public final double destinationY;
        public final double departureTime_s;

        public final double homeX;
        public final double homeY;
        public final boolean hasGA;
        public final boolean hasHalbtaxSubscription;
        public final boolean hasVerbundAbo;
        public final boolean hasStreckenAbo;
        public final boolean hasGleis7Abo;
        public final boolean hasJuniorAbo;
        public final int age;

        public CSVRequest(
            String ID,
            double originX,
            double originY,
            double destinationX,
            double destinationY,
            double departureTime_s,
            double homeX,
            double homeY,
            boolean hasGA,
            boolean hasHalbtaxSubscription,
            boolean hasVerbundAbo,
            boolean hasStreckenAbo,
            boolean hasGleis7Abo,
            boolean hasJuniorAbo,
            int personAge
        ) {
            this.requestId = ID;
            this.originX = originX;
            this.originY = originY;
            this.destinationX = destinationX;
            this.destinationY = destinationY;
            this.departureTime_s = departureTime_s;
            this.homeX = homeX;
            this.homeY = homeY;
            this.hasGA = hasGA;
            this.hasHalbtaxSubscription = hasHalbtaxSubscription;
            this.hasVerbundAbo = hasVerbundAbo;
            this.hasStreckenAbo = hasStreckenAbo;
            this.hasGleis7Abo = hasGleis7Abo;
            this.hasJuniorAbo = hasJuniorAbo;
            this.age = personAge;
        }
    }

    public static class CSVRequestReader{
        public String csvPath;
        public Map<Integer, CSVRequest> requests = new HashMap<>();

        public CSVRequestReader(String csvPath){
            this.csvPath = csvPath;
        }

        public void readCSV() throws FileNotFoundException, IOException, CsvValidationException{
            File csv = new File(this.csvPath);
            try(CSVReader reader = new CSVReader(new FileReader(csv))){
                String[] headers = reader.readNext();

                if (headers == null) {
                    throw new IOException("CSV file is empty: " + csvPath);
                }

                Map<String, Integer> colIndex = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    colIndex.put(headers[i].trim(), i);
                }

                String[] row;
                int id = 0;
                while ((row = reader.readNext()) != null) {
                    String theID   = row[colIndex.get("ID")];
                    double originX = Double.parseDouble(row[colIndex.get("originX")]);
                    double originY = Double.parseDouble(row[colIndex.get("originY")]);
                    double destinationX = Double.parseDouble(row[colIndex.get("destinationX")]);
                    double destinationY = Double.parseDouble(row[colIndex.get("destinationY")]);
                    double departureTime_s = Double.parseDouble(row[colIndex.get("departureTime_s")]);

                    double homeX = Double.parseDouble(row[colIndex.get("homeX")]);
                    double homeY = Double.parseDouble(row[colIndex.get("homeY")]);

                    boolean hasGA = Boolean.parseBoolean(row[colIndex.get("hasGA")]);
                    boolean hasHalbtaxSubscription = Boolean.parseBoolean(row[colIndex.get("hasHalbtaxSubscription")]);
                    boolean hasVerbundAbo = Boolean.parseBoolean(row[colIndex.get("hasVerbundAbo")]);
                    boolean hasStreckenAbo = Boolean.parseBoolean(row[colIndex.get("hasStreckenAbo")]);
                    boolean hasGleis7Abo = Boolean.parseBoolean(row[colIndex.get("hasGleis7Abo")]);
                    boolean hasJuniorAbo = Boolean.parseBoolean(row[colIndex.get("hasJuniorAbo")]);
                    int age = Integer.parseInt(row[colIndex.get("age")]);

                    // 5. Create request
                    CSVRequest request = new CSVRequest(theID,
                            originX, originY, destinationX, destinationY, departureTime_s,
                            homeX, homeY,
                            hasGA, hasHalbtaxSubscription, hasVerbundAbo,
                            hasStreckenAbo, hasGleis7Abo, hasJuniorAbo,
                            age
                    );
                
                    requests.put(id++, request);
                }
            }
        }
    }

    public static class CSVRequestWriter {
        public final String csvOuptputPath;

        public CSVRequestWriter(String csvPath){
            this.csvOuptputPath = csvPath;
        }

        public void writeCSV(Map<Integer, CSVRequest> requests, Map<Integer, Double> prices, Map<Integer, Double> oldPrices, Map<Integer, Double> distances,
             Map<Integer, Double> travelTimes,
             Map<Integer, Double> inVehicleTimes,
             Map<Integer, Double> accessEgressTimes,
             Map<Integer, Double> waitingTimes,
             Map<Integer, Integer> nbTransfers
            ) throws IOException {
            try (CSVWriter writer = new CSVWriter(new FileWriter(this.csvOuptputPath))){
                String[] header = { "id",
                    "originX", "originY", "destinationX", "destinationY", "departureTime_s",
                    "homeX", "homeY",
                    "hasGA", "hasHalbtaxSubscription", "hasVerbundAbo",
                    "hasStreckenAbo", "hasGleis7Abo", "hasJuniorAbo", "age",
                    "price", "priceOldModel", "networkDistance", "total_travel_time_min",
                    "in_vehicle_time_min", "access_egress_time_min", "waiting_time_min", "number_of_line_switches"
                };
                writer.writeNext(header);

                for (Map.Entry<Integer, CSVRequest> entry : requests.entrySet()){
                    int id         = entry.getKey();
                    CSVRequest req = entry.getValue();

                    double price            = prices.getOrDefault(id, Double.NaN);
                    double oldPrice         = oldPrices.getOrDefault(id, Double.NaN);
                    double distance         = distances.getOrDefault(id, Double.NaN);
                    double travelTime       = travelTimes.getOrDefault(id, Double.NaN);
                    double invehicleTime    = inVehicleTimes.getOrDefault(id, Double.NaN);
                    double accessEgressTime = accessEgressTimes.getOrDefault(id, Double.NaN);
                    double waitingTime      = waitingTimes.getOrDefault(id, Double.NaN);
                    int nbLineSwitches      = nbTransfers.getOrDefault(id, 0);

                    String[] row = {
                        req.requestId,
                        Double.toString(req.originX),
                        Double.toString(req.originY),
                        Double.toString(req.destinationX),
                        Double.toString(req.destinationY),
                        Double.toString(req.departureTime_s),
                        Double.toString(req.homeX),
                        Double.toString(req.homeY),
                        Boolean.toString(req.hasGA),
                        Boolean.toString(req.hasHalbtaxSubscription),
                        Boolean.toString(req.hasVerbundAbo),
                        Boolean.toString(req.hasStreckenAbo),
                        Boolean.toString(req.hasGleis7Abo),
                        Boolean.toString(req.hasJuniorAbo),
                        Integer.toString(req.age),
                        Double.toString(price),
                        Double.toString(oldPrice),
                        Double.toString(distance),
                        Double.toString(travelTime),
                        Double.toString(invehicleTime),
                        Double.toString(accessEgressTime),
                        Double.toString(waitingTime),
                        Integer.toString(nbLineSwitches)
                    };
                    writer.writeNext(row);
                }
            }
        }
    }

    public static class RouteResult {
        public final double price;
        public final double oldPrice;
        public final double distance_km;
        public final double totalTravelTime_min;
        public final double inVehicleTime_min;
        public final double accessEgressTime_min;
        public final double waitingTime_min;
        public final int numberTransfers;

        public RouteResult(double price, double oldPrice, double distance_km, double travelTime_min,
            double inVehicleTime_min, double access_egress_time_min, double waiting_time_min, int numberTransfers
        ) {
            this.price = price;
            this.oldPrice = oldPrice;
            this.distance_km = distance_km;
            this.totalTravelTime_min = travelTime_min;
            this.inVehicleTime_min = inVehicleTime_min;
            this.accessEgressTime_min = access_egress_time_min;
            this.waitingTime_min = waiting_time_min;
            this.numberTransfers = numberTransfers;
        }
    }


    public static RouteResult processRequest(
        int id, CSVRequest request,
        Network network,
        SwissRailRaptor router,
        SwissPtRoutePredictor ptRoutePredictor,
        SwissPtStageCostCalculator swissPtStageCostCalculator
    ) {
        try {
            Coord fromCoord = new Coord(request.originX, request.originY);
            Coord toCoord   = new Coord(request.destinationX, request.destinationY);

            Link fromLink = NetworkUtils.getNearestLink(network, fromCoord);
            Link toLink   = NetworkUtils.getNearestLink(network, toCoord);

            Facility fromFacility = FacilitiesUtils.wrapLinkAndCoord(fromLink, fromCoord);
            Facility toFacility   = FacilitiesUtils.wrapLinkAndCoord(toLink, toCoord);

            int age = request.age;
            PersonVariables personVariables = new PersonVariables(age);

            List<? extends PlanElement> route = router.calcRoute(
                DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, request.departureTime_s, null));

            if (route == null || route.isEmpty()) {
                return new RouteResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0);
            }

            // Compute travel time
            double travelTime_s         = 0.0;
            double in_vehicle_time_s    = 0.0;
            double waiting_time_s       = 0.0;
            double access_egress_time_s = 0.0;
            int nb_vehicular_legs       = 0;
            double currentTime          = request.departureTime_s;

            int cpt = 0;

            for (PlanElement pe : route) {
                if (pe instanceof Leg leg) {
                    if (leg.getMode().equals("walk")) {
                        double travelTime = leg.getRoute().getTravelTime().seconds();
                        travelTime_s         += travelTime;
                        if (cpt == 0){
                            access_egress_time_s += travelTime;
                        }
                        else if (cpt == route.size() - 1 ){
                            access_egress_time_s += travelTime;
                        }
                        currentTime += travelTime;
                    } else if (leg.getRoute() instanceof TransitPassengerRoute passengerRoute) {
                        double boardingTime = passengerRoute.getBoardingTime().seconds();
                        double waitTime     = boardingTime - currentTime;
                        double travelTime   = passengerRoute.getTravelTime().seconds();

                        nb_vehicular_legs += 1;
                        travelTime_s      += travelTime;
                        in_vehicle_time_s += travelTime - waitTime;
                        waiting_time_s    += waitTime;

                        currentTime += travelTime ;
                    }
                }
                cpt += 1;
            }

            double travelTime_min         = travelTime_s / 60.0;
            double access_egress_time_min = access_egress_time_s / 60.0;
            double waiting_time_min       = waiting_time_s / 60.0;
            double in_vehicle_time_min    = in_vehicle_time_s / 60.0;
            int number_of_line_switches   = 0;
            if (nb_vehicular_legs >= 2){
                number_of_line_switches = nb_vehicular_legs - 1;
            }

            // Predict PT variables once
            SwissPtVariables ptVariables = ptRoutePredictor.predictPtVariables(route);
            if (ptVariables.legVariables == null || ptVariables.legVariables.isEmpty()) {
                return new RouteResult(Double.NaN, Double.NaN, Double.NaN, travelTime_min, in_vehicle_time_min, access_egress_time_min, waiting_time_min, number_of_line_switches);
            }

            Map<String, List<SwissPtLegVariables>> groupedByAuthority = ptVariables.getPricingStrategy();

            // Compute network distance
            double distance_m = groupedByAuthority.values().stream()
                .flatMap(List::stream)
                .mapToDouble(lv -> lv.networkDistance)
                .sum();

            double distance_km = distance_m / 1000.0;

            // === Apply pricing logic once ===
            double price = 0.0;
            double oldPrice = 0.0;

            boolean isGleis7 = request.departureTime_s < 5 * 3600 || request.departureTime_s >= 19 * 3600;
            boolean hasGleis7FreeTravel = request.age < 25 && request.hasGleis7Abo && isGleis7;
            boolean hasFreePublicTransport = request.hasGA
                    || request.age < 6
                    || (request.age < 16 && request.hasJuniorAbo)
                    || hasGleis7FreeTravel;

            if (hasFreePublicTransport) {
                return new RouteResult(0.0, 0.0, distance_km, travelTime_min, in_vehicle_time_min, access_egress_time_min, waiting_time_min, number_of_line_switches);
            }

            boolean hasRegionalSubscription = request.hasVerbundAbo || request.hasStreckenAbo;
            if (hasRegionalSubscription) {
                double homeDist = calculateHomeDistance_km(request);
                if (homeDist <= ptRegionalRadius_km) {
                    return new RouteResult(0.0, 0.0, distance_km, travelTime_min, in_vehicle_time_min, access_egress_time_min, waiting_time_min, number_of_line_switches);
                }
            }

            boolean halfFare = request.hasHalbtaxSubscription || (request.age < 16);

            // New model price
            for (var entry : groupedByAuthority.entrySet()) {
                String authority = entry.getKey();
                List<SwissPtLegVariables> legs = entry.getValue();

                PtStageCostCalculator calc =
                    swissPtStageCostCalculator.priceCalculators.getOrDefault(authority,
                        swissPtStageCostCalculator.priceCalculators.get("None"));

                price += calc.calculatePrice(authority, legs, halfFare, personVariables);
            }

            // Old model price
            oldPrice = Math.max(2.8, 2*(0.21 * distance_km - 0.00015 * Math.pow(distance_km,2) ));
            if (halfFare) oldPrice /= 2.0;
            oldPrice = Math.round(oldPrice * 100.0) / 100.0;
            price    = Math.round(price * 100.0) / 100.0;

            double maximumPrice = halfFare? 35.0 : 60.0;
            price = Math.min(price, maximumPrice);
            oldPrice = Math.min(oldPrice, maximumPrice);

            return new RouteResult(price, oldPrice, distance_km, travelTime_min, in_vehicle_time_min, access_egress_time_min, waiting_time_min, number_of_line_switches);

        } catch (Exception e) {
            System.err.println("Routing failed for request " + id + ": " + e.getMessage());
            return new RouteResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, 0);
        }
    }


    public static double calculateHomeDistance_km(CSVRequest request){
        double originHomeDistance_km = CoordUtils.calcEuclideanDistance(
            new Coord(request.originX, request.originY), new Coord(request.homeX, request.homeY));
        double destinationHomeDistance_km = CoordUtils.calcEuclideanDistance(
            new Coord(request.destinationX, request.destinationY), new Coord(request.homeX, request.homeY));
        return Math.max(originHomeDistance_km, destinationHomeDistance_km);
    }

    @SuppressWarnings("null")
    static public void main(String[] args) throws Exception {

		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "requests-path", "output-path") //
                .allowPrefixes("ptRegionalRadius_km") //
				.build();

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
        Scenario scenario = ScenarioUtils.loadScenario(config);
        if (cmd.hasOption("ptRegionalRadius_km")){
            ptRegionalRadius_km = Double.parseDouble(cmd.getOptionStrict("ptRegionalRadius_km"));
        }

        SwissPTZonesConfigGroup ptZonesConfig = ConfigUtils.addOrGetModule(config, SwissPTZonesConfigGroup.class);        
        String ptZonesFilePath = ptZonesConfig.getZonePath();
        String sbbDistances    = ptZonesConfig.getSBBDistancesPath();
        String operators       = ptZonesConfig.getPricingDescriptionPath();

        ZonalReader zonalReader = new ZonalReader();

        if (ptZonesFilePath == null | sbbDistances == null | operators == null){
            throw new ConfigurationException("ptZones configuration is incomplete: "
                + "ptZonesFilePath=" + ptZonesFilePath
                + ", sbbDistances=" + sbbDistances
                + ", pricingDescription=" + operators);
        }
        
        File zonesPath = new File(ptZonesFilePath);
        Collection<Authority> authorities = zonalReader.readTarifNetworks(zonesPath);
		Collection<Zone> zones            = zonalReader.readZones(zonesPath, authorities);
		ZonalRegistry zonalRegistry       = new ZonalRegistry(authorities, zones);

        File sbbPath = new File(sbbDistances);
        Zone sbbZone = SBBDistanceReader.createZone(sbbPath);
        ZonalRegistry sbbZonalRegistry = SBBDistanceReader.createZonalRegistry(sbbZone);
        zonalRegistry.merge(sbbZonalRegistry);

        File pricingPath = new File(operators);     
        SwissPtStageCostCalculator swissPtStageCostCalculator = PricingDescriptionReader.readPriceDescription(pricingPath);

        NetworkOfDistances sbbNetwork = SBBDistanceReader.createNetworkOfDistances(sbbPath);

        Network network = scenario.getNetwork();
        TransitSchedule transitSchedule = scenario.getTransitSchedule();

        OccupancyData occupancyData = new OccupancyData();
        RaptorStaticConfig srrStaticConfig = RaptorUtils.createStaticConfig(config);

        SwissRailRaptorData srrData = SwissRailRaptorData.create(transitSchedule,
				scenario.getTransitVehicles(), srrStaticConfig, network, occupancyData);

        ThreadLocal<SwissRailRaptor> threadLocalRouter                       = ThreadLocal.withInitial(() -> new SwissRailRaptor.Builder(srrData, config).build()); 
        ThreadLocal<SwissPtRoutePredictor> threadLocalPtRoutePredictor       = ThreadLocal.withInitial(() -> new SwissPtRoutePredictor(transitSchedule, zonalRegistry, sbbNetwork)); 
        ThreadLocal<SwissPtStageCostCalculator> threadLocalPtStageCalculator = ThreadLocal.withInitial(() -> swissPtStageCostCalculator); 

        CSVRequestReader csvRequestReader = new RunComputeTransitPrices.CSVRequestReader(cmd.getOptionStrict("requests-path"));
        csvRequestReader.readCSV();

        System.out.println("CSV read.");

        Map<Integer, RouteResult> results = csvRequestReader.requests.entrySet()
        .parallelStream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> processRequest(
                entry.getKey(),
                entry.getValue(),
                network,
                threadLocalRouter.get(),
                threadLocalPtRoutePredictor.get(),
                threadLocalPtStageCalculator.get()
            )
        ));

        Map<Integer, Double> prices            = results.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().price));
        Map<Integer, Double> oldPrices         = results.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().oldPrice));
        Map<Integer, Double> distances         = results.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().distance_km));
        Map<Integer, Double> travelTimes       = results.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().totalTravelTime_min));
        Map<Integer, Double> inVehicleTimes    = results.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().inVehicleTime_min));
        Map<Integer, Double> accessEgressTimes = results.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().accessEgressTime_min));
        Map<Integer, Double> waitingTimes      = results.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().waitingTime_min));
        Map<Integer, Integer> nbTransfers      = results.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().numberTransfers));


        CSVRequestWriter writer = new RunComputeTransitPrices.CSVRequestWriter(cmd.getOptionStrict("output-path"));
        writer.writeCSV(csvRequestReader.requests, prices, oldPrices, distances, travelTimes, inVehicleTimes, accessEgressTimes, waitingTimes, nbTransfers);
    }    
}
