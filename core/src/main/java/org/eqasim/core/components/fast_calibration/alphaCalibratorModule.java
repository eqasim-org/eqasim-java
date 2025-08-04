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

import java.util.Map;

public class alphaCalibratorModule extends AbstractEqasimExtension {
    private static final Logger logger = LogManager.getLogger(alphaCalibratorModule.class);

    @Override
    protected void installEqasimExtension() {
        alphaCalibratorConfig calConfig = alphaCalibratorConfig.getOrCreate(getConfig());
        if (calConfig.isActivate()){
            logger.info("Activate calibration module (calibration of the alpha parameters of the utilities)");
            addControlerListenerBinding().to(alphaCalibrator.class).asEagerSingleton();
        }
    }

    @Provides
    @Singleton
    public alphaCalibrator provideAlphaCalibrator(Scenario scenario,
                                                  OutputDirectoryHierarchy outputHierarchy,
                                                  ModeParameters modeParameters,
                                                  TripListConverter tripListConverter,
                                                  alphaCalibratorConfig calConfig) {
        double beta = calConfig.getBeta();
        Map<String, Double> targetModeShares = Map.of(
                "car", calConfig.getCarModeShare(),
                "pt", calConfig.getPtModeShare(),
                "walk", calConfig.getWalkModeShare(),
                "bike", calConfig.getBikeModeShare(),
                "car_passenger", calConfig.getCarPassengerModeShare()
        );
        return new alphaCalibrator(scenario,outputHierarchy,modeParameters,tripListConverter,targetModeShares,beta);
    }
}
