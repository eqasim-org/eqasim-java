package org.eqasim.switzerland.ch_cmdp.StrategyWeightDecay;

import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
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
    private static final int REMAINING_ITERATIONS_TO_START_DECAY = 30;
    private static final List<Integer> DMC_BOOTSTRAP_ITERATIONS = List.of(6,11,16);
    private static final List<Double> DMC_BOOTSTRAP_WEIGHT = List.of(0.17,0.14,0.1);
    private static final List<Double> ROUTING_NOISE = List.of(0.04, 0.03, 0.02);

    private final StrategyManager strategyManager;
    private GenericPlanStrategy<Plan, Person> dmcStrategy;
    private GenericPlanStrategy<Plan, Person> reRouteStrategy;
    private GenericPlanStrategy<Plan, Person> keepLastSelectedStrategy;
    private final StWeights initialWeights;
    private final Set<String> subpopulations;
    private final int lastIteration;
    // routing (since we increase the innovation weight in the first few iterations, we increase noise too)
    private final EqasimTravelDisutilityFactory eqasimTravelDisutilityFactory;
    private final double initialRoutingNoise;

    @Inject
    public StrategyWeightDecay(ReplanningConfigGroup replanningConfigGroup, StrategyManager strategyManager,
                               ControllerConfigGroup controllerConfigGroup, EqasimTravelDisutilityFactory eqasimTravelDisutilityFactory) {
        this.strategyManager = strategyManager;
        this.subpopulations = getSubPopulations(replanningConfigGroup);
        // get strategies
        initStrategies();
        // get initial weights
        this.initialWeights = initWeights();
        this.lastIteration = controllerConfigGroup.getLastIteration();
        // routing noise
        this.eqasimTravelDisutilityFactory = eqasimTravelDisutilityFactory;
        this.initialRoutingNoise = eqasimTravelDisutilityFactory.getSigmaNoise();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        int iteration = event.getIteration();

        for (int i = 0; i < DMC_BOOTSTRAP_ITERATIONS.size(); i++) {
            int bootstrapIteration = DMC_BOOTSTRAP_ITERATIONS.get(i);
            if (iteration < bootstrapIteration) {
                double dmcWeight = DMC_BOOTSTRAP_WEIGHT.get(i);
                double reRouteWeight = initialWeights.reRouteWeight;
                double keepLastSelectedWeight = round(1.0 - dmcWeight - reRouteWeight);
                double routingNoise = ROUTING_NOISE.get(i);
                strategyManager.changeWeightOfStrategy(dmcStrategy, null, dmcWeight);
                strategyManager.changeWeightOfStrategy(reRouteStrategy, null, reRouteWeight);
                strategyManager.changeWeightOfStrategy(keepLastSelectedStrategy,null,keepLastSelectedWeight);
                eqasimTravelDisutilityFactory.setSigmaNoise(routingNoise);
                return;
            }
        }

        if (startDecay(iteration)) {
            StWeights weights = getStrategiesWeight(iteration);
            strategyManager.changeWeightOfStrategy(dmcStrategy, null, weights.dmcWeight);
            strategyManager.changeWeightOfStrategy(reRouteStrategy, null, weights.reRouteWeight);
            strategyManager.changeWeightOfStrategy(keepLastSelectedStrategy,null,weights.keepLastSelectedWeight);
            setReRouteWeightForAllPopulations(weights.reRouteWeight);
            return;
        }

        strategyManager.changeWeightOfStrategy(dmcStrategy,null,initialWeights.dmcWeight);
        strategyManager.changeWeightOfStrategy(reRouteStrategy,null,initialWeights.reRouteWeight);
        strategyManager.changeWeightOfStrategy(keepLastSelectedStrategy,null,initialWeights.keepLastSelectedWeight);
        eqasimTravelDisutilityFactory.setSigmaNoise(initialRoutingNoise);
    }

    private void setReRouteWeightForAllPopulations(double reRouteWeight) {
        for (String subpopulation : subpopulations) {
            if (subpopulation != null){
                for (GenericPlanStrategy<Plan, Person> strategy : strategyManager.getStrategies(subpopulation)) {
                    if (isReRouteStrategy(strategy)) {
                        strategyManager.changeWeightOfStrategy(strategy, subpopulation, reRouteWeight);
                    } else if (isKeepLastSelectedStrategy(strategy)) {
                        strategyManager.changeWeightOfStrategy(strategy, subpopulation, round(1.0-reRouteWeight));
                    }
                }
            }
        }
    }

    private static double round(double x){
        return Math.round(x * 1000.0) / 1000.0;
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
            } else {
                factor = 1.2 / 5.0;
            }
        }
        return new StWeights(initialWeights.dmcWeight * factor,
                initialWeights.reRouteWeight * factor); // KeepLastSelected unchanged or adjust as needed
    }

    private boolean startDecay(int iteration){
        return REMAINING_ITERATIONS_TO_START_DECAY > lastIteration - iteration;
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
            this.dmcWeight = round(dmcWeight);
            this.reRouteWeight = round(reRouteWeight);
            this.keepLastSelectedWeight = round(keepLastSelectedWeight);
        }
        public StWeights(double dmcWeight, double reRouteWeight) {
            this.dmcWeight = round(dmcWeight);
            this.reRouteWeight = round(reRouteWeight);
            this.keepLastSelectedWeight = round(1.0 - this.dmcWeight - this.reRouteWeight);
        }
    }
}
