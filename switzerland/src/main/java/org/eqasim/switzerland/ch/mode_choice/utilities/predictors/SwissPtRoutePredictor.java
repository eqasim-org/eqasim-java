package org.eqasim.switzerland.ch.mode_choice.utilities.predictors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPtVariables;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Authority;
import org.eqasim.switzerland.ch.utils.pricing.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch.utils.pricing.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Zone;
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

    private Set<Zone> filterZones(Map<String, List<Zone>> zonesByStop) {
        Set<Zone> filtered = new HashSet<Zone>();

        Set<Authority> authorities = new HashSet<Authority>();

        // Get all authorities
        for (List<Zone> zones : zonesByStop.values()){
            for (Zone zone : zones){
                authorities.add(zone.getAuthority());
            }
        }

        // Get all authorities serving all visited stops
        Set<Authority> cleanedAuthorities = new HashSet<>();
        for (Authority authority : authorities) {
            boolean servesAllStops = true;

            for (List<Zone> zones : zonesByStop.values()) {
                boolean servedHere = zones.stream().anyMatch(z -> z.getAuthority().equals(authority));
                if (!servedHere) {
                    servesAllStops = false;
                    break;
                }
            }
            if (servesAllStops) {
                cleanedAuthorities.add(authority);
            }
        }

        // Only keep authority with max priority if applicable
        if (cleanedAuthorities.size() > 1) {
            int maxPriority = cleanedAuthorities.stream()
                    .mapToInt(Authority::getPriority)
                    .max()
                    .orElse(Integer.MIN_VALUE);

            cleanedAuthorities.removeIf(auth -> auth.getPriority() < maxPriority);
        }

        // If a stop is served by only one zone of a cleanedAuthority, add this zone to the filtered list
        for (List<Zone> zones: zonesByStop.values()){
            List<Zone> matchingZones = zones.stream()
                .filter(z -> cleanedAuthorities.contains(z.getAuthority()))
                .collect(Collectors.toList());
            if (matchingZones.size() == 1) {
                filtered.add(matchingZones.get(0));
            }
        }

        // Now that the identified unique zones are identified, add the multiple zones by selecting only the relevant ones
        for (List<Zone> zones: zonesByStop.values()){
            List<Zone> matchingZones = zones.stream()
                .filter(z -> cleanedAuthorities.contains(z.getAuthority()))
                .collect(Collectors.toList());
            if (matchingZones.size() > 1) {
                boolean hasIntersection = matchingZones.stream().anyMatch(filtered::contains);
                if (!hasIntersection) {
                    filtered.addAll(matchingZones);
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


    private double computeSBBDistance(Set<Zone> zones, String fromId, String toId){

        String simplifiedFromId = fromId.contains(":") ? fromId.split("[:\\.]")[0] : fromId;
        String simplifiedToId   = toId.contains(":") ? toId.split("[:\\.]")[0] : toId;

        double dist = -1.0;

        if (zones.size() == 1){
            Zone onlyZone = zones.iterator().next();
            if (onlyZone.getAuthority().getId() == "SBB"){
                return networkOfDistances.getDistance(simplifiedFromId, simplifiedToId);
            }
        }

        return dist;

    }


    @Override
    protected SwissPtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        SwissPtVariables tripDescription = new SwissPtVariables();

        Set<Zone> zones = new HashSet<>();

        for (PlanElement element : elements) {
			if (element instanceof Leg) {

                zones   = new HashSet<>();
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

                        intermediateStops = intermediateStops.subList(start, end+1);

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

                        zones = filterZones(stopsAndZones);

                        double sbbDistance = computeSBBDistance(zones, accessStopId, egressStopId);
                        double networkDistance = route.getDistance();

                        SwissPtLegVariables legVariables = new SwissPtLegVariables(zones, departureTime, arrivalTime, networkDistance, sbbDistance, accessStopId, egressStopId);
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
    
}