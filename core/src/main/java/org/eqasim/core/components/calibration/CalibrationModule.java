package org.eqasim.core.components.calibration;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.calibration.optimizer.OptimizerHandler;
import org.eqasim.core.components.calibration.optimizer.Optimizer;
import org.eqasim.core.components.calibration.writer.VariablesWriterHandler;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class CalibrationModule extends AbstractEqasimExtension {
    private static final Logger logger = LogManager.getLogger(CalibrationModule.class);

    @Override
    protected void installEqasimExtension() {
        CalibrationConfigGroup calibrationConfig = CalibrationConfigGroup.getOrCreate(getConfig());
        if (calibrationConfig.isActivated()) {
            logger.info("Activate calibration module");
            addControlerListenerBinding().to(VariablesWriterHandler.class).asEagerSingleton();
            addControlerListenerBinding().to(OptimizerHandler.class).asEagerSingleton();
        } else {
            logger.info("Calibration module is not activated.");
        }
    }

    @Provides
    @Singleton
    public VariablesWriterHandler provideVariablesWriterHandler(DiscreteModeChoiceConfigGroup discreteModeChoiceConfigGroup, OutputDirectoryHierarchy outputDirectoryHierarchy,
                                                                EqasimConfigGroup eqasimConfigGroup, CalibrationConfigGroup calibrationConfig){
        if (calibrationConfig.isActivated()){
            discreteModeChoiceConfigGroup.getMultinomialLogitSelectorConfig().setWriteDetailedUtilities(true);
        }
        return new VariablesWriterHandler(discreteModeChoiceConfigGroup, outputDirectoryHierarchy, eqasimConfigGroup, calibrationConfig);
    }

    @Provides
    @Singleton
    public OptimizerHandler provideCalibrationHandler(CalibrationConfigGroup calibrationConfig, OutputDirectoryHierarchy outputDirectoryHierarchy,
                                                      EqasimConfigGroup eqasimConfigGroup, ModeParameters parameters, Optimizer optimizer) {
        return new OptimizerHandler(calibrationConfig, outputDirectoryHierarchy, eqasimConfigGroup, parameters, optimizer);
    }

    @Provides
    @Singleton
    public Optimizer provideOptimizer(CalibrationConfigGroup calibrationConfig) {
        return new Optimizer(calibrationConfig);
    }

}
