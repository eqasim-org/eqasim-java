package org.eqasim.core.simulation;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.EqasimComponentsModule;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.eqasim.core.components.transit.EqasimTransitModule;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.AbstractAccessModule;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.AbstractAccessModuleConfigGroup;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.AbstractAccessQSimModule;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.routing.AbstractAccessRouteFactory;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.routing.DefaultAbstractAccessRoute;
import org.eqasim.core.simulation.calibration.CalibrationConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.households.Household;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class EqasimConfigurator {
	protected final List<ConfigGroup> configGroups = new LinkedList<>();
	protected final List<ConfigGroup> optionalConfigGroups = new LinkedList<>();
	protected final List<AbstractModule> modules = new LinkedList<>();
	protected final List<AbstractQSimModule> qsimModules = new LinkedList<>();

	public EqasimConfigurator() {
		configGroups.addAll(Arrays.asList( //
				new SwissRailRaptorConfigGroup(), //
				new EqasimConfigGroup(), //
				new DiscreteModeChoiceConfigGroup(), //
				new CalibrationConfigGroup()//
		));

		optionalConfigGroups.add(new AbstractAccessModuleConfigGroup());

		modules.addAll(Arrays.asList( //
				new SwissRailRaptorModule(), //
				new EqasimTransitModule(), //
				new DiscreteModeChoiceModule(), //
				new EqasimComponentsModule() //
		));

		qsimModules.addAll(Arrays.asList( //
				new EqasimTransitQSimModule(), //
				new EqasimTrafficQSimModule(),
				new AbstractAccessQSimModule()//
		));
	}

	public ConfigGroup[] getConfigGroups() {
		return configGroups.toArray(new ConfigGroup[configGroups.size()]);
	}

	public void addOptionalConfigGroups(Config config) {
		for(ConfigGroup configGroup: optionalConfigGroups) {
			if(config.getModules().get(configGroup.getName()) != null) {
				config.addModule(configGroup);
			}
		}
	}

	public List<AbstractModule> getModules() {
		return modules;
	}

	public List<AbstractQSimModule> getQSimModules() {
		return qsimModules;
	}

	public void configureController(Controler controller) {
		for (AbstractModule module : getModules()) {
			controller.addOverridingModule(module);
		}

		for (AbstractQSimModule module : getQSimModules()) {
			controller.addOverridingQSimModule(module);
		}

		controller.configureQSimComponents(configurator -> {
			EqasimTransitQSimModule.configure(configurator, controller.getConfig());
			AbstractAccessQSimModule.configure(configurator, controller.getConfig());
		});

		if(controller.getConfig().getModules().containsKey(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME)) {
			AbstractAccessModuleConfigGroup abstractAccessModuleConfigGroup = (AbstractAccessModuleConfigGroup) controller.getConfig().getModules().get(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME);
			controller.addOverridingModule(new AbstractAccessModule(abstractAccessModuleConfigGroup));
		}
	}

	public void configureScenario(Scenario scenario) {
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultAbstractAccessRoute.class, new AbstractAccessRouteFactory());
	}

	public void adjustScenario(Scenario scenario) {
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					copyAttribute(household, person, "bikeAvailability");
					copyAttribute(household, person, "spRegion");
				}
			}
		}
	}

	static protected void copyAttribute(Household household, Person person, String attribute) {
		if (household.getAttributes().getAsMap().containsKey(attribute)) {
			person.getAttributes().putAttribute(attribute, household.getAttributes().getAttribute(attribute));
		}
	}
}
