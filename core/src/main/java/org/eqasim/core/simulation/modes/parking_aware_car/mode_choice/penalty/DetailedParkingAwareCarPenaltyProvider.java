package org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.penalty;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageEventListener;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DetailedParkingAwareCarPenaltyProvider implements ParkingAwareCarPenaltyProvider {

    private final IdMap<Link, IdMap<ParkingType, Map<Integer, Double>>> penalties;
    private final ParkingUsageEventListener parkingUsageEventListener;
    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;
    private final double alpha;

    public DetailedParkingAwareCarPenaltyProvider(ParkingUsageEventListener parkingUsageEventListener, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, double alpha) {
        this.penalties = new IdMap<>(Link.class);
        this.parkingUsageEventListener = parkingUsageEventListener;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.alpha = alpha;
    }

    private double getPenalty(Id<Link> linkId, double parkingStartTime, double parkingEndTime, Id<ParkingType> parkingTypeId) {
        double penalty = 0;
        for(int i=this.parkingUsageEventListener.getTimeSlotIndex(parkingStartTime); i<=this.parkingUsageEventListener.getTimeSlotIndex(parkingEndTime); i++) {
            int finalI = i;
            penalty += Optional.ofNullable(penalties.get(linkId)).map(map -> map.get(parkingTypeId)).map(map -> map.get(finalI)).orElse(0.0);
        }
        return penalty;
    }

    @Override
    public double getPenalty(Id<Person> person, Id<Link> link, double parkingStartTime, double parkingEndTime, Id<ParkingType> parkingType) {
        return this.getPenalty(link, parkingStartTime, parkingEndTime, parkingType);
    }

    @Override
    public void update(int iteration) {
        IdMap<Link, IdMap<ParkingType, Map<Integer, Double>>> usage = this.parkingUsageEventListener.getParkingUsage();
        for(Map.Entry<Id<Link>, IdMap<ParkingType, Map<Integer, Double>>> linkEntry : usage.entrySet()) {
            for(Map.Entry<Id<ParkingType>, Map<Integer, Double>> parkingTypeMapEntry: linkEntry.getValue().entrySet()) {
                if(parkingTypeMapEntry.getKey().equals(this.networkWideParkingSpaceStore.getFallBackParkingType().id())) {
                    continue;
                }
                for(Map.Entry<Integer, Double> timeSlotEntry: parkingTypeMapEntry.getValue().entrySet()) {
                    ParkingSpace parkingSpace = this.networkWideParkingSpaceStore.getLinkParkingSpaces(linkEntry.getKey()).get(parkingTypeMapEntry.getKey());
                    double unmetDemand = timeSlotEntry.getValue() - parkingSpace.capacity();
                    double currentPenalty = this.getPenalty(linkEntry.getKey(), this.parkingUsageEventListener.getSlotStartTime(timeSlotEntry.getKey()), this.parkingUsageEventListener.getSlotEndTime(timeSlotEntry.getKey()), parkingTypeMapEntry.getKey());
                    currentPenalty = Math.max(0, currentPenalty + unmetDemand * this.alpha);

                    this.penalties.computeIfAbsent(linkEntry.getKey(), k -> new IdMap<>(ParkingType.class))
                            .computeIfAbsent(parkingTypeMapEntry.getKey(), k -> new HashMap<>())
                            .put(timeSlotEntry.getKey(), currentPenalty);
                }
            }
        }
    }
}
