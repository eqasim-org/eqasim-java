package org.eqasim.core.simulation.modes.parking_aware_car.handlers;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
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

public class ParkingUsageControlerListener implements IterationEndsListener {

    private final ParkingUsageEventListener parkingUsageEventListener;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;

    public ParkingUsageControlerListener(ParkingUsageEventListener parkingUsageEventListener, OutputDirectoryHierarchy outputDirectoryHierarchy) {
        this.parkingUsageEventListener = parkingUsageEventListener;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        IdMap<Link, IdMap<ParkingType, Map<Integer, Integer>>> parkingUsage = this.parkingUsageEventListener.getParkingUsage();
        IdSet<ParkingType> parkingTypeIdSet = new IdSet<>(ParkingType.class);
        parkingUsage.values().stream().flatMap(m -> m.keySet().stream()).forEach(parkingTypeIdSet::add);

        List<Id<ParkingType>> parkingTypes = parkingTypeIdSet.stream().toList();

        String fileName = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "parking.csv");
        try {
            String[] header = new String[]{"linkId", "parkingType", "startTime", "endTime", "usage"};
            CompactCSVWriter csvWriter = new CompactCSVWriter(new BufferedWriter(new FileWriter(fileName)), ';');
            csvWriter.writeNext(new CSVLineBuilder().addAll(header));
            for(int i=0; i<this.parkingUsageEventListener.getLastRecordedTimeSlotIndex(); i++) {
                double startTime = this.parkingUsageEventListener.getSlotStartTime(i);
                double endTime = this.parkingUsageEventListener.getSlotEndTime(i);
                for(Map.Entry<Id<Link>, IdMap<ParkingType, Map<Integer, Integer>>> linkEntry : parkingUsage.entrySet()) {
                    Id<Link> linkId = linkEntry.getKey();
                    for(Map.Entry<Id<ParkingType>, Map<Integer, Integer>> parkingTypeEntry: linkEntry.getValue().entrySet()) {
                        Id<ParkingType> parkingTypeId = parkingTypeEntry.getKey();
                        Map<Integer, Integer> usageMap = parkingTypeEntry.getValue();
                        if(!usageMap.containsKey(i)) {
                            continue;
                        }
                        int users = usageMap.get(i);
                        csvWriter.writeNext(new CSVLineBuilder().addAll(linkId.toString(), parkingTypeId.toString(), String.valueOf(startTime), String.valueOf(endTime), String.valueOf(users)));
                    }
                }
            }
            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
