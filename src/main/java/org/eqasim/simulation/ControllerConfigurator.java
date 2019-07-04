package org.eqasim.simulation;

import java.util.Arrays;
import java.util.List;

import org.eqasim.simulation.transit.EqasimTransitQSimModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class ControllerConfigurator {
	static public List<AbstractModule> getModules() {
		return Arrays.asList( //
				new SwissRailRaptorModule() //
		);
	}

	static public void apply(Controler controller) {
		for (AbstractModule module : getModules()) {
			controller.addOverridingModule(module);
		}
		
		controller.configureQSimComponents(EqasimTransitQSimModule::configure);
	}
}
