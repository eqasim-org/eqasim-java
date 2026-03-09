package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtVariables;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs.Zone;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;

public class SwissPtRoutePredictor extends CachedVariablePredictor<SwissPtVariables> {

    private final TransitSchedule schedule;
    private final ZonalRegistry zonalRegistry;
    private final NetworkOfDistances networkOfDistances;

    @Inject
	public SwissPtRoutePredictor(TransitSchedule schedule, ZonalRegistry zonalRegistry, NetworkOfDistances sbbNetwork) {
		this.schedule      = schedule;
        this.zonalRegistry = zonalRegistry;
        this.networkOfDistances = sbbNetwork;
	}

    private Map<String, List<Zone>> cleanZoneInfo(Map<String, List<Zone>> zonesByStop) {
        List<String> stopIds = new ArrayList<>(zonesByStop.keySet());

        if (stopIds.isEmpty()) return new HashMap<>();

        // STEP 1: For each stop, deduplicate zones and group by authority
        Map<String, Map<Authority, List<Zone>>> zonesByStopAndAuth = new HashMap<>();

        for (String stopId : stopIds) {
            List<Zone> zonesAtStop = zonesByStop.get(stopId);

            // Dedup zones
            Map<String, Zone> uniqueZones = zonesAtStop.stream()
            .collect(Collectors.toMap(
                Zone::getZoneId,
                z -> z,
                (e1, e2) -> e1.getAuthority().getPriority() >= e2.getAuthority().getPriority() ? e1 : e2
            ));

            // Group by authority
            Map<Authority, List<Zone>> byAuth = uniqueZones.values().stream()
                .collect(Collectors.groupingBy(Zone::getAuthority));
            zonesByStopAndAuth.put(stopId, byAuth);
        }

        // STEP 2: Find common authorities across ALL stops, then pick highest priority among them
        Set<Authority> authoritiesAtEveryStop = zonesByStopAndAuth.values().stream()
            .map(Map::keySet)
            .reduce((a, b) -> {
                Set<Authority> intersection = new HashSet<>(a);
                intersection.retainAll(b);
                return intersection;
            })
            .orElse(new HashSet<>());

        if (authoritiesAtEveryStop.isEmpty()) {
            //System.out.println("WARNING: No common authority found across all stops!");
            return new HashMap<>();
        }

        // Pick the highest priority among common authorities
        int highestCommonPriority = authoritiesAtEveryStop.stream()
            .mapToInt(Authority::getPriority)
            .max()
            .getAsInt();

        Set<Authority> selectedAuthorities = authoritiesAtEveryStop.stream()
            .filter(a -> a.getPriority() == highestCommonPriority)
            .collect(Collectors.toSet());

        // STEP 3: Collect zones for selected authorities (globally deduplicated)
        Map<String, Set<String>> seenZonesByAuth = new HashMap<>();
        Map<String, List<Zone>> filtered = new HashMap<>();

        for (String stopId : stopIds) {
            Map<Authority, List<Zone>> authsAtStop = zonesByStopAndAuth.get(stopId);

            for (Authority auth : selectedAuthorities) {
                List<Zone> zones = authsAtStop.getOrDefault(auth, List.of());

                for (Zone zone : zones) {
                    String authId = auth.getId();
                    String zoneId = zone.getZoneId();
                    if (seenZonesByAuth.computeIfAbsent(authId, k -> new HashSet<>()).add(zoneId)) {
                        filtered.computeIfAbsent(authId, k -> new ArrayList<>()).add(zone);
                    }
                }
            }
        }

        return filtered;
    }


    private Stream<Zone> getZones(ZonalRegistry registry, String stopId) {
        // Direct match
        Stream<Zone> direct = registry.getZones(stopId).stream();

        // Simplified match
        String simplifiedId = stopId.contains(":") ? stopId.split("[:\\.]")[0] : stopId;
        Stream<Zone> simplified = registry.getZones(simplifiedId).stream();

        // Merge both streams
        return Stream.concat(direct, simplified).distinct();
    }


