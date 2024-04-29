package org.eqasim.core.simulation.analysis;

import com.google.inject.Inject;
import org.eqasim.core.tools.ExtractPlanUtilities;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import java.io.IOException;


public class UtilitiesWriterHandler implements ShutdownListener {

    private final Population population;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;

    @Inject
    public UtilitiesWriterHandler(Population population, OutputDirectoryHierarchy outputDirectoryHierarchy) {
        this.population = population;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        String filePath = this.outputDirectoryHierarchy.getOutputFilename("dmc_utilities.csv");
        try {
            ExtractPlanUtilities.writePlanUtilities(population, filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
