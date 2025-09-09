package org.eqasim.core.components.fast_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.util.List;
import java.util.Map;

public class FastCalibrationModule extends AbstractEqasimExtension {
    private static final Logger logger = LogManager.getLogger(FastCalibrationModule.class);

    @Override
    protected void installEqasimExtension() {
        AlphaCalibratorConfig calConfig = AlphaCalibratorConfig.getOrCreate(getConfig());
        addControlerListenerBinding().to(FastCalibration.class).asEagerSingleton();
        bind(FastCalibration.class).to(AlphaCalibrator.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public AlphaCalibrator provideAlphaCalibrator(Scenario scenario,
                                                  OutputDirectoryHierarchy outputHierarchy,
                                                  ModeParameters modeParameters,
                                                  TripListConverter tripListConverter,
                                                  AlphaCalibratorConfig calConfig) {
        double beta = calConfig.getBeta();
        Map<String, Double> targetModeShares = Map.of(
                "car", calConfig.getCarModeShare(),
                "pt", calConfig.getPtModeShare(),
                "walk", calConfig.getWalkModeShare(),
                "bike", calConfig.getBikeModeShare(),
                "car_passenger", calConfig.getCarPassengerModeShare()
        );
        boolean isActivated = calConfig.isActivate();
        List<String> modesToCalibrate = calConfig.getCalibratedModes();
        return new AlphaCalibrator(scenario,outputHierarchy,modeParameters,tripListConverter,targetModeShares, modesToCalibrate, beta, isActivated);
    }
}
