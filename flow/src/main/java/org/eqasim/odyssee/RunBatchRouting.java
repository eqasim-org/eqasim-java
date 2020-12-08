package org.eqasim.odyssee;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.routing.PopulationRouter;
import org.eqasim.core.scenario.routing.PopulationRouterModule;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Injector;

public class RunBatchRouting {
	static public void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "input-path", "output-path") //
				.allowOptions("threads", "batch-size") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		config.strategy().clearStrategySettings();

		config.plans().setInputFile(null);
		config.facilities().setInputFile(null);
		config.transit().setVehiclesFile(null);

		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton("car"));

		Set<String> modes = new HashSet<>();

		{
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(cmd.getOptionStrict("input-path"))));
			String line = null;
			List<String> header = null;

			PopulationFactory factory = scenario.getPopulation().getFactory();

			while ((line = reader.readLine()) != null) {
				List<String> row = Arrays.asList(line.split(";"));

				if (header == null) {
					header = row;
				} else {
					String id = row.get(header.indexOf("id"));
					double endTime = Double.parseDouble(row.get(header.indexOf("departure_time")));

					String mode = row.get(header.indexOf("mode"));
					modes.add(mode);

					double originX = Double.parseDouble(row.get(header.indexOf("origin_x")));
					double originY = Double.parseDouble(row.get(header.indexOf("origin_y")));
					double destinationX = Double.parseDouble(row.get(header.indexOf("destination_x")));
					double destinationY = Double.parseDouble(row.get(header.indexOf("destination_y")));

					Coord originCoord = new Coord(originX, originY);
					Coord destinationCoord = new Coord(destinationX, destinationY);

					Link originLink = NetworkUtils.getNearestLink(roadNetwork, originCoord);
					Link destinationLink = NetworkUtils.getNearestLink(roadNetwork, destinationCoord);

					Person person = factory.createPerson(Id.createPersonId(id));
					scenario.getPopulation().addPerson(person);

					Plan plan = factory.createPlan();
					person.addPlan(plan);

					Activity originActivity = PopulationUtils.createActivityFromCoordAndLinkId("generic", originCoord,
							originLink.getId());
					originActivity.setEndTime(endTime);
					plan.addActivity(originActivity);

					Leg leg = PopulationUtils.createLeg(mode);
					plan.addLeg(leg);

					Activity destinationActivity = PopulationUtils.createActivityFromCoordAndLinkId("generic",
							destinationCoord, destinationLink.getId());
					plan.addActivity(destinationActivity);

				}
			}

			reader.close();
		}

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.addOverridingModule(new PopulationRouterModule(numberOfThreads, batchSize, true, modes)) //
				.build();

		PopulationRouter populationRouter = injector.getInstance(PopulationRouter.class);
		populationRouter.run(scenario.getPopulation());

		{
			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));

			writer.write(String.join(";", new String[] { //
					"id", //
					"travel_time" //
			}) + "\n");

			for (Person person : scenario.getPopulation().getPersons().values()) {
				Plan plan = person.getSelectedPlan();

				double travelTime = 0.0;

				for (Leg leg : TripStructureUtils.getLegs(plan)) {
					travelTime += leg.getTravelTime().seconds();
				}

				writer.write(String.join(";", new String[] { //
						String.valueOf(person.getId()), //
						String.valueOf(travelTime) //
				}) + "\n");
			}

			writer.close();
		}
	}
}
