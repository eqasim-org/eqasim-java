package org.eqasim.switzerland.ch_cmdp;

//import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.switzerland.ch_cmdp.config.SwissPTZonesConfigGroup;
import org.eqasim.switzerland.ch_cmdp.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.households.Household;
import org.matsim.vehicles.VehicleType;

import java.util.Objects;

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
			// Compute car ownership ratio by household
			int numAdults = (int) household.getMemberIds().stream()
				.map(id -> scenario.getPopulation().getPersons().get(id))
				.filter(Objects::nonNull)
				.mapToInt(p -> (int) p.getAttributes().getAttribute("age"))
				.filter(age -> age >= 18)
				.count();
			String numCarsStr = (String) household.getAttributes().getAttribute("numberOfCars");
			double numCars = Double.parseDouble(numCarsStr.replace("+", ""));
			double carRatio = numAdults > 0 ? (1.0 - numCars / numAdults) : 0.0;

			// Copy household attributes to persons
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					copyAttribute(household, person, "spRegion");
					copyAttribute(household, person, "numberOfCars");
					copyAttribute(household, person, "cantonId");
					copyAttribute(household, person, "municipalityType");
					copyAttribute(household, person, "incomePerCapita");
					copyAttribute(household, person, "cantonName");
					copyAttribute(household, person, "ovgk");
					person.getAttributes().putAttribute("carOwnershipRatio", Math.min(Math.max(0.0, carRatio), 1.0));
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
		//EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		config.qsim().setNumberOfThreads(Math.min(12, Runtime.getRuntime().availableProcessors()));
		config.global().setNumberOfThreads(Runtime.getRuntime().availableProcessors());

	}
}
