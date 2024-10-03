package org.eqasim.core.simulation;

import java.util.*;
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
import org.eqasim.core.simulation.modes.transit_with_abstract_access.TransitWithAbstractAccessModule;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.TransitWithAbstractAbstractAccessModuleConfigGroup;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.TransitWithAbstractAccessQSimModule;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice.TransitWithAbstractAccessModeChoiceModule;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.AbstractAccessRouteFactory;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.DefaultAbstractAccessRoute;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.eqasim.core.simulation.termination.EqasimTerminationModule;
import org.eqasim.core.simulation.termination.mode_share.ModeShareModule;
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
                new EqasimRaptorConfigGroup() //
        ));

        modules.addAll(Arrays.asList( //
                new SwissRailRaptorModule(), //
                new EqasimTransitModule(), //
                new DiscreteModeChoiceModule(), //
                new EqasimComponentsModule(), //
                new EpsilonModule(), //
                new EqasimRaptorModule(),
                new EqasimModeChoiceModule()//
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
        this.registerOptionalConfigGroup(new EqasimTerminationConfigGroup(), List.of(new EqasimTerminationModule(), new ModeShareModule()));
        this.registerOptionalConfigGroup(new MultiModeFeederDrtConfigGroup(), List.of(new MultiModeFeederDrtModule(), new EqasimFeederDrtModeChoiceModule()));
        this.registerOptionalConfigGroup(
                new TransitWithAbstractAbstractAccessModuleConfigGroup(),
                List.of(new TransitWithAbstractAccessModule(),
                        new TransitWithAbstractAccessModeChoiceModule()),
                List.of(new TransitWithAbstractAccessQSimModule()),
                Collections.singletonList((controller, components) -> TransitWithAbstractAccessQSimModule.configure(components, controller.getConfig())));
        this.registerOptionalConfigGroup(new VDFConfigGroup(),
                List.of(new VDFModule()),
                List.of(new VDFQSimModule()));
        this.registerOptionalConfigGroup(new VDFEngineConfigGroup(),
                List.of(new VDFEngineModule()),
                Collections.emptyList(),
                Collections.singletonList((controller, components) -> components.addNamedComponent(VDFEngineModule.COMPONENT_NAME)));
        this.registerOptionalConfigGroup(new LegTimeConstraintConfigGroup(), Collections.singleton(new LegTimeConstraintModule()));
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
        Map<String, ConfigGroup> modules = controller.getConfig().getModules();

        this.optionalModules.keySet().stream()
                .filter(modules::containsKey)
                .map(this.optionalModules::get)
                .flatMap(Collection::stream)
                .forEach(controller::addOverridingModule);

        this.getModules().forEach(controller::addOverridingModule);

        this.optionalQSimModules.keySet().stream()
                .filter(modules::containsKey)
                .map(this.optionalQSimModules::get)
                .flatMap(Collection::stream)
                .forEach(controller::addOverridingQSimModule);
        this.getQSimModules().forEach(controller::addOverridingQSimModule);

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
		this.optionalModules.computeIfAbsent(configGroup.getName(), key -> new ArrayList<>()).addAll(modules);
        this.optionalQSimModules.computeIfAbsent(configGroup.getName(), key -> new ArrayList<>()).addAll(qsimModules);
        this.optionalQSimComponentConfigurationSteps.computeIfAbsent(configGroup.getName(), key -> new ArrayList<>()).addAll(componentsConsumers);
    }

    public void addOptionalConfigGroups(Config config) {
        for (ConfigGroup configGroup : optionalConfigGroups.values()) {
            ConfigGroup existingConfigGroup = config.getModules().get(configGroup.getName());
            // if a config group with the same name exist and is still a generic ConfigGroup instance, we replace it by the optional config group instance
            if (existingConfigGroup != null && existingConfigGroup.getClass().equals(ConfigGroup.class)) {
                config.addModule(configGroup);
            }
        }
    }

    public void configureScenario(Scenario scenario) {
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());
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
