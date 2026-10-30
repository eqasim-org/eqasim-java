package org.eqasim.switzerland.ch_cmdp.utils.routing;

import java.io.File;
import java.util.function.DoubleUnaryOperator;
import java.util.List;
import java.util.ArrayList;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.switzerland.ch_cmdp.config.SwissPTZonesConfigGroup;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.SwissCarCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.SwissWeissCarCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.PricingDescriptionReader;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.SBBDistanceReader;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.ZonalReader;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.ZonalRegistry;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.RunComputeTransitPrices;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.RunComputeTransitPrices.CSVRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Cost inputs shared by all workers, loaded only when cost estimation is enabled. */
final class RouterCosts {
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Compact, self-contained fare inputs persisted in the routed CSV. */
    public record FareProfile(String identifier, double originX, double originY, double destinationX,
            double destinationY, double departureTime, double homeX, double homeY, boolean hasGA,
            boolean hasHalbtaxSubscription, boolean hasVerbundAbo, boolean hasStreckenAbo,
            boolean hasGleis7Abo, boolean hasJuniorAbo, int age) {
        CSVRequest request() {
            if (age < 0 || !Double.isFinite(originX) || !Double.isFinite(originY)
                    || !Double.isFinite(destinationX) || !Double.isFinite(destinationY)
                    || !Double.isFinite(homeX) || !Double.isFinite(homeY)
                    || !Double.isFinite(departureTime) || departureTime < 0) {
                throw new IllegalArgumentException("Invalid saved PT traveler profile");
            }
            return new CSVRequest(identifier, originX, originY, destinationX, destinationY, departureTime,
                    homeX, homeY, hasGA, hasHalbtaxSubscription, hasVerbundAbo, hasStreckenAbo,
                    hasGleis7Abo, hasJuniorAbo, age);
        }
    }

    public record PtLeg(String lineId, String routeId, String accessStopId, String egressStopId,
            double departureTime, double travelTime, double distance, double boardingTime) {}

    public record SavedPtRoute(FareProfile traveler, List<PtLeg> legs) {
        List<Leg> restore(TransitSchedule schedule) {
            if (traveler == null || legs == null) {
                throw new IllegalArgumentException("pt_cost_data requires traveler and legs");
            }
            List<Leg> restored = new ArrayList<>(legs.size());
            for (PtLeg item : legs) {
                if (item == null || item.lineId == null || item.routeId == null || item.accessStopId == null
                        || item.egressStopId == null || !Double.isFinite(item.distance) || item.distance < 0
                        || !Double.isFinite(item.departureTime) || item.departureTime < 0
                        || !Double.isFinite(item.travelTime) || item.travelTime < 0 || !Double.isFinite(item.boardingTime)) {
                    throw new IllegalArgumentException("Invalid saved PT leg");
                }
                TransitLine line = schedule.getTransitLines().get(Id.create(item.lineId, TransitLine.class));
                TransitRoute route = line == null ? null : line.getRoutes().get(Id.create(item.routeId, TransitRoute.class));
                TransitStopFacility from = schedule.getFacilities().get(Id.create(item.accessStopId, TransitStopFacility.class));
                TransitStopFacility to = schedule.getFacilities().get(Id.create(item.egressStopId, TransitStopFacility.class));
                if (route == null || from == null || to == null || route.getStops().stream().noneMatch(stop -> stop.getStopFacility().equals(from))
                        || route.getStops().stream().noneMatch(stop -> stop.getStopFacility().equals(to))) {
                    throw new IllegalArgumentException("Saved PT itinerary is not present in the loaded schedule");
                }
                var passengerRoute = new DefaultTransitPassengerRoute(from, line, route, to);
                passengerRoute.setDistance(item.distance);
                passengerRoute.setTravelTime(item.travelTime);
                passengerRoute.setBoardingTime(item.boardingTime);
                Leg leg = PopulationUtils.getFactory().createLeg("pt");
                leg.setDepartureTime(item.departureTime);
                leg.setTravelTime(item.travelTime);
                leg.setRoute(passengerRoute);
                restored.add(leg);
            }
            return restored;
        }
    }

