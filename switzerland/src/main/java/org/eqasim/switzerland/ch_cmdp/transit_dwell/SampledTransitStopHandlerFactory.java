package org.eqasim.switzerland.ch_cmdp.transit_dwell;

import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandler;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.vehicles.Vehicle;

/** A separate, stateful native stop handler is created for each transit vehicle. */
public final class SampledTransitStopHandlerFactory implements TransitStopHandlerFactory {
    private final double sampleSize;
    private final TransitStopHandlerFactory delegate;

    public SampledTransitStopHandlerFactory(double sampleSize) {
        SampledTransitStopHandler.validateSampleSize(sampleSize);
        this.sampleSize = sampleSize;
        this.delegate = new ComplexTransitStopHandlerFactory();

    }

    @Override
    public TransitStopHandler createTransitStopHandler(Vehicle vehicle) {
        return new SampledTransitStopHandler(delegate, vehicle, sampleSize);
    }
}
