package org.eqasim.ile_de_france;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import javax.management.InvalidAttributeValueException;

import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierUtils;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.controler.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;

import com.graphhopper.jsprit.analysis.toolbox.Plotter;
import com.graphhopper.jsprit.core.algorithm.PrettyAlgorithmBuilder;
import com.graphhopper.jsprit.core.algorithm.SearchStrategy;
import com.graphhopper.jsprit.core.algorithm.SearchStrategyManager;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithmFactory;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.ServiceDeliveriesFirstConstraint;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.vehicle.InfiniteFleetManagerFactory;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleFleetManager;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Solutions;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException, InvalidAttributeValueException, ExecutionException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), IDFConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);
		
		FreightConfigGroup freightConfig = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
		freightConfig.setCarriersFile("/home/shoerl/lyon/data/carriers.xml");
		freightConfig.setCarriersVehicleTypesFile("/home/shoerl/lyon/freight_vehicle_types.xml");
		freightConfig.setTravelTimeSliceWidth(900);

		Scenario scenario = ScenarioUtils.createScenario(config);
		IDFConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		FreightUtils.loadCarriersAccordingToFreightConfig(scenario);

		FreightUtils.getCarriers(scenario).getCarriers().values().forEach(c -> {
			CarrierUtils.setJspritIterations(c, 20);
		});

		FreightUtils.runJsprit(scenario, freightConfig);

		Controler controller = new Controler(scenario);
		IDFConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new CarrierModule());

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CarrierPlanStrategyManagerFactory.class).toInstance(new CarrierPlanStrategyManagerFactory() {
					@Override
					public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
						GenericStrategyManager<CarrierPlan, Carrier> manager = new GenericStrategyManager<>();
						manager.setMaxPlansPerAgent(1);
						manager.addStrategy(new GenericPlanStrategyImpl<>(new KeepSelected<>()), null, 1.0);
						return manager;
					}
				});

				bind(CarrierScoringFunctionFactory.class).toInstance(new CarrierScoringFunctionFactory() {
					@Override
					public ScoringFunction createScoringFunction(Carrier carrier) {
						return new SumScoringFunction();
					}
				});
			}
		});

		{
			Network roadNetwork = NetworkUtils.createNetwork();
			new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));

			NetworkBasedTransportCosts.Builder costBuilder = NetworkBasedTransportCosts.Builder
					.newInstance(roadNetwork);
			costBuilder.setTimeSliceWidth(900);
			costBuilder.addVehicleTypeSpecificCosts("randomVehicleTypeId", 20.0, 30.0, 0.35);

			NetworkBasedTransportCosts costs = costBuilder.build();

			for (Carrier carrier : FreightUtils.getCarriers(scenario).getCarriers().values()) {
				carrier.clearPlans();

				VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier,
						roadNetwork);
				vrpBuilder.setRoutingCost(costs);

				VehicleRoutingProblem vrp = vrpBuilder.build();
				
				/*SearchStrategyManager searchStrategyManager = new SearchStrategyManager();
				SearchStrategy strategy = new SearchStr
				
				searchStrategyManager.addStrategy(strategy, 1.0);
				VehicleRoutingAlgorithm algorithm = new VehicleRoutingAlgorithm(vrp, searchStrategyManager);

				VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(vrp);*/
				
				// VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(vrp);
				
				Jsprit.Builder algorithmBuilder = Jsprit.Builder.newInstance(vrp);

				StateManager stateManager = new StateManager(vrp);
				
				ConstraintManager constraintManager = new ConstraintManager(vrp, stateManager);
				constraintManager.addConstraint(new ServiceDeliveriesFirstConstraint(), ConstraintManager.Priority.CRITICAL);
				
				algorithmBuilder.setStateAndConstraintManager(stateManager, constraintManager);
				algorithmBuilder.addCoreStateAndConstraintStuff(true);
				
				VehicleRoutingAlgorithm algorithm = algorithmBuilder.buildAlgorithm();

				VehicleRoutingProblemSolution solution = Solutions.bestOf(algorithm.searchSolutions());
				CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);

				NetworkRouter.routePlan(plan, costs);
				carrier.setSelectedPlan(plan);

				SolutionPrinter.print(vrp, solution, SolutionPrinter.Print.VERBOSE);
				
			}
		}
		
		controller.run();
	}
}