package org.eqasim.simulation;

import java.util.Arrays;
import java.util.List;

import org.eqasim.simulation.transit.EqasimTransitQSimModule;
import org.eqasim.simulation.transit.routing.DefaultEnrichedTransitRoute;
import org.eqasim.simulation.transit.routing.DefaultEnrichedTransitRouteFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class ScenarioConfigurator {
	static public List<AbstractModule> getModules() {
		return Arrays.asList( //
				new SwissRailRaptorModule() //
		);
	}

	static public void configureController(Controler controller) {
		for (AbstractModule module : getModules()) {
			controller.addOverridingModule(module);
		}

		controller.configureQSimComponents(EqasimTransitQSimModule::configure);
	}

	static public void configureScenario(Scenario scenario) {
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
	}
}
