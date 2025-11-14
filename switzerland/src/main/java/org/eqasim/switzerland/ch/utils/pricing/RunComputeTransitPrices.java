package org.eqasim.switzerland.ch.utils.pricing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eqasim.switzerland.ch.config.SwissPTZonesConfigGroup;
import org.eqasim.switzerland.ch.mode_choice.costs.pt.PtStageCostCalculator;
import org.eqasim.switzerland.ch.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPtVariables;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Authority;
import org.eqasim.switzerland.ch.utils.pricing.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch.utils.pricing.inputs.SBBDistanceReader;
import org.eqasim.switzerland.ch.utils.pricing.inputs.ZonalReader;
import org.eqasim.switzerland.ch.utils.pricing.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Zone;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
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

        public void writeCSV(Map<Integer, CSVRequest> requests, Map<Integer, Double> prices, Map<Integer, Double> oldPrices, Map<Integer, Double> distances) throws IOException {
            try (CSVWriter writer = new CSVWriter(new FileWriter(this.csvOuptputPath))){
                String[] header = { "id",
                    "originX", "originY", "destinationX", "destinationY", "departureTime_s",
                    "homeX", "homeY",
                    "hasGA", "hasHalbtaxSubscription", "hasVerbundAbo",
                    "hasStreckenAbo", "hasGleis7Abo", "hasJuniorAbo", "age",
                    "price", "priceOldModel", "networkDistance"
                };
                writer.writeNext(header);

                for (Map.Entry<Integer, CSVRequest> entry : requests.entrySet()){
                    int id = entry.getKey();
                    CSVRequest req = entry.getValue();
                    double price = prices.getOrDefault(id, Double.NaN);
                    double oldPrice = oldPrices.getOrDefault(id, Double.NaN);
                    double distance = distances.getOrDefault(id, Double.NaN);

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
                        Double.toString(distance)
                    };
                    writer.writeNext(row);
                }
            }
        }
    }

    public static double calculateHomeDistance_km(CSVRequest request){
        double originHomeDistance_km = CoordUtils.calcEuclideanDistance(
            new Coord(request.originX, request.originY), new Coord(request.homeX, request.homeY));
        double destinationHomeDistance_km = CoordUtils.calcEuclideanDistance(
            new Coord(request.destinationX, request.destinationY), new Coord(request.homeX, request.homeY));
        return Math.max(originHomeDistance_km, destinationHomeDistance_km);
    }


    public static double processPriceRequest(int id, CSVRequest request, Network network, SwissRailRaptor router, SwissPtRoutePredictor ptRoutePredictor, 
        SwissPtStageCostCalculator swissPtStageCostCalculator){

            Coord fromCoord = new Coord(request.originX, request.originY);
            Coord toCoord   = new Coord(request.destinationX, request.destinationY);

            Link fromLink = NetworkUtils.getNearestLink(network, fromCoord);
            Link toLink   = NetworkUtils.getNearestLink(network, toCoord);

            Facility fromFacility = FacilitiesUtils.wrapLinkAndCoord(fromLink, fromCoord);
            Facility toFacility = FacilitiesUtils.wrapLinkAndCoord(toLink, toCoord);

            try {
                List<? extends PlanElement> route = router.calcRoute(
                    DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, request.departureTime_s, null));

                if (route == null || route.isEmpty()){
                    return Double.NaN;
                }
                
                SwissPtVariables ptVariables = ptRoutePredictor.predictPtVariables(route);
                Map<String, List<SwissPtLegVariables>> groupedByAuthority =  ptVariables.getPricingStrategy();

                if (ptVariables.legVariables == null || ptVariables.legVariables.isEmpty()){
                    return Double.NaN;
                }

                double price = 0.0;

                // Junior abo should only be for kids and teens travelling with at least one parent...
                if (request.hasGA || request.age < 6 || (request.hasJuniorAbo && request.age < 16)){
                    return 0.0;
                }

                // TODO improve later
                if (request.hasVerbundAbo) {
                    double homeDistance_km = calculateHomeDistance_km(request);

                    if (homeDistance_km <= 15.0) {
                        return 0.0;
                    }
                }

                if (request.departureTime_s >= 19*3600 && request.age < 25 && request.hasGleis7Abo){
                    return 0.0;
                }

                boolean halfFareTariff = request.hasHalbtaxSubscription || (request.age < 16);

                for (Map.Entry<String, List<SwissPtLegVariables>> legEntry : groupedByAuthority.entrySet()){
                    String authority                        = legEntry.getKey();
                    List<SwissPtLegVariables> authorityLegs = legEntry.getValue();
                    PtStageCostCalculator calculator        = swissPtStageCostCalculator.priceCalculators.get("None");

                    if (swissPtStageCostCalculator.priceCalculators.containsKey(authority)){
                        calculator = swissPtStageCostCalculator.priceCalculators.get(authority);				
                    }

                    price += calculator.calculatePrice(authorityLegs, halfFareTariff, authority);
                }

                return price;
            }
            catch (Exception e){
                System.err.println("Routing failed for request " + id + ": " + e.getMessage());
                return Double.NaN;
            }
    }


    public static double processPriceRequestBefore(int id, CSVRequest request, Network network, SwissRailRaptor router, SwissPtRoutePredictor ptRoutePredictor, 
        SwissPtStageCostCalculator swissPtStageCostCalculator){

            Coord fromCoord = new Coord(request.originX, request.originY);
            Coord toCoord   = new Coord(request.destinationX, request.destinationY);

            Link fromLink = NetworkUtils.getNearestLink(network, fromCoord);
            Link toLink   = NetworkUtils.getNearestLink(network, toCoord);

            Facility fromFacility = FacilitiesUtils.wrapLinkAndCoord(fromLink, fromCoord);
            Facility toFacility = FacilitiesUtils.wrapLinkAndCoord(toLink, toCoord);

            try {
                List<? extends PlanElement> route = router.calcRoute(
                    DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, request.departureTime_s, null));

                if (route == null || route.isEmpty()){
                    return Double.NaN;
                }
                
                SwissPtVariables ptVariables = ptRoutePredictor.predictPtVariables(route);
                Map<String, List<SwissPtLegVariables>> groupedByAuthority =  ptVariables.getPricingStrategy();

                if (ptVariables.legVariables == null || ptVariables.legVariables.isEmpty()){
                    return Double.NaN;
                }

                double distance = 0.0;

                // Junior abo should only be for kids and teens travelling with at least one parent...
                if (request.hasGA || request.age < 6 || (request.hasJuniorAbo && request.age < 16)){
                    return 0.0;
                }

                // TODO improve later
                if (request.hasVerbundAbo) {
                    double homeDistance_km = calculateHomeDistance_km(request);

                    if (homeDistance_km <= 15.0) {
                        return 0.0;
                    }
                }

                if (request.departureTime_s >= 19*3600 && request.age < 25 && request.hasGleis7Abo){
                    return 0.0;
                }

                boolean halfFareTariff = request.hasHalbtaxSubscription || (request.age < 16);

                for (Map.Entry<String, List<SwissPtLegVariables>> legEntry : groupedByAuthority.entrySet()){
                    List<SwissPtLegVariables> authorityLegs = legEntry.getValue();

                    for (SwissPtLegVariables legVar : authorityLegs){
                        distance += legVar.networkDistance;
                    }
                }

                distance = distance / 1000.0;

                double oldPriceModel = 0.6 * distance;
                if (halfFareTariff){
                    oldPriceModel /= 2;
                }
                oldPriceModel = Math.round(oldPriceModel * 100.0) / 100.0;

                return oldPriceModel;
            }
            catch (Exception e){
                System.err.println("Routing failed for request " + id + ": " + e.getMessage());
                return Double.NaN;
            }
    }

    public static double computeNetworkDistance(int id, CSVRequest request, Network network, SwissRailRaptor router, SwissPtRoutePredictor ptRoutePredictor, 
        SwissPtStageCostCalculator swissPtStageCostCalculator){

            Coord fromCoord = new Coord(request.originX, request.originY);
            Coord toCoord   = new Coord(request.destinationX, request.destinationY);

            Link fromLink = NetworkUtils.getNearestLink(network, fromCoord);
            Link toLink   = NetworkUtils.getNearestLink(network, toCoord);

            Facility fromFacility = FacilitiesUtils.wrapLinkAndCoord(fromLink, fromCoord);
            Facility toFacility = FacilitiesUtils.wrapLinkAndCoord(toLink, toCoord);

            try {
                List<? extends PlanElement> route = router.calcRoute(
                    DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, request.departureTime_s, null));

                if (route == null || route.isEmpty()){
                    return Double.NaN;
                }
                
                SwissPtVariables ptVariables = ptRoutePredictor.predictPtVariables(route);
                Map<String, List<SwissPtLegVariables>> groupedByAuthority =  ptVariables.getPricingStrategy();

                if (ptVariables.legVariables == null || ptVariables.legVariables.isEmpty()){
                    return Double.NaN;
                }

                double distance = 0.0;

                for (Map.Entry<String, List<SwissPtLegVariables>> legEntry : groupedByAuthority.entrySet()){
                    List<SwissPtLegVariables> authorityLegs = legEntry.getValue();

                    for (SwissPtLegVariables legVar : authorityLegs){
                        distance += legVar.networkDistance;
                    }
                }

                return distance;
            }
            catch (Exception e){
                System.err.println("Routing failed for request " + id + ": " + e.getMessage());
                return Double.NaN;
            }
    }


    static public void main(String[] args) throws ConfigurationException, IOException, CsvValidationException {

		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "requests-path", "output-path") //
				.allowPrefixes("mode-parameter", "cost-parameter", "preventwaitingtoentertraffic", "samplingRateForPT") //
				.build();

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
        Scenario scenario = ScenarioUtils.loadScenario(config);

        SwissPTZonesConfigGroup ptZonesConfig = ConfigUtils.addOrGetModule(config, SwissPTZonesConfigGroup.class);        
        String ptZonesFilePath = ptZonesConfig.getZonePath();
        String sbbDistances    = ptZonesConfig.getSBBDistancesPath();

        ZonalReader zonalReader = new ZonalReader();

        if (ptZonesFilePath == null | sbbDistances == null){
            throw new ConfigurationException("ptZones configuration is incomplete: "
                + "ptZonesFilePath=" + ptZonesFilePath
                + ", sbbDistances=" + sbbDistances);
        }
        
        File zonesPath = new File(ptZonesFilePath);
        Collection<Authority> authorities = zonalReader.readTarifNetworks(zonesPath);
		Collection<Zone> zones = zonalReader.readZones(zonesPath, authorities);
		ZonalRegistry zonalRegistry = new ZonalRegistry(authorities, zones);

        File sbbPath = new File(sbbDistances);
        Zone sbbZone = SBBDistanceReader.createZone(sbbPath);
        ZonalRegistry sbbZonalRegistry = SBBDistanceReader.createZonalRegistry(sbbZone);
        zonalRegistry.merge(sbbZonalRegistry);

        NetworkOfDistances sbbNetwork = SBBDistanceReader.createNetworkOfDistances(sbbPath);

        Network network = scenario.getNetwork();
        TransitSchedule transitSchedule = scenario.getTransitSchedule();

        OccupancyData occupancyData = new OccupancyData();
        RaptorStaticConfig srrStaticConfig = RaptorUtils.createStaticConfig(config);

        SwissRailRaptorData srrData = SwissRailRaptorData.create(transitSchedule,
				scenario.getTransitVehicles(), srrStaticConfig, network, occupancyData);

        ThreadLocal<SwissRailRaptor> threadLocalRouter = ThreadLocal.withInitial(() -> new SwissRailRaptor.Builder(srrData, config).build()); 
        ThreadLocal<SwissPtRoutePredictor> threadLocalPtRoutePredictor = ThreadLocal.withInitial(() -> new SwissPtRoutePredictor(transitSchedule, zonalRegistry, sbbNetwork)); 
        ThreadLocal<SwissPtStageCostCalculator> threadLocalPtStageCalculator = ThreadLocal.withInitial(() -> new SwissPtStageCostCalculator()); 

        CSVRequestReader csvRequestReader = new RunComputeTransitPrices.CSVRequestReader(cmd.getOptionStrict("requests-path"));
        csvRequestReader.readCSV();

        Map<Integer, Double> prices = csvRequestReader.requests.entrySet().parallelStream()  
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> processPriceRequest(
                    entry.getKey(),
                    entry.getValue(),
                    network,
                    threadLocalRouter.get(),
                    threadLocalPtRoutePredictor.get(),
                    threadLocalPtStageCalculator.get()
                )
            ));

        Map<Integer, Double> oldPrices = csvRequestReader.requests.entrySet().parallelStream()  
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> processPriceRequestBefore(
                    entry.getKey(),
                    entry.getValue(),
                    network,
                    threadLocalRouter.get(),
                    threadLocalPtRoutePredictor.get(),
                    threadLocalPtStageCalculator.get()
                )
            ));

        Map<Integer, Double> distances = csvRequestReader.requests.entrySet().parallelStream()  
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> computeNetworkDistance(
                    entry.getKey(),
                    entry.getValue(),
                    network,
                    threadLocalRouter.get(),
                    threadLocalPtRoutePredictor.get(),
                    threadLocalPtStageCalculator.get()
                )
            ));

        CSVRequestWriter writer = new RunComputeTransitPrices.CSVRequestWriter(cmd.getOptionStrict("output-path"));
        writer.writeCSV(csvRequestReader.requests, prices, oldPrices, distances);
    }    
}
