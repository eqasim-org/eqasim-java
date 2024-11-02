package org.eqasim.core.simulation;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import org.eqasim.core.components.EqasimComponentsModule;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.raptor.EqasimRaptorConfigGroup;
import org.eqasim.core.components.raptor.EqasimRaptorModule;
import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.eqasim.core.components.transit.EqasimTransitModule;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.mode_choice.constraints.leg_time.LegTimeConstraintConfigGroup;
import org.eqasim.core.simulation.mode_choice.constraints.leg_time.LegTimeConstraintModule;
import org.eqasim.core.simulation.mode_choice.epsilon.EpsilonModule;
import org.eqasim.core.simulation.modes.feeder_drt.MultiModeFeederDrtModule;
import org.eqasim.core.simulation.modes.feeder_drt.config.MultiModeFeederDrtConfigGroup;
import org.eqasim.core.simulation.modes.feeder_drt.mode_choice.EqasimFeederDrtModeChoiceModule;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.TransitWithAbstractAbstractAccessModuleConfigGroup;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.TransitWithAbstractAccessModule;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.TransitWithAbstractAccessQSimModule;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice.TransitWithAbstractAccessModeChoiceModule;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.AbstractAccessRouteFactory;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.DefaultAbstractAccessRoute;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.eqasim.core.simulation.termination.EqasimTerminationModule;
import org.eqasim.core.simulation.termination.mode_share.TerminationModeShareModule;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.VDFModule;
import org.eqasim.core.simulation.vdf.VDFQSimModule;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.households.Household;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class EqasimConfigurator {
	public EqasimConfigurator() {
		// Standard eqasim configuration
		registerConfigGroup(new SwissRailRaptorConfigGroup(), false);
		registerConfigGroup(new EqasimConfigGroup(), false);
		registerConfigGroup(new DiscreteModeChoiceConfigGroup(), false);
		registerConfigGroup(new EqasimRaptorConfigGroup(), false);

		registerModule(new SwissRailRaptorModule());
		registerModule(new EqasimTransitModule());
		registerModule(new DiscreteModeChoiceModule());
		registerModule(new EqasimComponentsModule());
		registerModule(new EpsilonModule());
		registerModule(new EqasimRaptorModule());
		registerModule(new EqasimModeChoiceModule());

		registerQSimModule(new EqasimTransitQSimModule());
		registerQSimModule(new EqasimTrafficQSimModule());

		registerComponents(EqasimTransitQSimModule::configure);

		// Termination
		registerConfigGroup(new EqasimTerminationConfigGroup(), false);
		registerModule(new EqasimTerminationModule(), EqasimTerminationConfigGroup.GROUP_NAME);
		registerModule(new TerminationModeShareModule(), EqasimTerminationConfigGroup.GROUP_NAME);

		// DRT functionality
		registerConfigGroup(new DvrpConfigGroup(), true);
		registerConfigGroup(new MultiModeDrtConfigGroup(), true);

		registerModule(new DvrpModule(), DvrpConfigGroup.GROUP_NAME);
		registerModule(new MultiModeDrtModule(), MultiModeDrtConfigGroup.GROUP_NAME);

		registerComponents((components, config) -> {
			MultiModeDrtConfigGroup configGroup = MultiModeDrtConfigGroup.get(config);
			DvrpQSimComponents.activateAllModes(configGroup).configure(components);
		}, MultiModeDrtConfigGroup.GROUP_NAME);

		// Feeder functionality
		registerConfigGroup(new MultiModeFeederDrtConfigGroup(), true);

		registerModule(new MultiModeFeederDrtModule(), MultiModeFeederDrtConfigGroup.GROUP_NAME);
		registerModule(new EqasimFeederDrtModeChoiceModule(), MultiModeFeederDrtConfigGroup.GROUP_NAME);

		// Abstract access
		registerConfigGroup(new TransitWithAbstractAbstractAccessModuleConfigGroup(), true);

		registerModule(new TransitWithAbstractAccessModule(),
				TransitWithAbstractAbstractAccessModuleConfigGroup.GROUP_NAME);
		registerModule(new TransitWithAbstractAccessModeChoiceModule(),
				TransitWithAbstractAbstractAccessModuleConfigGroup.GROUP_NAME);

		registerQSimModule(new TransitWithAbstractAccessQSimModule(),
				TransitWithAbstractAbstractAccessModuleConfigGroup.GROUP_NAME);

		registerComponents(TransitWithAbstractAccessQSimModule::configure,
				TransitWithAbstractAbstractAccessModuleConfigGroup.GROUP_NAME);

		// VDF functionality
		registerConfigGroup(new VDFConfigGroup(), true);
		registerModule(new VDFModule(), VDFConfigGroup.GROUP_NAME);
		registerQSimModule(new VDFQSimModule(), VDFConfigGroup.GROUP_NAME);

		registerConfigGroup(new VDFEngineConfigGroup(), true);
		registerModule(new VDFEngineModule(), VDFEngineConfigGroup.GROUP_NAME);
		registerComponents((components, config) -> {
			VDFEngineModule.configureQSim(components);
		}, VDFEngineConfigGroup.GROUP_NAME);

		// Leg time constraint functionality
		registerConfigGroup(new LegTimeConstraintConfigGroup(), true);
		registerModule(new LegTimeConstraintModule(), LegTimeConstraintConfigGroup.GROUP_NAME);

		// Emissions
		registerConfigGroup(new EmissionsConfigGroup(), true);
	}

	private record ConfigGroupItem(ConfigGroup configGroup, boolean isOptional) {
	}

	private record ModuleItem(AbstractModule module, String configName) {
	}

	private record QSimModuleItem(AbstractQSimModule module, String configName) {
	}

	private record ComponentsItem(BiConsumer<QSimComponentsConfig, Config> configurator, String configName) {
	}

	private final List<ConfigGroupItem> configGroups = new LinkedList<>();
	private final List<ModuleItem> modules = new LinkedList<>();
	private final List<QSimModuleItem> qsimModules = new LinkedList<>();
	private final List<ComponentsItem> components = new LinkedList<>();

	public void registerConfigGroup(ConfigGroup configGroup, boolean isOptional) {
		configGroups.add(new ConfigGroupItem(configGroup, isOptional));
	}

	public void registerModule(AbstractModule module) {
		registerModule(module, null);
	}

	public void registerModule(AbstractModule module, String configName) {
		modules.add(new ModuleItem(module, configName));
	}

	public void registerQSimModule(AbstractQSimModule module) {
		registerQSimModule(module, null);
	}

	public void registerQSimModule(AbstractQSimModule module, String configName) {
		qsimModules.add(new QSimModuleItem(module, configName));
	}

	public void registerComponents(BiConsumer<QSimComponentsConfig, Config> comoonent) {
		registerComponents(comoonent, null);
	}

	public void registerComponents(BiConsumer<QSimComponentsConfig, Config> comoonent, String configName) {
		components.add(new ComponentsItem(comoonent, configName));
	}

	public void updateConfig(Config config) {
		for (ConfigGroupItem item : configGroups) {
			ConfigGroup existing = config.getModules().get(item.configGroup.getName());

			if (!item.isOptional || existing != null) {
				if (existing == null) {
					config.addModule(item.configGroup);
				} else if (existing.getClass().equals(item.configGroup.getClass())) {
					// fine, already properly initialized
				} else if (existing.getClass().equals(ConfigGroup.class)) {
					// not initialized yet, will be transformed
					config.addModule(item.configGroup);
				} else {
					throw new IllegalStateException("Config group with this name already existing in a different type: "
							+ item.configGroup.getName());
				}
			}
		}
	}

	public List<AbstractModule> getModules(Config config) {
		List<AbstractModule> active = new LinkedList<>();

		for (ModuleItem item : modules) {
			if (item.configName == null || config.getModules().containsKey(item.configName)) {
				active.add(item.module);
			}
		}

		return Collections.unmodifiableList(active);
	}

	public List<AbstractQSimModule> getQSimModules(Config config) {
		List<AbstractQSimModule> active = new LinkedList<>();

		for (QSimModuleItem item : qsimModules) {
			if (item.configName == null || config.getModules().containsKey(item.configName)) {
				active.add(item.module);
			}
		}

		return Collections.unmodifiableList(active);
	}

	public void configureComponents(QSimComponentsConfig components, Config config) {
		for (ComponentsItem item : this.components) {
			if (item.configName == null || config.getModules().containsKey(item.configName)) {
				item.configurator.accept(components, config);
			}
		}
	}

	public void configureController(Controler controller) {
		Config config = controller.getConfig();

		for (AbstractModule module : getModules(config)) {
			controller.addOverridingModule(module);
		}

		for (AbstractQSimModule qsimModule : getQSimModules(config)) {
			controller.addOverridingQSimModule(qsimModule);
		}

		controller.configureQSimComponents(components -> {
			configureComponents(components, config);
		});
	}

	public void configureScenario(Scenario scenario) {
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class,
				new DrtRouteFactory());
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultAbstractAccessRoute.class,
				new AbstractAccessRouteFactory());
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
