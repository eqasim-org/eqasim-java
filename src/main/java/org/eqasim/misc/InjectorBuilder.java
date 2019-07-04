package org.eqasim.misc;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;

public class InjectorBuilder {
	private final Scenario scenario;
	private AbstractModule module = new ControlerDefaultsModule();

	public InjectorBuilder(Scenario scenario) {
		this.scenario = scenario;
	}

	public InjectorBuilder addOverridingModule(AbstractModule module) {
		this.module = AbstractModule.override(Collections.singleton(this.module), module);
		return this;
	}

	public InjectorBuilder addOverridingModules(List<AbstractModule> modules) {
		for (AbstractModule module : modules) {
			addOverridingModule(module);
		}

		return this;
	}

	public com.google.inject.Injector build() {
		return Injector.createInjector( //
				scenario.getConfig(), //
				new ScenarioByInstanceModule(scenario), //
				new NewControlerModule(), //
				new ControlerDefaultCoreListenersModule(), //
				module //
		);
	}
}
