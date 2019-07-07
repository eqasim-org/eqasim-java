package org.eqasim.core.misc;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;

public class InjectorBuilder {
	private final Scenario scenario;
	private AbstractModule module = AbstractModule.emptyModule(); // new ControlerDefaultsModule();

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
		AbstractModule standardModule = new AbstractModule() {
			@Override
			public void install() {
				install(new EventsManagerModule());
				install(new TripRouterModule());
				install(new CharyparNagelScoringFunctionModule());
				install(new TravelDisutilityModule());
				install(new TravelTimeCalculatorModule());
			}
		};

		return Injector.createInjector( //
				scenario.getConfig(), //
				new ScenarioByInstanceModule(scenario), //
				AbstractModule.override(Collections.singleton(standardModule), module));
	}
}