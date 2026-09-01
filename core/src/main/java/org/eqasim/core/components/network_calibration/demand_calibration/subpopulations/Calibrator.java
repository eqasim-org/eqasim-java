package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.background.BackgroundPlanRelocator;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.background.BackgroundTrafficCalibrator;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.config.SubpopulationsCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder.CrossBorderCloneFactory;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder.CrossBorderPopulationEditor;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder.CrossBorderStationDetector;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder.CrossBorderState;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder.CrossBorderVolumeCalibrator;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.RouteImpact;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.TrafficScoringTracker;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouter;

/** Schedules background location calibration and cross-border volume calibration. */
public final class Calibrator implements IterationEndsListener {
    private static final Logger logger = LogManager.getLogger(Calibrator.class);

    private final boolean active;
    private final SubpopulationsCalibrationConfigGroup config;
    private final TrafficScoringTracker tracker;
    private final BackgroundTrafficCalibrator backgroundTraffic;
    private final CrossBorderVolumeCalibrator crossBorderVolume;

    public Calibrator(Scenario scenario,
                      TripListConverter tripListConverter,
                      Provider<CountsProcessor> countsProcessorProvider,
                      Provider<FlowProcessor> flowProcessorProvider,
                      EqasimConfigGroup eqasimConfig,
                      NetworkCalibrationConfigGroup calibrationConfig,
                      SubpopulationsCalibrationConfigGroup config,
                      Provider<TripRouter> tripRouterProvider) {
        this.config = config;
        this.active = calibrationConfig.isActivated()
                && calibrationConfig.isSubpopulationsCalibrationActivated()
                && calibrationConfig.isSubpopulationsActivated();
        if (!active) {
            tracker = null;
            backgroundTraffic = null;
            crossBorderVolume = null;
            return;
        }

        CountsProcessor counts = countsProcessorProvider.get();
        FlowProcessor flows = flowProcessorProvider.get();
        RouteImpact.Extractor extractor = new RouteImpact.Extractor(tripListConverter, counts);
        tracker = new TrafficScoringTracker(scenario.getPopulation(), counts, flows, extractor,
                                            eqasimConfig.getSampleSize(), config.getFlowUnderEstimationThreshold(),
                                            config.getFlowOverEstimationThreshold());

        int numberOfThreads = scenario.getConfig().global().getNumberOfThreads();
        if (config.isCrossBorderCalibrationEnabled()) {
            CrossBorderState crossBorderState = new CrossBorderState();
            CrossBorderPopulationEditor editor = new CrossBorderPopulationEditor(
                    scenario.getPopulation(), crossBorderState);
            CrossBorderCloneFactory cloneFactory = new CrossBorderCloneFactory(
                    scenario, tripListConverter, tripRouterProvider,
                    MatsimRandom.getLocalInstance(), config.getRelocationRadius(),
                    config.getHomeRelocationRadius(), config.getMaximumTimeShift());
            CrossBorderStationDetector stationDetector = new CrossBorderStationDetector(
                    scenario.getNetwork(), config.getCrossBorderShareThreshold());
            crossBorderVolume = new CrossBorderVolumeCalibrator(
                    scenario.getPopulation(), crossBorderState, editor, cloneFactory,
                    stationDetector, config.getCrossBorderUpdateFraction(), numberOfThreads);
        } else {
            crossBorderVolume = null;
        }

        BackgroundPlanRelocator relocator = new BackgroundPlanRelocator(
                scenario, tripRouterProvider,
                config.getBackgroundRelocationRadiusFactor(),
                config.getBackgroundMinimumRadius(),
                config.getBackgroundMaximumRadius(),
                config.getDestinationSelectionProbability());

        long randomSeed = scenario.getConfig().global().getRandomSeed();
        backgroundTraffic = new BackgroundTrafficCalibrator(
                scenario.getPopulation(), relocator, numberOfThreads, randomSeed,
                config.getBackgroundRelocationTryFraction());
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        int iteration = event.getIteration();
        boolean backgroundUpdate = doRegularUpdate(iteration);
        boolean crossBorderVolumeUpdate = config.isCrossBorderCalibrationEnabled()
                && config.getCrossBorderCloningIterations().contains(iteration);
        if (!active || (!crossBorderVolumeUpdate && !backgroundUpdate)) return;

        tracker.refresh();
        BackgroundTrafficCalibrator.Result relocation = backgroundUpdate
                ? backgroundTraffic.update(iteration, tracker) : null;
        CrossBorderVolumeCalibrator.Result volume = crossBorderVolumeUpdate
                ? crossBorderVolume.update(tracker) : null;

        logger.info("Subpopulation calibration [iter {}]: background-location-update={}, cross-border-volume-update={}",
                iteration, backgroundUpdate, crossBorderVolumeUpdate);
        logger.info("\t - freight-relocated={}, cross-border-relocated={}, cross-border-cloned={}, "
                        + "cross-border-removed={}, cross-border-restored={}",
                relocation == null ? 0 : relocation.freightRelocated(),
                relocation == null ? 0 : relocation.crossBorderRelocated(),
                volume == null ? 0 : volume.cloned(),
                volume == null ? 0 : volume.removed(),
                volume == null ? 0 : volume.restored());

    }

    private boolean doRegularUpdate(int iteration) {
        if (iteration < config.getWarmupIterations()) return false;
        if (iteration < config.getEarlyIterationLimit()) {
            return (iteration - config.getWarmupIterations()) % config.getEarlyUpdateInterval() == 0;
        }
        return (iteration - config.getEarlyIterationLimit()) % config.getUpdateInterval() == 0;
    }

    /** Exposes the authoritative live scoring API to other calibration components. */
    public TrafficScoringTracker scoring() {
        if (!active) throw new IllegalStateException("Subpopulation calibration is not active");
        return tracker;
    }
}
