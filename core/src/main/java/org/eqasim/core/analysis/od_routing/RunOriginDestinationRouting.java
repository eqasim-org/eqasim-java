package org.eqasim.core.analysis.od_routing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.analysis.od_routing.data.Location;
import org.eqasim.core.analysis.od_routing.data.LocationReader;
import org.eqasim.core.analysis.od_routing.data.ModalTravelTimeMatrix;
import org.eqasim.core.components.travel_time.RecordedTravelTime;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.Injector;

public class RunOriginDestinationRouting {
	static public void main(String[] args)
			throws ConfigurationException, InterruptedException, MalformedURLException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "locations-path", "output-path") //
				.allowOptions("threads", "batch-size", "modes", "travel-times-path", "location-attribute") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);
		config.strategy().clearStrategySettings();

		/*
		 * SwissRailRaptorConfigGroup raptorConfig = (SwissRailRaptorConfigGroup)
		 * config.getModules() .get(SwissRailRaptorConfigGroup.GROUP);
		 * raptorConfig.setUseRangeQuery(false);
		 * 
		 * RangeQuerySettingsParameterSet rangeConfig = new
		 * RangeQuerySettingsParameterSet(); rangeConfig.setMaxEarlierDeparture(30 *
		 * 60); rangeConfig.setMaxLaterDeparture(30 * 60);
		 * rangeConfig.getSubpopulations().add(null);
		 * raptorConfig.addRangeQuerySettings(rangeConfig);
		 * 
		 * RouteSelectorParameterSet selectorConfig = new RouteSelectorParameterSet();
		 * selectorConfig.setBetaDepartureTime(0.0);
		 * selectorConfig.setBetaTravelTime(1.0); selectorConfig.setBetaTransfers(0.0);
		 * selectorConfig.getSubpopulations().add(null);
		 * raptorConfig.addRouteSelector(selectorConfig);
		 */

		for (String mode : Arrays.asList("transit_walk", "access_walk", "egress_walk")) {
			ModeParams modeParams = config.planCalcScore().getOrCreateModeParams(mode);

			modeParams.setConstant(0.0);
			modeParams.setMarginalUtilityOfDistance(0.0);
			modeParams.setMarginalUtilityOfTraveling(-1.0);
			modeParams.setMonetaryDistanceRate(0.0);
		}

		config.planCalcScore().setPerforming_utils_hr(0.0);
		config.planCalcScore().setUtilityOfLineSwitch(0.0);
		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(-1.0);
		config.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(-1.0);

		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readURL(config.network().getInputFileURL(config.getContext()));
		new TransitScheduleReader(scenario).readURL(config.transit().getTransitScheduleFileURL(config.getContext()));

		// OPTIONALLY LOAD TRAVEL TIMES

		TravelTime travelTime = new FreeSpeedTravelTime();

		if (cmd.hasOption("travel-times-path")) {
			travelTime = RecordedTravelTime
					.readBinary(IOUtils.getInputStream(IOUtils.getFileUrl(cmd.getOptionStrict("travel-times-path"))));
		}

		final TravelTime fixedTravelTime = travelTime;

		// SET UP ROUTING INFRASTRUCTURE

		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.addOverridingModule(new OriginDestinationRouterModule(numberOfThreads, batchSize)) //
				.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						addTravelTimeBinding("car").toInstance(fixedTravelTime);
					}
				}).build();

		// PERFORM MATRIX ROUTING

		Set<String> modes = new HashSet<>(Arrays.asList("car", "pt", "bike", "walk"));

		if (cmd.hasOption("modes")) {
			modes.clear();

			for (String mode : cmd.getOptionStrict("modes").split(",")) {
				modes.add(mode.trim());
			}
		}

		double departureTime = cmd.getOption("departure-time").map(Time::parseTime).orElse(8.5 * 3600.0);
		String locationAttribute = cmd.getOption("location-attribute").orElse("id");

		Collection<Location> locations = new LocationReader(locationAttribute)
				.readFile(new File(cmd.getOptionStrict("locations-path")));

		OriginDestinationRouter router = injector.getInstance(OriginDestinationRouter.class);
		ModalTravelTimeMatrix matrix = router.run(locations, modes, departureTime);

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));

		writer.write("origin_id;destination_id;mode;travel_time\n");

		for (String mode : modes) {
			for (Location origin : locations) {
				for (Location destination : locations) {
					writer.write(String.join(";", new String[] { //
							origin.getId().toString(), //
							destination.getId().toString(), //
							mode, //
							String.valueOf(matrix.getValue(mode, origin, destination)) //
					}) + "\n");
				}
			}
		}

		writer.close();
	}
}
