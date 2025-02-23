package org.eqasim.core.misc;

import java.util.List;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

public class InjectorBuilder {
	private final Controler controller;

	public InjectorBuilder(Scenario scenario, EqasimConfigurator configurator) {
		this.controller = new Controler(scenario);
		configurator.configureController(controller);
	}

	public InjectorBuilder addOverridingModule(AbstractModule module) {
		controller.addOverridingModule(module);
		return this;
	}

	public InjectorBuilder addOverridingModules(List<AbstractModule> modules) {
		for (AbstractModule module : modules) {
			controller.addOverridingModule(module);
		}

		return this;
	}

	public com.google.inject.Injector build() {
		return controller.getInjector();
	}
}