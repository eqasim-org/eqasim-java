package org.eqasim.jakarta.scenario;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.jakarta.mode_choice.JakartaModeChoiceModule;
import org.eqasim.jakarta.scenario.RunAdaptConfig;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

public class RunAdaptConfig {
	static public void main(String[] args) throws ConfigurationException {
		ConfigAdapter.run(args, EqasimConfigurator.getConfigGroups(), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config) {
		// Ignore some input files
		//config.transit().setVehiclesFile(null);
		//config.households().setInputFile(null);
		
	
		//set link dynamics and seep mode
		
		config.qsim().setLinkDynamics(LinkDynamics.SeepageQ);
		
		
		List<String> seepMode = new LinkedList<>(config.qsim().getSeepModes());
		seepMode.add("motorcycle");
		seepMode.add("mcodt");
		seepMode.remove("bike");
		config.qsim().setSeepModes(seepMode);
		
		
		List<String> mainMode = new LinkedList<>(config.qsim().getMainModes());
		mainMode.add("motorcycle");
		mainMode.add("mcodt");
		mainMode.add("carodt");
		config.qsim().setSeepModes(mainMode);
		

		// Set up mode choice
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		

		eqasimConfig.setCostModel(TransportMode.car, JakartaModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, JakartaModeChoiceModule.PT_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.motorcycle, JakartaModeChoiceModule.MOTORCYCLE_COST_MODEL_NAME);
		eqasimConfig.setCostModel("mcodt", JakartaModeChoiceModule.MCODT_COST_MODEL_NAME);
		eqasimConfig.setCostModel("carodt", JakartaModeChoiceModule.CARODT_COST_MODEL_NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(JakartaModeChoiceModule.MODE_AVAILABILITY_NAME);
		Collection<String> tripConstraints = dmcConfig.getTripConstraints();
		tripConstraints.add("WalkDurationConstraint");
		dmcConfig.setTripConstraints(tripConstraints);
		

		//QsimConfigGroup eqasimConfig = QsimConfigGroup.get(config);	
		
		List<String> networkModes = new LinkedList<>(config.plansCalcRoute().getNetworkModes());
		networkModes.add("taxi");
		networkModes.add("carodt");
		networkModes.add("mcodt");
		networkModes.add("motorcycle");
		config.plansCalcRoute().setNetworkModes(networkModes);
		
		
		//RunAdaptConfig.QsimConfigGroup().getConfig().setSeepMode
		//Run.getConfig.
		
		
		ModeParams taxiParams = new ModeParams("taxi");
		config.planCalcScore().addModeParams(taxiParams);
		
		ModeParams motorcycleParams = new ModeParams("motorcycle");
		config.planCalcScore().addModeParams(motorcycleParams);
		
		ModeParams carodtParams = new ModeParams("carodt");
		config.planCalcScore().addModeParams(carodtParams);
		
		ModeParams mcodtParams = new ModeParams("mcodt");
		config.planCalcScore().addModeParams(mcodtParams);
		
		
		
	}

	private static Object QsimConfigGroup() {
		// TODO Auto-generated method stub
		return null;
	}
}
