package org.eqasim.core.components.network_calibration.demand_calibration.agent_ascs;

import com.google.inject.Provider;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.Tools;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.util.List;

public class CarASCsAdapter implements IterationEndsListener {

    private final OutputDirectoryHierarchy outputHierarchy;
    private final Population population;
    private final PopulationGroups populationGroups;
    private final TripListConverter tripListConverter;
    private final ODErrors odErrors;
    private final boolean calibrationEnabled;
    private final double initialLearningRate;
    private final double minimumLearningRate;
    private final double learningRateDecay;
    private final double minAscStep;
    private final double maxAscStep;
    private final double ascDeadband;
    private final double maxAbsoluteAsc;
    private final int warmupIterations;
    private final int updateInterval;
    private final AgentAscsCalibrationConfigGroup config;
    private int numUpdates;

    public CarASCsAdapter(Scenario scenario, Provider<PopulationGroups> populationGroupsProvider, TripListConverter tripListConverter,
                          OutputDirectoryHierarchy outputHierarchy, Provider<ODErrors> odErrorsProvider, double dmcWeight,
                          NetworkCalibrationConfigGroup calConfig, AgentAscsCalibrationConfigGroup config) {
        this.population = scenario.getPopulation();
        this.tripListConverter = tripListConverter;
        this.calibrationEnabled = calConfig.isActivated() && calConfig.isCalibrationEnabled()
                && calConfig.isAgentAscsCalibrationActivated();
        this.outputHierarchy = outputHierarchy;
        this.config = config;
        this.initialLearningRate = config.getLearningRate();
        this.minimumLearningRate = config.getMinimumLearningRate();
        this.learningRateDecay = config.getLearningRateDecay();
        this.minAscStep = config.getMinAscStep();
        this.maxAscStep = config.getMaxAscStep();
        this.ascDeadband = config.getAscDeadband();
        this.maxAbsoluteAsc = config.getMaxAbsoluteAsc();
        this.warmupIterations = config.getWarmupIterations();
        this.updateInterval = config.getUpdateInterval() > 0
                ? config.getUpdateInterval()
                : Math.max(1, (int) Math.floor(1.0 / dmcWeight));
        this.numUpdates = 0;

        this.odErrors = calibrationEnabled ? odErrorsProvider.get():null;;
        this.populationGroups = calibrationEnabled ? populationGroupsProvider.get():null;

        if (calibrationEnabled) {
            for (Person person : scenario.getPopulation().getPersons().values()) {
                if (!Tools.isInSubPopulation(person) && Tools.isCarAvailable(person)) {
                    Tools.setCarASCIfDoesntExist(person, 0.0);
                }
            }
        }
    }

    public void updateASCs(int iteration) {
        double[][] odCorrections = odErrors.getODCorrections();
        double currentLearningRate = currentLearningRate(iteration);

        for (Person person : population.getPersons().values()) {
            if (Tools.isInSubPopulation(person) || !Tools.isCarAvailable(person)) {
                continue;
            }

            List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(person.getSelectedPlan());

            double personDeltaSum = 0.0;
            int validTrips = 0;

            for (DiscreteModeChoiceTrip trip : trips) {
                Coord origin = trip.getOriginActivity().getCoord();
                Coord destination = trip.getDestinationActivity().getCoord();
                int groupOrigin = populationGroups.getGroup(origin);
                int groupDestination = populationGroups.getGroup(destination);

                if (groupOrigin < 0 || groupDestination < 0
                    || groupOrigin >= odCorrections.length
                    || groupDestination >= odCorrections[groupOrigin].length) {
                    continue;
                }

                personDeltaSum += getDeltaAsc(odCorrections[groupOrigin][groupDestination], currentLearningRate);
                validTrips++;
            }

            if (validTrips > 0) {
                double avgDelta = personDeltaSum / validTrips;
                if (Math.abs(avgDelta) < ascDeadband) {
                    avgDelta = 0.0;
                } else {
                    double magnitude = Math.min(maxAscStep, Math.max(minAscStep, Math.abs(avgDelta)));
                    avgDelta = Math.copySign(magnitude, avgDelta);
                }

                Tools.incrementCarASC(person, avgDelta, maxAbsoluteAsc);
            }
        }
    }

    private double getDeltaAsc(double odCorrection, double currentLearningRate) {
        return currentLearningRate * odCorrection;
    }

    private double currentLearningRate(int iteration) {
        int effectiveIteration = Math.max(0, iteration - warmupIterations);
        return Math.max(minimumLearningRate, initialLearningRate * Math.pow(learningRateDecay, effectiveIteration));
    }

    private void rebuildPopulationGroupsIfRequired(){
        List<Integer> updates = config.getGridRebuildUpdates();
        int index = updates.indexOf(numUpdates);
        if (index >= 0) {
            populationGroups.reBuild(
                    config.getGridRebuildInitialCellSizes().get(index),
                    config.getGridRebuildMinCellSizes().get(index),
                    config.getGridRebuildMaxPopulations().get(index));
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (!calibrationEnabled) {
            return;
        }
        int iteration = event.getIteration();
        // in the first iteration, we plot the boxes
        if (iteration==0) {
            correctionHeatMap.plotAverageCarAsc(population, populationGroups, outputHierarchy, tripListConverter, iteration);
        }

        // then, each interval iterations, we update the ASCs and plot the boxes again
        if (iteration >= warmupIterations && iteration % updateInterval == 0) {
            rebuildPopulationGroupsIfRequired();
            updateASCs(iteration);
            correctionHeatMap.plotAverageCarAsc(population, populationGroups, outputHierarchy, tripListConverter, iteration);
            numUpdates++;
        }
    }
}
