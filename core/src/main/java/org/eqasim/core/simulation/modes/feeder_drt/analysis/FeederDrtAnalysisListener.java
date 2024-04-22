package org.eqasim.core.simulation.modes.feeder_drt.analysis;

import com.google.inject.Inject;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.modes.drt.analysis.utils.VehicleRegistry;
import org.eqasim.core.simulation.modes.feeder_drt.analysis.passengers.FeederTripSequenceListener;
import org.eqasim.core.simulation.modes.feeder_drt.analysis.passengers.FeederTripSequenceWriter;
import org.eqasim.core.simulation.modes.feeder_drt.config.MultiModeFeederDrtConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FeederDrtAnalysisListener implements IterationStartsListener, IterationEndsListener, ShutdownListener {

    private static final String FEEDER_TRIPS_FILE_NAME = "eqasim_feeder_drt_trips.csv";
    private final OutputDirectoryHierarchy outputDirectory;
    private final int analysisInterval;
    private boolean isActive = false;
    private final VehicleRegistry vehicleRegistry;
    private final FeederTripSequenceListener feederTripSequenceListener;

    @Inject
    public FeederDrtAnalysisListener(EqasimConfigGroup config, MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup,
                                     OutputDirectoryHierarchy outputDirectory, Network network) {
        this.outputDirectory = outputDirectory;
        this.analysisInterval = config.getAnalysisInterval();
        this.vehicleRegistry = new VehicleRegistry();

        feederTripSequenceListener = new FeederTripSequenceListener(multiModeFeederDrtConfigGroup.getModeConfigs(), vehicleRegistry, network);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        try {
            if(isActive) {
                event.getServices().getEvents().removeHandler(feederTripSequenceListener);
                event.getServices().getEvents().removeHandler(vehicleRegistry);

                String feederTripsPath = outputDirectory.getIterationFilename(event.getIteration(), FEEDER_TRIPS_FILE_NAME);
                new FeederTripSequenceWriter(feederTripSequenceListener).writeTripItems(new File(feederTripsPath));
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (analysisInterval > 0) {
            isActive = event.getIteration() % analysisInterval == 0 || event.isLastIteration();
        }
        if (isActive) {
            event.getServices().getEvents().addHandler(feederTripSequenceListener);
            event.getServices().getEvents().addHandler(vehicleRegistry);
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            File iterationPath = new File(outputDirectory.getIterationFilename(event.getIteration(), FEEDER_TRIPS_FILE_NAME));
            File outputPath = new File(outputDirectory.getOutputFilename(FEEDER_TRIPS_FILE_NAME));
            Files.copy(iterationPath.toPath(), outputPath.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
