package org.eqasim.jakarta;



import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.jakarta.eventhandling.MyEventHandler1;
import org.eqasim.jakarta.eventhandling.MyEventHandler2;
import org.eqasim.jakarta.eventhandling.MyEventHandler3;
import org.eqasim.jakarta.eventhandling.MyEventHandler4;
import org.eqasim.jakarta.eventhandling.MyEventHandler5;
import org.eqasim.jakarta.eventhandling.MyEventHandler6;
import org.eqasim.jakarta.eventhandling.MyEventHandler7;
import org.eqasim.jakarta.mode_choice.JakartaModeChoiceModule;
import org.eqasim.jakarta.roadpricing.JakartaMcRoadPricingModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.roadpricing.RoadPricingModule;









public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();
		
		

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		EqasimConfigGroup.get(config).setTripAnalysisInterval(1);
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		EqasimConfigurator.adjustScenario(scenario);
		
		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
		eqasimConfig.setEstimator("walk", "jWalkEstimator");
		eqasimConfig.setEstimator("pt", "jPTEstimator");
		eqasimConfig.setEstimator("motorcycle", "jMotorcycleEstimator");
		eqasimConfig.setEstimator("car", "jCarEstimator");
		eqasimConfig.setEstimator("carodt", "jCarodtEstimator");
		eqasimConfig.setEstimator("mcodt", "jMcodtEstimator");
		
		//DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
		
		//List<String> availableModes = new ArrayList<>(dmcConfig.getCarModeAvailabilityConfig().getAvailableModes());
		//availableModes.add("car_odt");
		//dmcConfig.getCarModeAvailabilityConfig().setAvailableModes(availableModes);
		

		Controler controller = new Controler(scenario);
		EqasimConfigurator.configureController(controller);
		controller.addOverridingModule(new JakartaMcRoadPricingModule());
		// add the events handlers
		controller.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				this.addEventHandlerBinding().toInstance( new MyEventHandler1() );
				this.addEventHandlerBinding().toInstance( new MyEventHandler2() );
				this.addEventHandlerBinding().toInstance( new MyEventHandler3() );
				this.addEventHandlerBinding().toInstance( new MyEventHandler4() );
				this.addEventHandlerBinding().toInstance( new MyEventHandler5() );
				this.addEventHandlerBinding().toInstance( new MyEventHandler6() );
				this.addEventHandlerBinding().toInstance( new MyEventHandler7() );
					}
			  	
				
				});
		//controller.addOverridingModule(new RoadPricingModule());
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new JakartaModeChoiceModule(cmd));
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.run();
	}
}