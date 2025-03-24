package org.eqasim.core.simulation.modes.parking_aware_car.handlers;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.utils.ExportNetworkParkingsToCsv;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import java.io.IOException;

public class ParkingsWriterControlerListener implements ShutdownListener {

    private final Network network;

    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

    private final OutputDirectoryHierarchy outputDirectoryHierarchy;

    public ParkingsWriterControlerListener(Network network, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, OutputDirectoryHierarchy outputDirectoryHierarchy) {
        this.network = network;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            ExportNetworkParkingsToCsv.export(network, networkWideParkingSpaceStore, outputDirectoryHierarchy.getOutputFilename("parkings_db.csv"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
