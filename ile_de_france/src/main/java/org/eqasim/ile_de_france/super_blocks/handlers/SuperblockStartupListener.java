package org.eqasim.ile_de_france.super_blocks.handlers;

import org.eqasim.core.scenario.routing.PopulationRouter;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;


public class SuperblockStartupListener implements IterationStartsListener {


    private final PopulationRouter populationRouter;
    private final Population population;

    @Inject
    public SuperblockStartupListener(PopulationRouter populationRouter, Population population) {
        this.populationRouter = populationRouter;
        this.population = population;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if(event.getIteration() == 0) {
            try {
                this.populationRouter.run(this.population);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
