package org.eqasim.examples.zurich_carsharing;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.zurich_carsharing.listeners.CarsharingAvailabilityListener;
import org.eqasim.examples.zurich_carsharing.listeners.RentalHandlers;
import org.eqasim.examples.zurich_carsharing.mode_choice.CarsharingModeChoiceModule;
import org.eqasim.examples.zurich_carsharing.mode_choice.CarshringEqasimModule;
import org.eqasim.examples.zurich_carsharing.utils.VehicleChoice;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.events.handlers.PersonArrivalDepartureHandler;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.CarsharingManagerNew;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemand;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemandImpl;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgent;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipReader;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTrip;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTripImpl;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.manager.routers.RouterProviderImpl;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.contrib.carsharing.models.ChooseVehicleTypeExample;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.models.KeepingTheCarModelExample;
import org.matsim.contrib.carsharing.qsim.CarSharingQSimModule;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
import org.matsim.contrib.carsharing.relocation.utils.ChooseTheCompanyPriceBased;
import org.matsim.contrib.carsharing.runExample.CarsharingUtils;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Key;
import com.google.inject.name.Names;

public class RunSimulation {

	public static void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				SwitzerlandConfigurator.getConfigGroups());
		config.addModule(new CarsharingConfigGroup());
		config.addModule(new FreeFloatingConfigGroup());
		config.addModule(new DvrpConfigGroup());
		
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new CarsharingModeChoiceModule(cmd));
		controller.addOverridingModule(new CarshringEqasimModule());
		controller.addOverridingModule(new DvrpTravelTimeModule());

		installCarSharing(controller);

		controller.run();
	}

	public static void installCarSharing(final Controler controler) throws IOException {
		final Scenario scenario = controler.getScenario();

		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		Set<String> modes = new HashSet<>();
		modes.add("car");
		Network networkFF = NetworkUtils.createNetwork();
		filter.filter(networkFF, modes);

		CarsharingXmlReaderNew reader = new CarsharingXmlReaderNew(networkFF);

		CarsharingConfigGroup configGroup = (CarsharingConfigGroup) scenario.getConfig().getModules()
				.get(CarsharingConfigGroup.GROUP_NAME);
		// this is necessary to populate the companies list
		reader.readFile(configGroup.getvehiclelocations());
		Set<String> carsharingCompanies = reader.getCompanies().keySet();

		MembershipReader membershipReader = new MembershipReader();
		membershipReader.readFile(configGroup.getmembership());
		final MembershipContainer memberships = membershipReader.getMembershipContainer();

		final CostsCalculatorContainer costsCalculatorContainer = CarsharingUtils
				.createCompanyCostsStructure(carsharingCompanies);

		// final SetupListener setupListener = new SetupListener();
		final CarsharingListener carsharingListener = new CarsharingListener();
		// final KmlWriterListener relocationListener = new
		// KmlWriterListener(configGroup.getStatsWriterFrequency());
		// final FFVehiclesRentalsWriterListener vehicleRentalsWriterListener = new
		// FFVehiclesRentalsWriterListener(
		// configGroup.getStatsWriterFrequency());

		final KeepingTheCarModel keepingCarModel = new KeepingTheCarModelExample();
		final ChooseTheCompany chooseCompany = new ChooseTheCompanyPriceBased();
		final ChooseVehicleType chooseCehicleType = new ChooseVehicleTypeExample();
		final RouterProvider routerProvider = new RouterProviderImpl();
		final CurrentTotalDemand currentTotalDemand = new CurrentTotalDemandImpl(networkFF);
		final CarsharingManagerInterface carsharingManager = new CarsharingManagerNew();
		final RouteCarsharingTrip routeCarsharingTrip = new RouteCarsharingTripImpl();
		// final AverageDemandRelocationListener demandRelocationListener = new
		// AverageDemandRelocationListener();
		// final PastIntervalDemandRelocationListener demandRelocationListener =
		// new PastIntervalDemandRelocationListener();

		final VehicleChoiceAgent vehicleChoiceAgent = new VehicleChoice();
		controler.addOverridingQSimModule(new CarSharingQSimModule());

		controler.configureQSimComponents(configurator -> {
			EqasimTransitQSimModule.configure(configurator);
			CarSharingQSimModule.configureComponents(configurator);
		});

		// ===adding carsharing objects on supply and demand infrastructure ===
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(KeepingTheCarModel.class).toInstance(keepingCarModel);
				bind(ChooseTheCompany.class).toInstance(chooseCompany);
				bind(ChooseVehicleType.class).toInstance(chooseCehicleType);
				bind(RouterProvider.class).toInstance(routerProvider);
				bind(CurrentTotalDemand.class).toInstance(currentTotalDemand);
				bind(RouteCarsharingTrip.class).toInstance(routeCarsharingTrip);
				bind(CostsCalculatorContainer.class).toInstance(costsCalculatorContainer);
				bind(MembershipContainer.class).toInstance(memberships);
				bind(CarsharingSupplyInterface.class).to(CarsharingSupplyContainer.class);
				bind(CarsharingManagerInterface.class).toInstance(carsharingManager);
				// bind(CarsharingVehicleRelocationContainer.class).toInstance(carsharingVehicleRelocation);
				// bind(CarSharingDemandTracker.class).toInstance(demandTracker);
				bind(DemandHandler.class).asEagerSingleton();
				bind(RentalHandlers.class).asEagerSingleton();
				// bind(DemandDistributionHandler.class).asEagerSingleton();

				bind(VehicleChoiceAgent.class).toInstance(vehicleChoiceAgent);
				// bind(MobismBeforeSimStepRelocationListener.class).asEagerSingleton();
				// bind(MobsimRelocationBackIntervalListener.class).asEagerSingleton();

				bind(CarsharingAvailabilityListener.class).asEagerSingleton();
				// bind(Network.class).annotatedWith(Names.named(DvrpModule.DVRP_ROUTING)).to(Network.class);

				bind(Network.class).annotatedWith(Names.named("carnetwork")).toInstance(networkFF);
				bind(TravelTime.class).annotatedWith(Names.named("ff"))
						.to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
				bind(Network.class).annotatedWith(Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING))
						.to(Network.class);

			}
		});

		// === adding controller listeners and event handlers
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toInstance(carsharingListener);
				addControlerListenerBinding().toInstance((CarsharingManagerNew) carsharingManager);
				addControlerListenerBinding().to(CarsharingAvailabilityListener.class);
				addMobsimListenerBinding().to(CarsharingAvailabilityListener.class);
				addEventHandlerBinding().to(PersonArrivalDepartureHandler.class);
				addEventHandlerBinding().to(DemandHandler.class);
			}
		});

		// === routing moduels for carsharing trips ===
		controler.addOverridingModule(CarsharingUtils.createRoutingModule());
	}

}
