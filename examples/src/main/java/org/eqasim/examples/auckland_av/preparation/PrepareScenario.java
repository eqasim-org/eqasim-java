package org.eqasim.examples.auckland_av.preparation;

import java.io.File;
import java.io.IOException;

import org.eqasim.automated_vehicles.components.AvConfigurator;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.examples.auckland_av.AucklandAvModeAvailability;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

/**
 * Input path should be the path to the generated Auckland scenario from the
 * pipeline. Output path should be the path to a new directory where the final
 * scenario will be saved.
 */
public class PrepareScenario {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("input-path") + "/auckland_config.xml",
				EqasimConfigurator.getConfigGroups());
		AvConfigurator.configure(config);
		cmd.applyConfiguration(config);

		OperatorConfig operatorConfig = AVConfigGroup.getOrCreate(config)
				.getOperatorConfig(OperatorConfig.DEFAULT_OPERATOR_ID);
		operatorConfig.getGeneratorConfig().setNumberOfVehicles(Integer.parseInt(cmd.getOptionStrict("fleet-size")));
		operatorConfig.getDispatcherConfig().setType("GlobalBipartiteMatchingDispatcher");

		operatorConfig.getWaitingTimeConfig().setEstimationAlpha(0.1);
		operatorConfig.getWaitingTimeConfig().setEstimationLinkAttribute("avWaitingTimeGroup");

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
		dmcConfig.setModeAvailability(AucklandAvModeAvailability.NAME);

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		EqasimConfigurator.adjustScenario(scenario);

		AvConfigurator.configureCarLinks(scenario);
		AvConfigurator.configureWaitingTimeGroupFromShapefile(
				new File(cmd.getOptionStrict("input-path") + "/zones.shp"), "group", scenario.getNetwork());

		ScenarioOptions scenarioOptions = new ScenarioOptions(new File(cmd.getOptionStrict("output-path")),
				ScenarioOptionsBase.getDefault());

		new ConfigWriter(config).write(cmd.getOptionStrict("output-path") + "/auckland_config.xml");
		new FacilitiesWriter(scenario.getActivityFacilities())
				.write(cmd.getOptionStrict("output-path") + "/auckland_facilities.xml.gz");
		new NetworkWriter(scenario.getNetwork()).write(cmd.getOptionStrict("output-path") + "/auckland_network.xml.gz");
		new PopulationWriter(scenario.getPopulation())
				.write(cmd.getOptionStrict("output-path") + "/auckland_population.xml.gz");
		new TransitScheduleWriter(scenario.getTransitSchedule())
				.writeFile(cmd.getOptionStrict("output-path") + "/auckland_transit_schedule.xml");
		new VehicleWriterV1(scenario.getTransitVehicles())
				.writeFile(cmd.getOptionStrict("output-path") + "/auckland_transit_vehicles.xml");

	}
}
