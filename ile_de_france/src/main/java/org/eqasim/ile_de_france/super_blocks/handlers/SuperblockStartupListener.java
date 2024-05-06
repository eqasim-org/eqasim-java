package org.eqasim.ile_de_france.super_blocks.handlers;

import com.google.inject.Inject;
import org.eqasim.core.scenario.routing.PopulationRouter;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;


public class SuperblockStartupListener implements StartupListener {


    private final PopulationRouter populationRouter;
    private final Population population;

    @Inject
    public SuperblockStartupListener(PopulationRouter populationRouter, Population population) {
        this.populationRouter = populationRouter;
        this.population = population;
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        try {
            this.populationRouter.run(this.population);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
