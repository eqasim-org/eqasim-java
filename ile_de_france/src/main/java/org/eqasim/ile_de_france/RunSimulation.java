package org.eqasim.ile_de_france;

import java.util.Collections;
import java.util.Set;

import org.eqasim.core.scenario.validation.VehiclesValidator;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.core.simulation.vdf.travel_time.VDFTravelTime;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.groups.QSimConfigGroup.NodeTransition;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter", "use-vdf", "use-vdf-engine") //
				.allowOptions("passenger-speed-factor") //
				.build();

		IDFConfigurator configurator = new IDFConfigurator(cmd);
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		configurator.updateConfig(config);

		if (cmd.getOption("use-vdf").map(Boolean::parseBoolean).orElse(false)) {
			VDFConfigGroup vdfConfig = new VDFConfigGroup();
			config.addModule(vdfConfig);

			vdfConfig.setCapacityFactor(1.0);
			vdfConfig.setModes(Set.of("car", "car_passenger"));

			config.qsim().setFlowCapFactor(1e9);
			config.qsim().setStorageCapFactor(1e9);
			config.qsim().setStuckTime(3600.0);

			if (cmd.getOption("use-vdf-engine").map(Boolean::parseBoolean).orElse(false)) {
				VDFEngineConfigGroup engineConfig = new VDFEngineConfigGroup();
				engineConfig.setModes(Set.of("car", "car_passenger"));
				engineConfig.setGenerateNetworkEvents(false);
				config.addModule(engineConfig);

				config.qsim().setMainModes(Collections.emptySet());
			}
		}

		cmd.applyConfiguration(config);
		VehiclesValidator.validate(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);

		double passengerSpeedFactor = cmd.getOption("passenger-speed-factor").map(Double::parseDouble).orElse(1.0);

		if (cmd.hasOption("use-vdf")) {
			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addTravelTimeBinding("car_passenger").toProvider(new Provider<>() {
						@Inject
						VDFTravelTime delegate;

						@Override
						public TravelTime get() {
							return new TravelTime() {
								@Override
								public double getLinkTravelTime(Link link, double time, Person person,
										Vehicle vehicle) {
									double travelTime = delegate.getLinkTravelTime(link, time, person, vehicle);
									travelTime /= passengerSpeedFactor;
									double linkTravelTime = Math.floor(travelTime);
									return linkTravelTime + 1.0;
								}
							};
						}
					});
				}
			});
		}

		controller.run();
	}
}