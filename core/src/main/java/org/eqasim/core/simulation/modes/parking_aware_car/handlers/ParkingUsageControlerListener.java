package org.eqasim.core.simulation.modes.parking_aware_car.handlers;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParkingUsageControlerListener implements IterationEndsListener {

    private final ParkingUsageEventListener parkingUsageEventListener;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

    public ParkingUsageControlerListener(ParkingUsageEventListener parkingUsageEventListener, OutputDirectoryHierarchy outputDirectoryHierarchy, NetworkWideParkingSpaceStore networkWideParkingSpaceStore) {
        this.parkingUsageEventListener = parkingUsageEventListener;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        IdMap<Link, IdMap<ParkingType, Map<Integer, Integer>>> parkingUsage = this.parkingUsageEventListener.getParkingUsage();

        List<Id<ParkingType>> parkingTypes = networkWideParkingSpaceStore.getParkingTypes().keySet().stream().toList();

        String fileName = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "parking_demand.csv");
        try {
            CompactCSVWriter csvWriter = new CompactCSVWriter(new BufferedWriter(new FileWriter(fileName)), ';');
            CSVLineBuilder csvLineBuilder = new CSVLineBuilder();
            csvLineBuilder.addAll("linkId", "startTime", "endTime");
            csvLineBuilder.addAll(parkingTypes.stream().map(Id::toString).toList());
            csvWriter.writeNext(csvLineBuilder.build());
            for(int i=0; i<this.parkingUsageEventListener.getLastRecordedTimeSlotIndex(); i++) {
                double startTime = this.parkingUsageEventListener.getSlotStartTime(i);
                double endTime = this.parkingUsageEventListener.getSlotEndTime(i);
                for(Map.Entry<Id<Link>, IdMap<ParkingType, Map<Integer, Integer>>> linkEntry : parkingUsage.entrySet()) {
                    Id<Link> linkId = linkEntry.getKey();
                    csvLineBuilder = new CSVLineBuilder();
                    csvLineBuilder.addAll(linkId.toString(), String.valueOf(startTime), String.valueOf(endTime));
                    for(Id<ParkingType> parkingType : parkingTypes) {
                        Map<Integer, Integer> usagesMap = linkEntry.getValue().get(parkingType);
                        int finalI = i;
                        csvLineBuilder.add(Optional.ofNullable(usagesMap).map(map -> map.get(finalI)).map(String::valueOf).orElse(null));
                    }
                    csvWriter.writeNext(csvLineBuilder.build());
                }
            }
            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
