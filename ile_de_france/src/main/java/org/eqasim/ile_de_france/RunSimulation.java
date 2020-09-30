package org.eqasim.ile_de_france;

import java.util.Collection;
import java.util.HashSet;

import org.eqasim.core.components.car_pt.routing.EqasimCarPtModule;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), IDFConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);
	
		
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
	    eqasimConfig.setEstimator("car_pt", "CarPtUtilityEstimator");
	    
	    PlanCalcScoreConfigGroup scoringConfig = config.planCalcScore();
	    ModeParams carPtParams = new ModeParams("car_pt");
	    scoringConfig.addModeParams(carPtParams);
	    
	    ActivityParams params = new ActivityParams( "carPt interaction");
		params.setTypicalDuration(100.0);
		params.setScoringThisActivityAtAll(false);

		scoringConfig.addActivityParams(params);
		
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		//And then you have an attribute "cachedModes". You should augment it with your "car_pt" mode:

		 Collection<String> cachedModes = new HashSet<>(dmcConfig.getCachedModes());
		 cachedModes.add("car_pt");
		 dmcConfig.setCachedModes(cachedModes);

		Scenario scenario = ScenarioUtils.createScenario(config);
		IDFConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		Controler controller = new Controler(scenario);
		IDFConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new EqasimCarPtModule());
		controller.run();
	}
}