    static String save(CSVRequest request, List<? extends PlanElement> elements) {
        List<PtLeg> legs = new ArrayList<>();
        for (PlanElement element : elements) {
            if (element instanceof Leg leg && leg.getRoute() instanceof TransitPassengerRoute route) {
                legs.add(new PtLeg(route.getLineId().toString(), route.getRouteId().toString(),
                        route.getAccessStopId().toString(), route.getEgressStopId().toString(),
                        leg.getDepartureTime().seconds(), leg.getTravelTime().seconds(), route.getDistance(),
                        route.getBoardingTime().seconds()));
            }
        }
        var profile = new FareProfile(request.requestId, request.originX, request.originY, request.destinationX,
                request.destinationY, request.departureTime_s, request.homeX, request.homeY, request.hasGA,
                request.hasHalbtaxSubscription, request.hasVerbundAbo, request.hasStreckenAbo,
                request.hasGleis7Abo, request.hasJuniorAbo, request.age);
        try {
            return JSON.writeValueAsString(new SavedPtRoute(profile, legs));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize PT fare inputs", e);
        }
    }

    static SavedPtRoute read(String data) {
        try {
            return JSON.readValue(data, SavedPtRoute.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid pt_cost_data JSON", e);
        }
    }

    record PtPricer(SwissPtRoutePredictor predictor, TransitSchedule schedule, PtPricing pricing) {
        double price(CSVRequest request, List<? extends PlanElement> elements) {
            return RunComputeTransitPrices.calculatePrices(request, predictor.predictPtVariables(elements),
                    pricing.calculators, pricing.regionalRadius_km).price();
        }

        double price(SavedPtRoute saved) {
            List<Leg> legs = saved.restore(schedule);
            return price(saved.traveler.request(), legs);
        }
    }
    static SwissCostParameters parameters(Config config) {
        SwissCostParameters parameters = SwissCostParameters.buildDefault();
        ConfigGroup eqasim = config == null ? null : config.getModules().get(EqasimConfigGroup.GROUP_NAME);
        String path = eqasim == null ? null : eqasim.getParams().get("costParametersPath");
        // Generic MATSim config groups retain the XML spelling of an absent optional path.
        if (path != null && !path.isBlank() && !path.equals("null")) {
            ParameterDefinition.applyFile(file(config, path), parameters);
        }
        if (!Double.isFinite(parameters.carCost_CHF_km) || parameters.carCost_CHF_km < 0
                || !Double.isFinite(parameters.ptRegionalRadius_km) || parameters.ptRegionalRadius_km < 0) {
            throw new IllegalArgumentException("Invalid carCost_CHF_km or ptRegionalRadius_km in cost parameters");
        }
        return parameters;
    }

    static DoubleUnaryOperator carModel(String model, SwissCostParameters parameters) {
        return switch (model) {
            case "simple" -> new SwissCarCostModel(parameters)::calculateCost_CHF;
            case "weiss" -> new SwissWeissCarCostModel(parameters)::calculateCost_CHF;
            default -> throw new IllegalArgumentException("car-cost-model must be simple or weiss");
        };
    }

    record PtPricing(ZonalRegistry zones, NetworkOfDistances distances, SwissPtStageCostCalculator calculators,
            double regionalRadius_km) {
        PtPricer pricer(TransitSchedule schedule) {
            return new PtPricer(new SwissPtRoutePredictor(schedule, zones, distances), schedule, this);
        }
    }

    static PtPricing ptPricing(Config config, SwissCostParameters parameters) {
        var ptZones = ConfigUtils.addOrGetModule(config, SwissPTZonesConfigGroup.class);
        if (ptZones.getZonePath() == null || ptZones.getSBBDistancesPath() == null
                || ptZones.getPricingDescriptionPath() == null) {
            throw new IllegalArgumentException("compute-cost requires complete ptZones configuration for PT: "
                    + "ptZonesFilePath, sbbDistancesPath and pricingDescriptionPath");
        }
        try {
            var reader = new ZonalReader();
            File zoneFile = file(config, ptZones.getZonePath());
            var authorities = reader.readTarifNetworks(zoneFile);
            var zones = new ZonalRegistry(authorities, reader.readZones(zoneFile, authorities));
            File distanceFile = file(config, ptZones.getSBBDistancesPath());
            zones.merge(SBBDistanceReader.createZonalRegistry(SBBDistanceReader.createZone(distanceFile)));
            return new PtPricing(zones, SBBDistanceReader.createNetworkOfDistances(distanceFile),
                    PricingDescriptionReader.readPriceDescription(file(config, ptZones.getPricingDescriptionPath())),
                    parameters.ptRegionalRadius_km);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load PT pricing inputs: " + e.getMessage(), e);
        }
    }

    private static File file(Config config, String path) {
        try {
            return new File(ConfigGroup.getInputFileURL(config.getContext(), path).toURI());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid local pricing input path: " + path, e);
        }
    }
}
