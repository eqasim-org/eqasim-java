package org.eqasim.switzerland.ch_cmdp;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.switzerland.ch_cmdp.config.SwissPTZonesConfigGroup;
import org.eqasim.switzerland.ch_cmdp.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.households.Household;
import org.matsim.vehicles.VehicleType;

import java.util.*;

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
		// Only copy inputs consumed by this runner. Person municipalityType and
		// bikeAvailability are unused (activity municipalityType and bikeAvail are used).
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			var members = household.getMemberIds().stream()
					.map(id -> scenario.getPopulation().getPersons().get(id))
					.filter(Objects::nonNull).toList();
			for (Person person : members) {
				for (String attribute : List.of("spRegion", "numberOfCars", "cantonId",
						"incomePerCapita", "cantonName", "ovgk")) {
					copyRequiredAttribute(household, person, attribute);
				}
			}
			if (members.stream().anyMatch(p -> p.getAttributes().getAttribute("carOwnershipRatio") == null)) {
				long adults = members.stream()
						.filter(p -> ((Number) p.getAttributes().getAttribute("age")).intValue() >= 18).count();
				for (Person person : members) {
					if (person.getAttributes().getAttribute("carOwnershipRatio") == null) {
						double cars = Double.parseDouble(person.getAttributes()
								.getAttribute("numberOfCars").toString().replace("+", ""));
						double ratio = adults > 0 ? 1.0 - cars / adults : 0.0;
						person.getAttributes().putAttribute("carOwnershipRatio", Math.min(Math.max(0.0, ratio), 1.0));
					}
				}
			}
		}
	}

	private static void copyRequiredAttribute(Household household, Person person, String attribute) {
		if (person.getAttributes().getAttribute(attribute) != null) return;
		Object value = household.getAttributes().getAttribute(attribute);
		if (value == null) {
			throw new IllegalArgumentException("Missing required attribute '" + attribute
					+ "' on person " + person.getId() + " and household " + household.getId());
		}
		person.getAttributes().putAttribute(attribute, value);
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

	public void removeBikeFromRoutedModes(Config config) {
		QSimConfigGroup qsimConfigGroup = config.qsim();
		Set<String> mainModes = new HashSet<>(qsimConfigGroup.getMainModes());
		// if bike is routed, we should turn this off
		if (mainModes.contains(TransportMode.bike)) {
			// remove it from qsim
			mainModes.remove(TransportMode.bike);
			qsimConfigGroup.setMainModes(mainModes);

			// remove it from the router
			RoutingConfigGroup routingConfig  = config.routing();
			List<String> routedModes = new ArrayList<>(routingConfig.getNetworkModes());
			if (routedModes.contains(TransportMode.bike)) {
				routedModes.remove(TransportMode.bike);
				routingConfig.setNetworkModes(routedModes);
			}

			// change link dynamics to FIFO
			qsimConfigGroup.setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);

			// remove from tt calculator
			TravelTimeCalculatorConfigGroup ttcConfig = config.travelTimeCalculator();
            ttcConfig.getAnalyzedModes().remove(TransportMode.bike);

			// set the beeline factor and speed for bike mode
			RoutingConfigGroup.TeleportedModeParams bikeParams = routingConfig.getOrCreateModeRoutingParams(TransportMode.bike);
			bikeParams.setBeelineDistanceFactor(1.4);
			bikeParams.setTeleportedModeSpeed(4.0);
		}

	}
}
