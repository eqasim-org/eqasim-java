package org.eqasim.core.simulation;

import java.util.*;
import java.util.function.BiConsumer;

import org.eqasim.core.components.EqasimComponentsModule;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.eqasim.core.components.transit.EqasimTransitModule;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.calibration.CalibrationConfigGroup;
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
import org.matsim.contrib.dvrp.run.MultiModal;
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
	protected final List<ConfigGroup> configGroups = new LinkedList<>();
	protected final List<AbstractModule> modules = new LinkedList<>();
	protected final List<AbstractQSimModule> qsimModules = new LinkedList<>();
	private final Map<String, Collection<AbstractModule>> optionalModules = new HashMap<>();
	private final Map<String, Collection<AbstractQSimModule>> optionalQSimModules = new HashMap<>();
	private final Map<String, List<BiConsumer<Controler, QSimComponentsConfig>>> optionalQSimComponentConfigurationSteps = new HashMap<>();
	private final Map<String, ConfigGroup> optionalConfigGroups = new HashMap<>();

	public EqasimConfigurator() {
		configGroups.addAll(Arrays.asList( //
				new SwissRailRaptorConfigGroup(), //
				new EqasimConfigGroup(), //
				new DiscreteModeChoiceConfigGroup(), //
				new CalibrationConfigGroup() //
		));

		modules.addAll(Arrays.asList( //
				new SwissRailRaptorModule(), //
				new EqasimTransitModule(), //
				new DiscreteModeChoiceModule(), //
				new EqasimComponentsModule() //
		));

		qsimModules.addAll(Arrays.asList( //
				new EqasimTransitQSimModule(), //
				new EqasimTrafficQSimModule() //
		));

		this.registerOptionalConfigGroup(new MultiModeDrtConfigGroup(),
				Collections.singleton(new MultiModeDrtModule()),
				Collections.emptyList(),
				Collections.singletonList((controller, components) ->
						DvrpQSimComponents.activateAllModes((MultiModal<?>) controller.getConfig().getModules().get(MultiModeDrtConfigGroup.GROUP_NAME)).configure(components)));

		this.registerOptionalConfigGroup(new DvrpConfigGroup(), Collections.singleton(new DvrpModule()));
	}

	public ConfigGroup[] getConfigGroups() {
		return configGroups.toArray(ConfigGroup[]::new);
	}

	public List<AbstractModule> getModules() {
		return modules;
	}

	public List<AbstractQSimModule> getQSimModules() {
		return qsimModules;
	}

	public void configureController(Controler controller) {

		// The optional modules are added after the non-optional ones because we consider that their bindings have less priority
		this.optionalModules.entrySet().stream()
				.filter(e -> controller.getConfig().getModules().containsKey(e.getKey()))
				.map(Map.Entry::getValue)
				.flatMap(Collection::stream)
				.forEach(controller::addOverridingModule);

		for (AbstractModule module : getModules()) {
			controller.addOverridingModule(module);
		}

		this.optionalQSimModules.entrySet().stream()
				.filter(e -> controller.getConfig().getModules().containsKey(e.getKey()))
				.map(Map.Entry::getValue)
				.flatMap(Collection::stream)
				.forEach(controller::addOverridingQSimModule);

		for (AbstractQSimModule module : getQSimModules()) {
			controller.addOverridingQSimModule(module);
		}

		controller.configureQSimComponents(components -> {
			optionalQSimComponentConfigurationSteps.entrySet().stream()
					.filter(e -> controller.getConfig().getModules().containsKey(e.getKey()))
					.map(Map.Entry::getValue)
					.flatMap(Collection::stream)
					.forEach(step -> step.accept(controller, components));
			EqasimTransitQSimModule.configure(components, controller.getConfig());
		});
	}

	protected void registerOptionalConfigGroup(ConfigGroup configGroup) {
		registerOptionalConfigGroup(configGroup, new ArrayList<>());
	}

	protected void registerOptionalConfigGroup(ConfigGroup configGroup, Collection<AbstractModule> modules) {
		registerOptionalConfigGroup(configGroup, modules, new ArrayList<>());
	}

	protected void registerOptionalConfigGroup(ConfigGroup configGroup, Collection<AbstractModule> modules, Collection<AbstractQSimModule> qsimModules) {
		registerOptionalConfigGroup(configGroup, modules, qsimModules, new ArrayList<>());
	}
	protected void registerOptionalConfigGroup(ConfigGroup configGroup, Collection<AbstractModule> modules, Collection<AbstractQSimModule> qsimModules, List<BiConsumer<Controler, QSimComponentsConfig>> componentsConsumers) {
		this.optionalConfigGroups.put(configGroup.getName(), configGroup);
		this.optionalModules.putIfAbsent(configGroup.getName(), new ArrayList<>());
		this.optionalModules.get(configGroup.getName()).addAll(modules);

		this.optionalQSimModules.putIfAbsent(configGroup.getName(), new ArrayList<>());
		this.optionalQSimModules.get(configGroup.getName()).addAll(qsimModules);

		this.optionalQSimComponentConfigurationSteps.putIfAbsent(configGroup.getName(), new ArrayList<>());
		this.optionalQSimComponentConfigurationSteps.get(configGroup.getName()).addAll(componentsConsumers);
	}

	public void addOptionalConfigGroups(Config config) {
		for(ConfigGroup configGroup: optionalConfigGroups.values()) {
			if(config.getModules().get(configGroup.getName()) != null) {
				config.addModule(configGroup);
			}
		}
	}

	public void configureScenario(Scenario scenario) {
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());
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
