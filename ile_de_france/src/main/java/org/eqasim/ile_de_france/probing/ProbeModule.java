package org.eqasim.ile_de_france.probing;

import java.io.File;
import java.util.Set;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.ile_de_france.mode_choice.IDFModeAvailability;
import org.eqasim.ile_de_france.mode_choice.costs.IDFCarCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFPtCostModel;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFBicycleUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFCarPassengerUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFCarUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFPtUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFCarPassengerPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFParkingPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPtPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class ProbeModule extends AbstractModule {
    @Override
    public void install() {
        ProbeConfigGroup config = ProbeConfigGroup.get(getConfig());

        if (config.useProbeTravelTimes) {
            for (String mode : Set.of("car", "car_passenger")) {
                // delegate
                bind(Key.get(RoutingModule.class, Names.named("base_" + mode)))
                        .toProvider(new NetworkRoutingProvider(mode));

                // override
                addRoutingModuleBinding(mode).toProvider(new Provider<>() {
                    @Inject
                    Injector injector;

                    @Override
                    public ProbeNetworkRoutingModule get() {
                        RoutingModule delegate = injector
                                .getInstance(Key.get(RoutingModule.class, Names.named("base_" + mode)));
                        return new ProbeNetworkRoutingModule(delegate, mode);
                    }
                });
            }

            // explicit bindings are required
            bind(IDFCarUtilityEstimator.class);
            bind(IDFCarPassengerUtilityEstimator.class);
            bind(IDFPtUtilityEstimator.class);
            bind(IDFBicycleUtilityEstimator.class);
            bind(WalkUtilityEstimator.class);
            bind(IDFCarCostModel.class);
            bind(IDFPtCostModel.class);
            bind(IDFModeAvailability.class);
        }
    }

    @Provides
    @Singleton
    PredictionWriter providePredictionWriter(Population population, ActivityFacilities facilities,
            TripRouter tripRouter, OutputDirectoryHierarchy outputHierarchy, Injector injector) {
        File outputPath = new File(outputHierarchy.getOutputFilename("predictions.json"));
        PredictionWriter writer = new PredictionWriter(population, tripRouter, facilities, outputPath);

        // mode specific predictors
        writer.addPredictor("car", "car", injector.getInstance(CarPredictor.class));
        writer.addPredictor("pt", "pt", injector.getInstance(IDFPtPredictor.class));
        writer.addPredictor("car_passenger", "car_passenger", injector.getInstance(IDFCarPassengerPredictor.class));
        writer.addPredictor("bicycle", "bicycle", injector.getInstance(BikePredictor.class));
        writer.addPredictor("walk", "walk", injector.getInstance(WalkPredictor.class));

        // mode independent predictors
        writer.addPredictor("person", injector.getInstance(IDFPersonPredictor.class));
        writer.addPredictor("spatial", injector.getInstance(IDFSpatialPredictor.class));
        writer.addPredictor("parking", injector.getInstance(IDFParkingPredictor.class));

        // mode estimators
        writer.addEstimator("car", injector.getInstance(IDFCarUtilityEstimator.class));
        writer.addEstimator("car_passenger", injector.getInstance(IDFCarPassengerUtilityEstimator.class));
        writer.addEstimator("pt", injector.getInstance(IDFPtUtilityEstimator.class));
        writer.addEstimator("bicycle", injector.getInstance(IDFBicycleUtilityEstimator.class));
        writer.addEstimator("walk", injector.getInstance(WalkUtilityEstimator.class));

        // mode costs
        writer.addCostModel("car", injector.getInstance(IDFCarCostModel.class));
        writer.addCostModel("pt", injector.getInstance(IDFPtCostModel.class));

        // availability
        writer.addAvailability("standard", injector.getInstance(IDFModeAvailability.class));

        return writer;
    }
}
