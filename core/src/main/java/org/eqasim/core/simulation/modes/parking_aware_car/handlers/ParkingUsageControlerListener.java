package org.eqasim.core.simulation.modes.parking_aware_car.handlers;

import org.apache.commons.io.FileUtils;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.utils.HierarchicalComparator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.csv.CSVLineBuilder;
import org.matsim.contrib.common.csv.CompactCSVWriter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class ParkingUsageControlerListener implements IterationEndsListener, ShutdownListener {

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
        IdMap<Link, IdMap<ParkingType, Map<Integer, Double>>> parkingUsage = this.parkingUsageEventListener.getParkingUsage();

        List<Id<ParkingType>> parkingTypes = new ArrayList<>(networkWideParkingSpaceStore.getParkingTypes().keySet().stream().toList());
        parkingTypes.add(networkWideParkingSpaceStore.getFallBackParkingType().id());

        String fileName = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "parking_demand_aggregated.csv");
        try {
            CompactCSVWriter csvWriter = new CompactCSVWriter(new BufferedWriter(new FileWriter(fileName)), ';');
            CSVLineBuilder csvLineBuilder = new CSVLineBuilder();
            csvLineBuilder.addAll("linkId", "startTime", "endTime");
            csvLineBuilder.addAll(parkingTypes.stream().map(Id::toString).toList());
            csvWriter.writeNext(csvLineBuilder.build());
            for(int i=0; i<this.parkingUsageEventListener.getLastRecordedTimeSlotIndex(); i++) {
                double startTime = this.parkingUsageEventListener.getSlotStartTime(i);
                double endTime = this.parkingUsageEventListener.getSlotEndTime(i);
                for(Map.Entry<Id<Link>, IdMap<ParkingType, Map<Integer, Double>>> linkEntry : parkingUsage.entrySet()) {
                    Id<Link> linkId = linkEntry.getKey();
                    csvLineBuilder = new CSVLineBuilder();
                    csvLineBuilder.addAll(linkId.toString(), String.valueOf(startTime), String.valueOf(endTime));
                    boolean oneItemAdded = false;
                    for(Id<ParkingType> parkingType : parkingTypes) {
                        Map<Integer, Double> usagesMap = linkEntry.getValue().get(parkingType);
                        int finalI = i;
                        Double demand = Optional.ofNullable(usagesMap).map(map -> map.get(finalI)).orElse(null);
                        if (demand != null) {
                            csvLineBuilder.add(String.valueOf(demand));
                            oneItemAdded = true;
                        } else {
                            csvLineBuilder.add(null);
                        }
                    }
                    if(oneItemAdded) {
                        csvWriter.writeNext(csvLineBuilder.build());
                    }
                }
            }
            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        fileName = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "parking_demand.csv");

        try {
            CompactCSVWriter csvWriter = new CompactCSVWriter(new BufferedWriter(new FileWriter(fileName)), ';');
            CSVLineBuilder csvLineBuilder = new CSVLineBuilder();
            csvLineBuilder.addAll("personId", "linkId", "parkingType", "startTime", "endTime", "occupancy", "parkingOccupancyOnArrival", "parkingOccupancyOnDeparture");

            csvWriter.writeNext(csvLineBuilder.build());
            this.parkingUsageEventListener.getParkingUsagesPerPerson().values().stream()
                    .flatMap(Collection::stream)
                    .sorted(new HierarchicalComparator<>(
                            Comparator.comparing(ParkingUsageEventListener.ParkingUsageRecord::getEnterTime),
                            Comparator.comparing(o -> o.getParkingSpace().linkId()),
                            Comparator.comparing(o -> o.getParkingSpace().parkingType().id()),
                            Comparator.comparing(o -> o.getExitTime())
                    ))
                    .forEach(parkingUsageRecord -> {
                        CSVLineBuilder builder = new CSVLineBuilder();
                        builder.add(parkingUsageRecord.getPersonId().toString());
                        builder.add(parkingUsageRecord.getParkingSpace().linkId().toString());
                        builder.add(parkingUsageRecord.getParkingSpace().parkingType().id().toString());
                        builder.add(String.valueOf(parkingUsageRecord.getEnterTime()));
                        builder.add(String.valueOf(parkingUsageRecord.getExitTime()));
                        builder.add(String.valueOf(parkingUsageRecord.getOccupancy()));
                        builder.add(String.valueOf(parkingUsageRecord.getParkingOccupancyOnArrival()));
                        builder.add(String.valueOf(parkingUsageRecord.getParkingOccupancyOnDeparture()));
                        csvWriter.writeNext(builder.build());
                    });
            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            FileUtils.copyFile(new File(outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "parking_demand.csv")), new File(outputDirectoryHierarchy.getOutputFilename("parking_demand.csv")));
            FileUtils.copyFile(new File(outputDirectoryHierarchy.getIterationFilename(event.getIteration(), "parking_demand_aggregated.csv")), new File(outputDirectoryHierarchy.getOutputFilename("parking_demand_aggregated.csv")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
