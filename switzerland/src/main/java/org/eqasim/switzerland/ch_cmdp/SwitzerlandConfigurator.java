package org.eqasim.switzerland.ch_cmdp;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.switzerland.ch.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.households.Household;
import org.matsim.vehicles.VehicleType;

public class SwitzerlandConfigurator extends EqasimConfigurator {
	private CommandLine cmd;
	public SwitzerlandConfigurator(CommandLine cmd) {
		super(cmd);
		this.cmd = cmd;

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
		// eqasimConfig.setEstimator("car", "SwissCarEstimator");
		// eqasimConfig.setEstimator("pt", "SwissPtEstimator");
		// eqasimConfig.setEstimator("bike", "SwissBikeEstimator");
		// eqasimConfig.setEstimator("walk", "SwissWalkEstimator");
		// eqasimConfig.setEstimator("outsider", "SwissZeroUtilityEstimator");
		// eqasimConfig.setEstimator("car_passenger", "SwissZeroUtilityEstimator");
		// eqasimConfig.setEstimator("truck", "SwissZeroUtilityEstimator");
	}
}