    private double computeSBBDistance(Map<String, List<Zone>> zones, String fromId, String toId){

        String simplifiedFromId = fromId.contains(":") ? fromId.split("[:\\.]")[0] : fromId;
        String simplifiedToId   = toId.contains(":") ? toId.split("[:\\.]")[0] : toId;

        if (zones.containsKey("SBB")) {
            return networkOfDistances.getDistance(simplifiedFromId, simplifiedToId);
        }

        return -1.0;

    }


    public SwissPtVariables predictPtVariables(List<? extends PlanElement> elements){
        SwissPtVariables tripDescription = new SwissPtVariables();

        Map<String, List<Zone>> zones = new HashMap<String, List<Zone>>();

        if (elements == null || elements.isEmpty()){
            return tripDescription;
        }

        for (PlanElement element : elements) {
			if (element instanceof Leg) {

                zones   = new HashMap<String, List<Zone>>();
				Leg leg = (Leg) element;

                double departureTime = leg.getDepartureTime().seconds();
                double arrivalTime   = departureTime + leg.getTravelTime().seconds();

				if ("pt".equals(leg.getMode())) {
					Route route = leg.getRoute();

					if (route instanceof TransitPassengerRoute) {
						TransitPassengerRoute ptRoute = (TransitPassengerRoute) route;

                        TransitLine transitLine   = schedule.getTransitLines().get(ptRoute.getLineId());
                        TransitRoute transitRoute = transitLine.getRoutes().get(ptRoute.getRouteId());

                        TransitStopFacility accessStop = schedule.getFacilities().get(ptRoute.getAccessStopId());
                        TransitStopFacility egressStop = schedule.getFacilities().get(ptRoute.getEgressStopId());

                        List<TransitStopFacility> intermediateStops = transitRoute.getStops().stream()
                            .map(x -> x.getStopFacility())
                            .collect(Collectors.toList());

                        int start = intermediateStops.indexOf(accessStop);
                        int end   = intermediateStops.indexOf(egressStop);

                        if (start <= end){
                            intermediateStops = intermediateStops.subList(start, end+1);
                        }
                        else{
                            intermediateStops = intermediateStops.subList(end, start+1);
                        }     

                        List<String> visitedStopIds = new ArrayList<String>();
                        for (TransitStopFacility stop : intermediateStops){
                            visitedStopIds.add(stop.getName());
                        }
                        
                        List<String> stopsVisited = intermediateStops.stream()
                            .map(stop -> stop.getId().toString().replaceAll("\\.link.*$", ""))
                            .collect(Collectors.toList());

                        String accessStopId = ptRoute.getAccessStopId().toString().replaceAll("\\.link.*$", "");
                        String egressStopId = ptRoute.getEgressStopId().toString().replaceAll("\\.link.*$", "");

                        Map<String, List<Zone>> stopsAndZones = new HashMap<>();

                        stopsAndZones.put(accessStopId, getZones(zonalRegistry, accessStopId).collect(Collectors.toList()));
                        stopsAndZones.put(egressStopId, getZones(zonalRegistry, egressStopId).collect(Collectors.toList()));

                        for (String stopId : stopsVisited) {
                            stopsAndZones.put(stopId, getZones(zonalRegistry, stopId).collect(Collectors.toList()));
                        }

                        zones = cleanZoneInfo(stopsAndZones);

                        double sbbDistance = computeSBBDistance(zones, accessStopId, egressStopId);
                        double networkDistance = route.getDistance();

                        //System.out.println("We identified the following applicable authorities and zones: " + zones.toString());

                        SwissPtLegVariables legVariables = new SwissPtLegVariables(zones, departureTime, arrivalTime, networkDistance, sbbDistance, accessStopId, egressStopId, accessStop.getName(), egressStop.getName());
                        tripDescription.addStage(legVariables);

					}                     
                    else {
						System.out.println("PT leg has no TransitPassengerRoute (route is " +
							(route == null ? "null" : route.getClass().getSimpleName()) + ")");
					}
				}
			}
		}

        return tripDescription;
    }

    @Override
    protected SwissPtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        return predictPtVariables(elements);
    }
    
}