package org.eqasim.examples.zurich_parking;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
//import org.eqasim.examples.zurich_parking.analysis.parking.ParkingSearchMetricsListener;
import org.eqasim.examples.zurich_parking.mode_choice.ZurichParkingModule;
import org.eqasim.examples.zurich_parking.parking.ParkingListener;
import org.eqasim.examples.zurich_parking.parking.manager.ZurichParkingManager;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingModule;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.NoVehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationToNearbyParking;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.routing.WithinDayParkingRouter;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchPopulationModule;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchQSimModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.router.AStarEuclideanFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;

public class RunZurichParkingSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter")
				.build();

		ZurichParkingConfigurator configurator = new ZurichParkingConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());

		{ // Configure parking
			ParkingSearchConfigGroup parkingSearchConfigGroup = new ParkingSearchConfigGroup();

			// set parameters
			parkingSearchConfigGroup.setParkingSearchStrategy(ParkingSearchStrategy.Random);

			// add to config
			config.addModule(parkingSearchConfigGroup);

			// additional requirements
			config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);

			// Do not simulate PT
			config.transit().setUseTransit(true);
			config.transit().setUsingTransitInMobsim(false);

		}

		cmd.applyConfiguration(config);

		{ // Add parking to mode choice
			EqasimConfigGroup eqasimConfigGroup = EqasimConfigGroup.get(config);
			eqasimConfigGroup.setEstimator("car", ZurichParkingModule.CAR_ESTIMATOR_NAME);
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));

		{// Configure controller for parking
			controller.addOverridingModule(new ZurichParkingModule(cmd));

//			// Add parking search module
//			controller.addOverridingModule(new AbstractModule() {
//				@Override
//				public void install() {
//					ParkingSlotVisualiser visualiser = new ParkingSlotVisualiser(scenario);
//					addEventHandlerBinding().toInstance(visualiser);
//					addControlerListenerBinding().toInstance(visualiser);
//				}
//			});

			// No need to route car routes in Routing module in advance, as they are
			// calculated on the fly
			if (!controller.getConfig().getModules().containsKey(DvrpConfigGroup.GROUP_NAME)) {
				controller.getConfig().addModule(new DvrpConfigGroup());
			}

			controller.addOverridingModule(new DvrpTravelTimeModule());
			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(TravelDisutilityFactory.class).annotatedWith(DvrpModes.mode(TransportMode.car))
							.toInstance(TimeAsTravelDisutility::new);
					bind(Network.class).annotatedWith(DvrpModes.mode(TransportMode.car))
							.to(Key.get(Network.class, Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)));
					install(new DvrpModeRoutingModule(TransportMode.car, new AStarEuclideanFactory()));
					bind(Network.class).annotatedWith(Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING))
							.to(Network.class)
							.asEagerSingleton();
					bind(ParkingSearchManager.class).to(ZurichParkingManager.class).asEagerSingleton();
					this.install(new ParkingSearchQSimModule());
					addControlerListenerBinding().to(org.matsim.contrib.parking.parkingsearch.evaluation.ParkingListener.class);
					addControlerListenerBinding().to(ZurichParkingManager.class);
					bind(ParkingRouter.class).to(WithinDayParkingRouter.class);
//					bind(VehicleTeleportationLogic.class).to(VehicleTeleportationToNearbyParking.class);
					bind(VehicleTeleportationLogic.class).to(NoVehicleTeleportationLogic.class);
				}
			});

			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					QSimComponentsConfig components = new QSimComponentsConfig();

					new StandardQSimComponentConfigurator(controller.getConfig()).configure(components);
					components.removeNamedComponent(PopulationModule.COMPONENT_NAME);
					components.addNamedComponent(ParkingSearchPopulationModule.COMPONENT_NAME);

					bind(QSimComponentsConfig.class).toInstance(components);
				}
			});

			// Add parking listeners
			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					Vehicle2DriverEventHandler vehicle2DriverEventHandler = new Vehicle2DriverEventHandler();
					ParkingListener parkingListener = new ParkingListener(vehicle2DriverEventHandler, new RoadNetwork(scenario.getNetwork()),
							0.0, 30*3600.0, 3600.0, 500.0);
					ParkingSearchMetricsListener parkingSearchMetricsListener = new ParkingSearchMetricsListener(vehicle2DriverEventHandler,
							scenario.getNetwork());

					bind(ParkingListener.class).toInstance(parkingListener);
					addEventHandlerBinding().toInstance(vehicle2DriverEventHandler);

					addEventHandlerBinding().toInstance(parkingListener);
					addControlerListenerBinding().toInstance(parkingListener);

//					addEventHandlerBinding().toInstance(parkingSearchMetricsListener);
//					addControlerListenerBinding().toInstance(parkingSearchMetricsListener);
				}
			});
		}

		controller.run();
	}
}