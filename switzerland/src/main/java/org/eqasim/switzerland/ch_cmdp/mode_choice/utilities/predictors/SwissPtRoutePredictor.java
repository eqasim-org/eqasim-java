package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;

import java.util.ArrayList;
import java.util.Collections;
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
        Map<String, List<Zone>> filtered = new HashMap<String, List<Zone>>();

        for (List<Zone> zonesAtStop : zonesByStop.values()) {

            int maxPriority = zonesAtStop.stream()
                .mapToInt(z -> z.getAuthority().getPriority())
                .max()
                .orElse(Integer.MIN_VALUE);

            List<Zone> highestPriorityZones = zonesAtStop.stream()
                .filter(z -> z.getAuthority().getPriority() == maxPriority)
                .collect(Collectors.toList());

            // group zones by authority at this stop
            Map<Authority, List<Zone>> byAuth = highestPriorityZones.stream()
                    .collect(Collectors.groupingBy(Zone::getAuthority));

            for (Map.Entry<Authority, List<Zone>> e : byAuth.entrySet()) {

                Authority auth = e.getKey();
                List<Zone> zones = e.getValue();

                // If exactly one zone covers this stop for this authority,
                // this zone is definitely crossed
                if (zones.size() == 1) {
                    Zone z = zones.get(0);
                    filtered
                        .computeIfAbsent(auth.getId(), a -> new ArrayList<>())
                        .add(z);
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

                        //System.out.println("   We identified the following applicable authorities and zones: " + zones.toString());

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