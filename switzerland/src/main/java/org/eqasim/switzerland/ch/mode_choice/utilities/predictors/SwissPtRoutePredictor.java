package org.eqasim.switzerland.ch.mode_choice.utilities.predictors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPtVariables;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Authority;
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

    @Inject
	public SwissPtRoutePredictor(TransitSchedule schedule, ZonalRegistry zonalRegistry) {
		this.schedule = schedule;
        this.zonalRegistry = zonalRegistry;
	}

    private Set<Zone> filterZones(Set<Zone> zones) {
        // Group zones by authority so that comparisons only happen within the same network
        Map<Authority, List<Zone>> groupedByAuthority = zones.stream()
            .collect(Collectors.groupingBy(Zone::getAuthority));

        Set<Zone> result = new HashSet<>();

        for (Map.Entry<Authority, List<Zone>> entry : groupedByAuthority.entrySet()) {
            List<Zone> authorityZones = entry.getValue();

            // Collect all individual zone IDs for quick lookup
            Set<String> individualIds = authorityZones.stream()
                .filter(z -> z.getZoneId().split(", ").length == 1)
                .flatMap(z -> Arrays.stream(z.getZoneId().split(", ")))
                .map(String::trim)
                .collect(Collectors.toSet());

            for (Zone z : authorityZones) {
                String[] parts = z.getZoneId().split(", ");
                if (parts.length == 1) {
                    result.add(z);
                } else {
                    boolean hasOverlap = Arrays.stream(parts)
                        .map(String::trim)
                        .anyMatch(individualIds::contains);
                    if (!hasOverlap) {
                        result.add(z);
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected SwissPtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

        Set<Zone> zones = new HashSet<>();

        for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

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

                        Stream.concat(
                            Stream.concat(
                                zonalRegistry.getZones(ptRoute.getAccessStopId().toString()).stream(), 
                                zonalRegistry.getZones(ptRoute.getEgressStopId().toString()).stream()),
                            stopsVisited.stream().flatMap(stopId -> zonalRegistry.getZones(stopId).stream())
                        ).forEach(zones::add);

						System.out.println("PT leg found:");
						System.out.println("  Access stop: " + ptRoute.getAccessStopId());
						System.out.println("  Egress stop: " + ptRoute.getEgressStopId());
                        System.out.println("  Visited stops: ");

                        for (String stopId : stopsVisited){
                           System.out.println("    " + stopId);
                        }

					} 
                    
                    else {
						System.out.println("PT leg has no TransitPassengerRoute (route is " +
							(route == null ? "null" : route.getClass().getSimpleName()) + ")");
					}
				}
			}
		}

        zones = filterZones(zones);

        System.out.println("  Visited zones: ");

        for (Zone zone : zones){
            System.out.println("    " + zone.toString());
        }

        return new SwissPtVariables(zones);
    }
    
}