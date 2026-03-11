package org.eqasim.switzerland.ch.utils.pt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class TransitTripInfo {
    public final String lineId;
    public final String lineName;
    public final String routeId;
    public final String departureId;
    public final String routeDirection;
    public final String routeMainDirection;

    public TransitTripInfo(String lineId, String lineName, String routeId, String departureId, String routeDirection, String routeMainDirection) {
        this.lineId = lineId;
        this.routeId = routeId;
        this.departureId = departureId;
        this.lineName = lineName;
        this.routeDirection = routeDirection;
        this.routeMainDirection = routeMainDirection;
    }

    @SuppressWarnings("null")
    public static List<String> findLineRouteInfo(TransitLine line, TransitRoute route ){
        // Identify direction (H vs R)
        String routeId      = route.getId().toString();
        char routeDirection = routeId.charAt(routeId.length() - 1);
        String routeDirectionStops = "";

        // Get all routes for the line
        Map<Id<TransitRoute>, TransitRoute> lineRoutes = line.getRoutes();
        
        // Dictionary storing the frequency of each route
        Map<String, Integer> directionFrequency = new HashMap<>();

        for (TransitRoute currentRoute : lineRoutes.values()){
            String currentRouteId = currentRoute.getId().toString();

            if (currentRouteId.charAt(currentRouteId.length()-1) == routeDirection){
                List<TransitRouteStop> stops = new ArrayList<>(currentRoute.getStops());
                if (stops.isEmpty()) continue;

                String firstStop = stops.get(0).getStopFacility().getName();
                String lastStop  = stops.get(stops.size()-1).getStopFacility().getName();

                String key = firstStop + "->" + lastStop;
                directionFrequency.merge(key, 1, Integer::sum);

                if (currentRouteId.equals(routeId)){
                    routeDirectionStops = key;
                }
            }
        }

        // Most frequent direction
        String mostFrequentDirection = directionFrequency.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("Unknown");

        // Returns first the current line direction, then the most frequent direction
        return Arrays.asList(routeDirectionStops, mostFrequentDirection);
    }
}