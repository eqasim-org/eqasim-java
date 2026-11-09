package org.eqasim.switzerland.ch_cmdp.routing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Set;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.PersonPrepareForSimAlgorithm;

/** Runs after MATSim assigns facilities, vehicles and routing modes, before its whole-plan preparation. */
public final class MissingTripRoutingModule extends AbstractModule {
    @Override public void install() {
        bind(MissingTripRouting.Worker.class);
        addPersonPrepareForSimAlgorithm().to(Preparation.class);
    }

    public static final class Preparation implements PersonPrepareForSimAlgorithm {
        private final Scenario scenario;
        private final Provider<MissingTripRouting.Worker> workers;
        private boolean done;
        @Inject public Preparation(Scenario scenario, Provider<MissingTripRouting.Worker> workers) {
            this.scenario = scenario; this.workers = workers;
        }
        @Override public synchronized void run(Person ignored) {
            if (done) return;
            MissingTripRouting.run(scenario, scenario.getConfig().global().getNumberOfThreads(), Set.of(), workers);
            done = true;
        }
    }
}
