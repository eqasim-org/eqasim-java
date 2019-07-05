package org.eqasim.simulation;

import java.util.Arrays;
import java.util.List;

import org.eqasim.components.traffic.EqasimTrafficModule;
import org.eqasim.components.transit.EqasimTransitModule;
import org.eqasim.components.transit.EqasimTransitQSimModule;
import org.eqasim.components.transit.routing.DefaultEnrichedTransitRoute;
import org.eqasim.components.transit.routing.DefaultEnrichedTransitRouteFactory;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.households.Household;

import ch.ethz.matsim.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class ScenarioConfigurator {
	static public List<AbstractModule> getModules() {
		return Arrays.asList( //
				new SwissRailRaptorModule(), //
				new EqasimTransitModule(), //
				new EqasimTrafficModule(), //
				new DiscreteModeChoiceModule() //
		);
	}

	static public List<AbstractQSimModule> getQSimModules() {
		return Arrays.asList( //
				new EqasimTransitQSimModule() //
		);
	}

	static public void configureController(Controler controller) {
		for (AbstractModule module : getModules()) {
			controller.addOverridingModule(module);
		}

		for (AbstractQSimModule module : getQSimModules()) {
			controller.addOverridingQSimModule(module);
		}

		controller.configureQSimComponents(configurator -> {
			EqasimTransitQSimModule.configure(configurator);
		});
	}

	static public void configureScenario(Scenario scenario) {
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
				new DefaultEnrichedTransitRouteFactory());
	}

	static public void adjustScenario(Scenario scenario) {
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					person.getAttributes().putAttribute("bikeAvailability",
							household.getAttributes().getAttribute("bikeAvailability"));
					person.getAttributes().putAttribute("spRegion", household.getAttributes().getAttribute("spRegion"));
				}
			}
		}
	}
}
