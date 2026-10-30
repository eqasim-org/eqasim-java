package org.eqasim.switzerland.ch_cmdp.utils.routing;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

/** PT mode-choice columns, with the same names and units as BatchPublicTransportRouter.
 * Fields stay blank for non-PT and unreachable requests; itinerary export is independent.
 */
// Avoid writing empty PT columns into every road trip's internal MARouter spool.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicTransportRoutingMetrics extends TripsRouter.RoutedTrip {
    @JsonProperty("access_travel_time_min") public Double accessTravelTime_min;
    @JsonProperty("access_distance_km") public Double accessDistance_km;
    @JsonProperty("egress_travel_time_min") public Double egressTravelTime_min;
    @JsonProperty("egress_distance_km") public Double egressDistance_km;
    @JsonProperty("transfer_travel_time_min") public Double transferTravelTime_min;
    @JsonProperty("transfer_distance_km") public Double transferDistance_km;
    @JsonProperty("total_walk_travel_time_min") public Double totalWalkTravelTime_min;
    @JsonProperty("total_walk_distance_km") public Double totalWalkDistance_km;
    @JsonProperty("in_vehicle_time_rail_min") public Double inVehicleTimeRail_min;
    @JsonProperty("in_vehicle_time_subway_min") public Double inVehicleTimeSubway_min;
    @JsonProperty("in_vehicle_time_bus_min") public Double inVehicleTimeBus_min;
    @JsonProperty("in_vehicle_time_tram_min") public Double inVehicleTimeTram_min;
    @JsonProperty("in_vehicle_time_other_min") public Double inVehicleTimeOther_min;
    @JsonProperty("in_vehicle_time_total_min") public Double inVehicleTimeTotal_min;
    @JsonProperty("in_vehicle_distance_rail_km") public Double inVehicleDistanceRail_km;
    @JsonProperty("in_vehicle_distance_subway_km") public Double inVehicleDistanceSubway_km;
    @JsonProperty("in_vehicle_distance_bus_km") public Double inVehicleDistanceBus_km;
    @JsonProperty("in_vehicle_distance_tram_km") public Double inVehicleDistanceTram_km;
    @JsonProperty("in_vehicle_distance_other_km") public Double inVehicleDistanceOther_km;
    @JsonProperty("in_vehicle_distance_total_km") public Double inVehicleDistanceTotal_km;
    @JsonProperty("transfers") public Integer numberOfTransfers;
    @JsonProperty("initial_waiting_time_min") public Double initialWaitingTime_min;
    @JsonProperty("transfer_waiting_time_min") public Double transferWaitingTime_min;
    @JsonProperty("headway_min") public Double headway_min;
    @JsonProperty("is_only_walk") public Integer isOnlyWalk;

    void populatePtMetrics(List<? extends PlanElement> elements, TransitSchedule schedule) {
        accessTravelTime_min = 0.0;
        accessDistance_km = 0.0;
        egressTravelTime_min = 0.0;
        egressDistance_km = 0.0;
        transferTravelTime_min = 0.0;
        transferDistance_km = 0.0;
        totalWalkTravelTime_min = 0.0;
        totalWalkDistance_km = 0.0;
        inVehicleTimeRail_min = 0.0;
        inVehicleTimeSubway_min = 0.0;
        inVehicleTimeBus_min = 0.0;
        inVehicleTimeTram_min = 0.0;
        inVehicleTimeOther_min = 0.0;
        inVehicleTimeTotal_min = 0.0;
        inVehicleDistanceRail_km = 0.0;
        inVehicleDistanceSubway_km = 0.0;
        inVehicleDistanceBus_km = 0.0;
        inVehicleDistanceTram_km = 0.0;
        inVehicleDistanceOther_km = 0.0;
        inVehicleDistanceTotal_km = 0.0;
        numberOfTransfers = 0;
        initialWaitingTime_min = 0.0;
        transferWaitingTime_min = 0.0;
        headway_min = Double.NaN;
        isOnlyWalk = 1;

        // Inspect actual leg positions: stage activities can occur between legs.
        Leg first = null, last = null;
        for (var element : elements) {
            if (element instanceof Leg leg) {
                if (first == null) first = leg;
                last = leg;
            }
        }
        int boardings = 0;
        for (var element : elements) {
            if (!(element instanceof Leg leg)) continue;
            double minutes = leg.getTravelTime().seconds() / 60.0;
            double km = leg.getRoute().getDistance() * 1e-3;
            if (leg.getRoute() instanceof TransitPassengerRoute route) {
                double waiting = route.getBoardingTime().seconds() - leg.getDepartureTime().seconds();
                if (boardings++ == 0) initialWaitingTime_min += waiting / 60.0;
                else transferWaitingTime_min += waiting / 60.0;
                double inVehicle = (route.getTravelTime().seconds() - waiting) / 60.0;
                String transitMode = schedule.getTransitLines().get(route.getLineId())
                        .getRoutes().get(route.getRouteId()).getTransportMode();
                switch (transitMode) {
                    case "rail" -> { inVehicleTimeRail_min += inVehicle; inVehicleDistanceRail_km += km; }
                    case "subway" -> { inVehicleTimeSubway_min += inVehicle; inVehicleDistanceSubway_km += km; }
                    case "bus" -> { inVehicleTimeBus_min += inVehicle; inVehicleDistanceBus_km += km; }
                    case "tram" -> { inVehicleTimeTram_min += inVehicle; inVehicleDistanceTram_km += km; }
                    default -> { inVehicleTimeOther_min += inVehicle; inVehicleDistanceOther_km += km; }
                }
            } else if (leg.getMode().contains("walk")) {
                if (leg == first) { accessTravelTime_min += minutes; accessDistance_km += km; }
                else if (leg == last) { egressTravelTime_min += minutes; egressDistance_km += km; }
                else { transferTravelTime_min += minutes; transferDistance_km += km; }
            } else {
                throw new IllegalStateException("Unsupported PT access/egress mode: " + leg.getMode());
            }
        }
        numberOfTransfers = Math.max(0, boardings - 1);
        isOnlyWalk = boardings == 0 ? 1 : 0;
        inVehicleTimeTotal_min = inVehicleTimeRail_min + inVehicleTimeSubway_min + inVehicleTimeBus_min
                + inVehicleTimeTram_min + inVehicleTimeOther_min;
        inVehicleDistanceTotal_km = inVehicleDistanceRail_km + inVehicleDistanceSubway_km + inVehicleDistanceBus_km
                + inVehicleDistanceTram_km + inVehicleDistanceOther_km;
        totalWalkTravelTime_min = accessTravelTime_min + egressTravelTime_min + transferTravelTime_min;
        totalWalkDistance_km = accessDistance_km + egressDistance_km + transferDistance_km;
        accessDistance = accessDistance_km * 1000;
        egressDistance = egressDistance_km * 1000;
    }
}
