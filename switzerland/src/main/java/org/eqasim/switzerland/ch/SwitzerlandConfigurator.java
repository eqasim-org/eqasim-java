package org.eqasim.switzerland.ch;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.switzerland.ch.config.SwissPTZonesConfigGroup;
import org.eqasim.switzerland.ch.mode_choice.SwissModeChoiceModule;
import org.eqasim.switzerland.zurich.mode_choice.ZurichModeAvailability;
import org.eqasim.switzerland.zurich.mode_choice.constraints.InfiniteHeadwayConstraint;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichBikeUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichCarUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichPtUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.utilities.estimators.ZurichWalkUtilityEstimator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.households.Household;
import org.matsim.vehicles.VehicleType;

import java.util.HashSet;
import java.util.Set;

public class SwitzerlandConfigurator extends EqasimConfigurator {
	private CommandLine cmd;
	public SwitzerlandConfigurator(CommandLine cmd) {
		super(cmd);
		this.cmd = cmd;
		
		registerConfigGroup(new SwissPTZonesConfigGroup(), true);
		registerModule(new SwissModeChoiceModule(cmd));
	}

	@Override
	public void adjustScenario(Scenario scenario) {
		super.adjustScenario(scenario);

		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					copyAttribute(household, person, "spRegion");
					copyAttribute(household, person, "bikeAvailability");
					copyAttribute(household, person, "cantonId");
					copyAttribute(household, person, "municipalityType");
					copyAttribute(household, person, "incomePerCapita");
				}
			}
		}
	}
	
	public void adjustPTpcu(Scenario scenario) {
		
		if (cmd.getOption("samplingRateForPT").isPresent()) {
			
			double samplingRate = Double.parseDouble(cmd.getOption("samplingRateForPT").get());
			
			for (VehicleType vt : scenario.getTransitVehicles().getVehicleTypes().values()) {
				
				vt.setPcuEquivalents(vt.getPcuEquivalents() * samplingRate);
			}
			
		}
	}

	public void configure(Config config) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		config.qsim().setNumberOfThreads(Math.min(12, Runtime.getRuntime().availableProcessors()));
		config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());

		// Estimators
		eqasimConfig.setEstimator("car", "SwissCarEstimator");
		eqasimConfig.setEstimator("pt", "SwissPtEstimator");
		eqasimConfig.setEstimator("bike", "SwissBikeEstimator");
		eqasimConfig.setEstimator("walk", "SwissWalkEstimator");
		// eqasimConfig.setEstimator("outsider", "SwissZeroUtilityEstimator");
		eqasimConfig.setEstimator("car_passenger", "SwissZeroUtilityEstimator");
		// eqasimConfig.setEstimator("truck", "SwissZeroUtilityEstimator");
	}
}
