package org.eqasim.san_francisco.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

public class CarTravelTimes {

	public static void main(String[] args) throws IOException, ConfigurationException, InterruptedException {

		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "events-path", "input-path", "traveltime-output-path",
						"crossing-penalty") //
				.build();

		Network fullNetwork = NetworkUtils.createNetwork();
		Network network = NetworkUtils.createNetwork();

		new MatsimNetworkReader(fullNetwork).readFile(cmd.getOptionStrict("network-path"));
		new TransportModeNetworkFilter(fullNetwork).filter(network, Collections.singleton("car"));

		int numberOfBins = 1 + (int) Math.floor((30.0 * 3600.0 - 0.0) / 900);
		SerializableTravelTime.Listener travelTimeListener = new SerializableTravelTime.Listener(network, 0.0,
				30 * 3600, 900, numberOfBins);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(travelTimeListener);
		new MatsimEventsReader(eventsManager).readFile(cmd.getOptionStrict("events-path"));

		EqasimFreeSpeedTravelTime freeSpeedTravelTime = new EqasimFreeSpeedTravelTime(Double.parseDouble(cmd.getOptionStrict("crossing-penalty")));
		SerializableTravelTime congestedTravelTime = new SerializableTravelTime(0.0, 30.0 * 3600.0, 900, numberOfBins,
				travelTimeListener.getData(Double.parseDouble(cmd.getOptionStrict("crossing-penalty"))),
				freeSpeedTravelTime);

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("traveltime-output-path"))));

		writer.write(String.join(",", new String[] { //
				"trip_id", "freespeed_travel_time", "congested_travel_time", "distance" }) + "\n");
		writer.flush();

		List<Thread> threads = new LinkedList<>();
		List<Task> tasks = new LinkedList<>();

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(cmd.getOptionStrict("input-path"))));
		reader.readLine();
		String s = reader.readLine();

		while (s != null) {
			String[] variables = s.split(",");
			Coord startCoord = CoordUtils.createCoord(Double.parseDouble(variables[1]),
					Double.parseDouble(variables[2]));
			Coord endCoord = CoordUtils.createCoord(Double.parseDouble(variables[4]), Double.parseDouble(variables[5]));
			double departureTime = Double.parseDouble(variables[7]);
			int tripId = Integer.parseInt(variables[0]);
			Task task = new Task(startCoord, endCoord, departureTime, tripId);
			tasks.add(task);
			s = reader.readLine();
		}

		reader.close();

		long totalNumberOfTasks = tasks.size();
		AtomicLong processedNumberOfTasks = new AtomicLong(0);

		for (int k = 0; k < Runtime.getRuntime().availableProcessors(); k++) {
			Thread thread = new Thread(() -> {
				List<Task> localTasks = new LinkedList<>();
				LeastCostPathCalculator freeSpeedRouter = new AStarLandmarksFactory(
						Runtime.getRuntime().availableProcessors()).createPathCalculator(network,
								new OnlyTimeDependentTravelDisutility(freeSpeedTravelTime), freeSpeedTravelTime);

				LeastCostPathCalculator congestedRouter = new AStarLandmarksFactory(
						Runtime.getRuntime().availableProcessors()).createPathCalculator(network,
								new OnlyTimeDependentTravelDisutility(congestedTravelTime), congestedTravelTime);

				while (true) {
					localTasks.clear();

					synchronized (tasks) {
						while (tasks.size() > 0 && localTasks.size() < 10) {
							localTasks.add(tasks.remove(0));
						}

					}

					for (Task task : localTasks) {

						Link startLink = NetworkUtils.getNearestLinkExactly(network, task.startCoord);
						Link endLink = NetworkUtils.getNearestLinkExactly(network, task.endCoord);

						double routeTime = task.departureTime;

						Path freespeedPath = freeSpeedRouter.calcLeastCostPath(startLink.getFromNode(),
								endLink.getToNode(), routeTime, null, null);
						Path congestedPath = congestedRouter.calcLeastCostPath(startLink.getFromNode(),
								endLink.getToNode(), routeTime, null, null);

						try {
							synchronized (writer) {
								double distance = 0.0;
								for (Link link : congestedPath.links) {
									distance += link.getLength();
								}
								writer.write(String.join(",", new String[] { //
										String.valueOf(task.tripId), String.valueOf(freespeedPath.travelTime), //
										String.valueOf(congestedPath.travelTime), String.valueOf(distance)//
								}) + "\n");
								writer.flush();
							}
						} catch (IOException e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
					}

					if (localTasks.size() == 0) {
						return;
					} else {
						processedNumberOfTasks.addAndGet(localTasks.size());
						System.out.println(String.format("%d / %d (%.2f%%)", processedNumberOfTasks.get(),
								totalNumberOfTasks, 100.0 * processedNumberOfTasks.get() / totalNumberOfTasks));
					}
				}
			});

			thread.start();
			threads.add(thread);
		}

		for (Thread thread : threads) {
			thread.join();
		}
		writer.close();

	}

	static public class Task {
		public final Coord startCoord;
		public final Coord endCoord;
		public final double departureTime;
		public final int tripId;

		public Task(Coord startCoord, Coord endCoord, double departureTime, int tripId) {
			this.startCoord = startCoord;
			this.endCoord = endCoord;
			this.departureTime = departureTime;
			this.tripId = tripId;
		}
	}

}
