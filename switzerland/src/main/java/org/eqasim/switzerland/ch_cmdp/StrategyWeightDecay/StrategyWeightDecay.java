package org.eqasim.switzerland.ch_cmdp.StrategyWeightDecay;

import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class StrategyWeightDecay implements IterationStartsListener {
    private final int REMAINING_ITERATIONS_TO_START_DECAY = 30;

    private final StrategyManager strategyManager;
    private GenericPlanStrategy<Plan, Person> dmcStrategy;
    private GenericPlanStrategy<Plan, Person> reRouteStrategy;
    private GenericPlanStrategy<Plan, Person> keepLastSelectedStrategy;
    private final StWeights initialWeights;
    private final Set<String> subpopulations;
    private final int lastIteration;

    @Inject
    public StrategyWeightDecay(ReplanningConfigGroup replanningConfigGroup, StrategyManager strategyManager, ControllerConfigGroup controllerConfigGroup) {
        this.strategyManager = strategyManager;
        this.subpopulations = getSubPopulations(replanningConfigGroup);
        // get strategies
        initStrategies();
        // get initial weights
        this.initialWeights = initWeights();
        this.lastIteration = controllerConfigGroup.getLastIteration();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        int iteration = event.getIteration();
        if (startDecay(iteration)) {
            StWeights weights = getStrategiesWeight(iteration);
            strategyManager.changeWeightOfStrategy(dmcStrategy, null, weights.dmcWeight);
            strategyManager.changeWeightOfStrategy(reRouteStrategy, null, weights.reRouteWeight);
            strategyManager.changeWeightOfStrategy(keepLastSelectedStrategy, null, weights.keepLastSelectedWeight);
            setReRouteWeightForAllPopulations(weights.reRouteWeight);
        }
    }

    private void setReRouteWeightForAllPopulations(double reRouteWeight) {
        for (String subpopulation : subpopulations) {
            if (subpopulation != null){
                for (GenericPlanStrategy<Plan, Person> strategy : strategyManager.getStrategies(subpopulation)) {
                    if (isReRouteStrategy(strategy)) {
                        strategyManager.changeWeightOfStrategy(strategy, subpopulation, reRouteWeight);
                    } else if (isKeepLastSelectedStrategy(strategy)) {
                        strategyManager.changeWeightOfStrategy(strategy, subpopulation, Math.round((1.0-reRouteWeight)*1000.0)/1000.0);
                    }
                }
            }
        }
    }

    private StWeights getStrategiesWeight(int iteration) {
        int remainingIterations = lastIteration - iteration;
        double factor = 1.0;
        boolean startDecay = startDecay(iteration);

        if (startDecay) {
            double step = (double) REMAINING_ITERATIONS_TO_START_DECAY /4;
            if (remainingIterations > 3*step) {
                factor = 3.5 / 5.0;
            } else if (remainingIterations > 2*step) {
                factor = 2.5 / 5.0;
            } else if (remainingIterations > step) {
                factor = 1.8 / 5.0;
            } else if (iteration >= 90) {
                factor = 1.2 / 5.0;
            }
        }
        return new StWeights(initialWeights.dmcWeight * factor,
                initialWeights.reRouteWeight * factor); // KeepLastSelected unchanged or adjust as needed
    }

    private boolean startDecay(int iteration){
        return iteration > 45 && REMAINING_ITERATIONS_TO_START_DECAY > lastIteration - iteration;
    }

    private void initStrategies() {
        for (GenericPlanStrategy<Plan, Person> strategy : strategyManager.getStrategies(null)) {
            if (isDmcStrategy(strategy)) {
                this.dmcStrategy = strategy;
            } else if (isReRouteStrategy(strategy)) {
                this.reRouteStrategy = strategy;
            } else if (isKeepLastSelectedStrategy(strategy)) {
                this.keepLastSelectedStrategy = strategy;
            }
        }
    }

    private StWeights initWeights() {
        double dmcWeight = 0.0;
        double reRouteWeight = 0.0;
        double keepLastSelectedWeight = 1.0;
        List<GenericPlanStrategy<Plan, Person>> strategies = strategyManager.getStrategies(null);
        for (GenericPlanStrategy<Plan, Person> strategy : strategies) {
            double weight = strategyManager.getWeights(null).get(strategies.indexOf(strategy));
            if (strategy == dmcStrategy) {
                dmcWeight = weight;
            } else if (strategy == reRouteStrategy) {
                reRouteWeight = weight;
            } else if (strategy == keepLastSelectedStrategy) {
                keepLastSelectedWeight = weight;
            }
        }
        return new StWeights(dmcWeight, reRouteWeight, keepLastSelectedWeight);
    }

    private Set<String> getSubPopulations(ReplanningConfigGroup replanningConfigGroup) {
        Set<String> subpopulations = new HashSet<>();
        for (ReplanningConfigGroup.StrategySettings strategy : replanningConfigGroup.getStrategySettings()) {
            subpopulations.add(strategy.getSubpopulation());
        }
        return subpopulations;
    }

    private boolean isDmcStrategy(GenericPlanStrategy<Plan, Person> strategy) {
        return strategy.toString().contains("DiscreteModeChoice");
    }

    private boolean isReRouteStrategy(GenericPlanStrategy<Plan, Person> strategy) {
        return strategy.toString().contains("ReRoute");
    }

    private boolean isKeepLastSelectedStrategy(GenericPlanStrategy<Plan, Person> strategy) {
        return strategy.toString().contains("KeepSelected");
    }

    private static class StWeights {
        public final double dmcWeight;
        public final double reRouteWeight;
        public final double keepLastSelectedWeight;

        public StWeights(double dmcWeight, double reRouteWeight, double keepLastSelectedWeight) {
            this.dmcWeight = Math.round(dmcWeight * 1000.0) / 1000.0;
            this.reRouteWeight = Math.round(reRouteWeight * 1000.0) / 1000.0;
            this.keepLastSelectedWeight = Math.round(keepLastSelectedWeight * 1000.0) / 1000.0;
        }
        public StWeights(double dmcWeight, double reRouteWeight) {
            this.dmcWeight = Math.round(dmcWeight * 1000.0) / 1000.0;
            this.reRouteWeight = Math.round(reRouteWeight * 1000.0) / 1000.0;
            this.keepLastSelectedWeight = Math.round((1.0 - this.dmcWeight - this.reRouteWeight) * 1000.0) / 1000.0;
        }
    }
}