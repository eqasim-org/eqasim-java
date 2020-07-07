package org.eqasim.ile_de_france.grand_paris;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class AdjustSchedule {
	static public void main(String[] args) throws ConfigurationException, NumberFormatException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("schedule-path", "network-path", "facilities-path", "routes-path", "departures-path",
						"output-schedule-path", "output-network-path") //
				.build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));
		TransitSchedule schedule = scenario.getTransitSchedule();

		new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("network-path"));
		Network network = scenario.getNetwork();

		Map<Id<TransitStopFacility>, TransitStopFacility> facilities = new HashMap<>();

		// Find 14

		TransitLine line14 = schedule.getTransitLines().get(Id.create("100110014:14", TransitLine.class));

		Set<TransitStopFacility> stopsForOlympiades = new HashSet<>();
		Set<TransitStopFacility> stopsForSaintLazare = new HashSet<>();

		for (TransitRoute transitRoute : line14.getRoutes().values()) {
			for (TransitRouteStop stop : transitRoute.getStops()) {
				if (stop.getStopFacility().getName().equals("Olympiades")) {
					stopsForOlympiades.add(stop.getStopFacility());
					facilities.put(Id.create("GPE:GA90", TransitStopFacility.class), stop.getStopFacility());
				} else if (stop.getStopFacility().getName().equals("Saint-Lazare")) {
					stopsForSaintLazare.add(stop.getStopFacility());
					facilities.put(Id.create("GPE:GA98", TransitStopFacility.class), stop.getStopFacility());
				}
			}
		}

		if (stopsForOlympiades.size() != 1 || stopsForSaintLazare.size() != 1) {
			throw new IllegalStateException();
		}

		{ // Set up stop facilities
			String facilitiesPath = cmd.getOptionStrict("facilities-path");
			String line = null;
			List<String> header = null;

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(facilitiesPath))));

			while ((line = reader.readLine()) != null) {
				List<String> row = Arrays.asList(line.split(","));

				if (header == null) {
					header = row;
				} else {
					String code = row.get(header.indexOf("code"));

					if (code.equals("GA90") || code.equals("GA98")) {
						continue;
					}

					Coord coord = new Coord(Double.parseDouble(row.get(header.indexOf("x"))),
							Double.parseDouble(row.get(header.indexOf("y"))));

					TransitStopFacility facility = schedule.getFactory().createTransitStopFacility( //
							Id.create("GPE:" + row.get(header.indexOf("code")), TransitStopFacility.class), //
							coord, //
							false //
					);
					facility.setName(row.get(header.indexOf("name")));
					schedule.addStopFacility(facility);
					facilities.put(facility.getId(), facility);

					Node stopNode = network.getFactory().createNode( //
							Id.createNodeId("GPE:" + row.get(header.indexOf("code"))), //
							facility.getCoord());
					network.addNode(stopNode);

					Link stopLink = network.getFactory().createLink( //
							Id.createLinkId("GPE:" + row.get(header.indexOf("code"))), stopNode, stopNode);

					network.addLink(stopLink);

					facility.setLinkId(stopLink.getId());
				}
			}

			reader.close();
		}

		Map<String, List<Id<TransitStopFacility>>> routes = new HashMap<>();

		{ // Set up lines
			String routesPath = cmd.getOptionStrict("routes-path");
			String line = null;
			List<String> header = null;

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(routesPath))));

			while ((line = reader.readLine()) != null) {
				List<String> row = Arrays.asList(line.split(","));

				if (header == null) {
					header = row;
				} else {
					String transitLine = row.get(header.indexOf("line"));
					Id<TransitStopFacility> stopId = Id.create("GPE:" + row.get(header.indexOf("stop")),
							TransitStopFacility.class);

					routes.computeIfAbsent(transitLine, n -> new LinkedList<>());
					routes.get(transitLine).add(stopId);
				}
			}

			reader.close();
		}

		Map<String, List<Double>> departures = new HashMap<>();

		{ // Set up departures
			String routesPath = cmd.getOptionStrict("departures-path");
			String line = null;
			List<String> header = null;

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(new File(routesPath))));

			while ((line = reader.readLine()) != null) {
				List<String> row = Arrays.asList(line.split(","));

				if (header == null) {
					header = row;
				} else {
					String transitLine = row.get(header.indexOf("line"));
					double departureTime = Double.parseDouble(row.get(header.indexOf("time")));

					departures.computeIfAbsent(transitLine, n -> new LinkedList<>());
					departures.get(transitLine).add(departureTime);
				}
			}

			reader.close();
		}

		// DONE LOADING THE DATA

		final double SPEED_KM_H = 40.0;

		for (String line : Arrays.asList("L14O", "L14L", "L15", "L16", "L17", "L18")) {
			TransitLine transitLine = schedule.getFactory()
					.createTransitLine(Id.create("GPE:" + line, TransitLine.class));
			schedule.addTransitLine(transitLine);

			List<TransitRouteStop> forwardStops = new LinkedList<>();
			double time = 0.0;

			for (int i = 0; i < routes.get(line).size(); i++) {
				TransitStopFacility stop = facilities.get(routes.get(line).get(i));

				if (forwardStops.size() > 0) {
					TransitRouteStop previousStop = forwardStops.get(forwardStops.size() - 1);

					Coord previousCoord = previousStop.getStopFacility().getCoord();
					Coord currentCoord = stop.getCoord();

					double euclideanDistance_km = CoordUtils.calcEuclideanDistance(previousCoord, currentCoord) * 1e-3;
					time += 3600 * euclideanDistance_km / SPEED_KM_H;
					time = Math.ceil(time / 60) * 60.0;
				}

				forwardStops.add(schedule.getFactory().createTransitRouteStop(stop, time, time));
			}

			List<TransitRouteStop> backwardStops = new LinkedList<>();
			time = 0.0;

			for (int i = routes.get(line).size() - 1; i >= 0; i--) {
				TransitStopFacility stop = facilities.get(routes.get(line).get(i));

				if (backwardStops.size() > 0) {
					TransitRouteStop previousStop = backwardStops.get(backwardStops.size() - 1);

					Coord previousCoord = previousStop.getStopFacility().getCoord();
					Coord currentCoord = stop.getCoord();

					double euclideanDistance_km = CoordUtils.calcEuclideanDistance(previousCoord, currentCoord) * 1e-3;
					time += 3600 * euclideanDistance_km / SPEED_KM_H;
					time = Math.ceil(time / 60) * 60.0;
				}

				backwardStops.add(schedule.getFactory().createTransitRouteStop(stop, time, time));
			}

			List<Id<Link>> forwardLinkIds = new LinkedList<>();
			List<Id<Link>> backwardLinkIds = new LinkedList<>();

			for (int i = 0; i < forwardStops.size() - 1; i++) {
				{
					Id<Link> previousLinkId = forwardStops.get(i).getStopFacility().getLinkId();
					Id<Link> followingLinkId = forwardStops.get(i + 1).getStopFacility().getLinkId();

					if (i == 0) {
						forwardLinkIds.add(previousLinkId);
					}

					Link forwardLink = network.getFactory().createLink( //
							Id.createLinkId("GPE:" + line + ":forward:" + i), //
							network.getLinks().get(previousLinkId).getToNode(), //
							network.getLinks().get(followingLinkId).getFromNode());

					network.addLink(forwardLink);

					forwardLinkIds.add(forwardLink.getId());
					forwardLinkIds.add(followingLinkId);
				}

				{
					Id<Link> previousLinkId = backwardStops.get(i).getStopFacility().getLinkId();
					Id<Link> followingLinkId = backwardStops.get(i + 1).getStopFacility().getLinkId();

					if (i == 0) {
						backwardLinkIds.add(previousLinkId);
					}

					Link backwardLink = network.getFactory().createLink( //
							Id.createLinkId("GPE:" + line + ":backward:" + i), //
							network.getLinks().get(previousLinkId).getToNode(), //
							network.getLinks().get(followingLinkId).getFromNode());

					network.addLink(backwardLink);

					backwardLinkIds.add(backwardLink.getId());
					backwardLinkIds.add(followingLinkId);
				}
			}

			LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

			NetworkRoute forwardNetworkRoute = (NetworkRoute) routeFactory.createRoute(forwardLinkIds.get(0),
					forwardLinkIds.get(forwardLinkIds.size() - 1));
			forwardNetworkRoute.setLinkIds(forwardLinkIds.get(0), forwardLinkIds.subList(1, forwardLinkIds.size() - 1),
					forwardLinkIds.get(forwardLinkIds.size() - 1));

			NetworkRoute backwardNetworkRoute = (NetworkRoute) routeFactory.createRoute(backwardLinkIds.get(0),
					backwardLinkIds.get(backwardLinkIds.size() - 1));
			backwardNetworkRoute.setLinkIds(backwardLinkIds.get(0),
					backwardLinkIds.subList(1, backwardLinkIds.size() - 1),
					backwardLinkIds.get(backwardLinkIds.size() - 1));

			TransitRoute forwardRoute = schedule.getFactory().createTransitRoute(
					Id.create("GPE:" + line + ":forward", TransitRoute.class), forwardNetworkRoute, forwardStops,
					"subway");

			TransitRoute backwardRoute = schedule.getFactory().createTransitRoute(
					Id.create("GPE:" + line + ":backward", TransitRoute.class), backwardNetworkRoute, backwardStops,
					"subway");

			transitLine.addRoute(forwardRoute);
			transitLine.addRoute(backwardRoute);

			String departuresLine = line.startsWith("L14") ? "L14" : line;

			for (double departureTime : departures.get(departuresLine)) {
				Id<Departure> forwardDepartureId = Id
						.create("GPE:" + line + ":forward:" + Time.writeTime(departureTime), Departure.class);
				Departure forwardDeparture = schedule.getFactory().createDeparture(forwardDepartureId, departureTime);
				forwardRoute.addDeparture(forwardDeparture);

				Id<Departure> backwardDepartureId = Id
						.create("GPE:" + line + ":backward:" + Time.writeTime(departureTime), Departure.class);
				Departure backwardDeparture = schedule.getFactory().createDeparture(backwardDepartureId, departureTime);
				backwardRoute.addDeparture(backwardDeparture);
			}
		}

		// NOW MERGE GPE:L14L and GPE:L14O ONTO EXISTING 14

		TransitStopFacility stopOlympiades = stopsForOlympiades.iterator().next();
		TransitStopFacility stopSaintLazare = stopsForSaintLazare.iterator().next();

		TransitRoute originalForwardRoute = null;
		TransitRoute originalBackwardRoute = null;

		for (TransitRoute route : line14.getRoutes().values()) {
			if (route.getStops().size() == 9) {
				if (route.getStops().get(0).getStopFacility().equals(stopSaintLazare)) {
					originalForwardRoute = route;
				}

				if (route.getStops().get(0).getStopFacility().equals(stopOlympiades)) {
					originalBackwardRoute = route;
				}
			}
		}

		schedule.removeTransitLine(line14);

		TransitLine newLineSaintLazare = schedule.getTransitLines().get(Id.create("GPE:L14L", TransitLine.class));
		TransitLine newLineOlympiades = schedule.getTransitLines().get(Id.create("GPE:L14O", TransitLine.class));

		schedule.removeTransitLine(newLineSaintLazare);
		schedule.removeTransitLine(newLineOlympiades);

		TransitRoute forwardRouteSaintLazare = newLineSaintLazare.getRoutes()
				.get(Id.create("GPE:L14L:forward", TransitRoute.class));
		TransitRoute backwardRouteSaintLazare = newLineSaintLazare.getRoutes()
				.get(Id.create("GPE:L14L:backward", TransitRoute.class));

		TransitRoute forwardRouteOlympiades = newLineOlympiades.getRoutes()
				.get(Id.create("GPE:L14O:forward", TransitRoute.class));
		TransitRoute backwardRouteOlympiades = newLineOlympiades.getRoutes()
				.get(Id.create("GPE:L14O:backward", TransitRoute.class));

		// Construct new line

		line14 = schedule.getFactory().createTransitLine(Id.create("GPE:L14", TransitLine.class));
		schedule.addTransitLine(line14);

		// ... forward direction from Saint Lazare to Olympiades

		List<TransitRouteStop> duplicatedForwardStops = new LinkedList<>();
		duplicatedForwardStops
				.addAll(forwardRouteSaintLazare.getStops().subList(0, forwardRouteSaintLazare.getStops().size() - 1));
		duplicatedForwardStops.addAll(originalForwardRoute.getStops());
		duplicatedForwardStops
				.addAll(forwardRouteOlympiades.getStops().subList(1, forwardRouteOlympiades.getStops().size()));

		List<TransitRouteStop> forwardStops = new LinkedList<>();
		{
			double time = 0.0;
			TransitRouteStop previousStop = null;

			for (TransitRouteStop stop : duplicatedForwardStops) {
				if (previousStop != null) {
					Coord previousCoord = previousStop.getStopFacility().getCoord();
					Coord currentCoord = stop.getStopFacility().getCoord();

					double euclideanDistance_km = CoordUtils.calcEuclideanDistance(previousCoord, currentCoord) * 1e-3;
					time += 3600 * euclideanDistance_km / SPEED_KM_H;
					time = Math.ceil(time / 60) * 60.0;
				}

				forwardStops.add(schedule.getFactory().createTransitRouteStop(stop.getStopFacility(), time, time));
				previousStop = stop;
			}
		}

		List<Id<Link>> forwardLinkIds = new LinkedList<>();
		forwardLinkIds.addAll(forwardRouteSaintLazare.getRoute().getLinkIds());
		forwardLinkIds.add(originalForwardRoute.getRoute().getStartLinkId());
		forwardLinkIds.addAll(originalForwardRoute.getRoute().getLinkIds());
		forwardLinkIds.add(originalForwardRoute.getRoute().getEndLinkId());
		forwardLinkIds.addAll(forwardRouteOlympiades.getRoute().getLinkIds());

		NetworkRoute forwardRoute = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(
				forwardRouteSaintLazare.getRoute().getStartLinkId(), forwardRouteOlympiades.getRoute().getEndLinkId());
		forwardRoute.setLinkIds(forwardRoute.getStartLinkId(), forwardLinkIds, forwardRoute.getEndLinkId());

		TransitRoute newForwardRoute = schedule.getFactory().createTransitRoute( //
				Id.create("GPE:L14:forward", TransitRoute.class), //
				forwardRoute, forwardStops, "subway");
		line14.addRoute(newForwardRoute);

		// ... backward direction from Olympiades to Saint Lazare

		List<TransitRouteStop> duplicatedBackwardStops = new LinkedList<>();
		duplicatedBackwardStops
				.addAll(backwardRouteOlympiades.getStops().subList(0, backwardRouteOlympiades.getStops().size() - 1));
		duplicatedBackwardStops.addAll(originalBackwardRoute.getStops());
		duplicatedBackwardStops
				.addAll(backwardRouteSaintLazare.getStops().subList(1, backwardRouteSaintLazare.getStops().size()));

		List<TransitRouteStop> backwardStops = new LinkedList<>();
		{
			double time = 0.0;
			TransitRouteStop previousStop = null;

			for (TransitRouteStop stop : duplicatedBackwardStops) {
				if (previousStop != null) {
					Coord previousCoord = previousStop.getStopFacility().getCoord();
					Coord currentCoord = stop.getStopFacility().getCoord();

					double euclideanDistance_km = CoordUtils.calcEuclideanDistance(previousCoord, currentCoord) * 1e-3;
					time += 3600 * euclideanDistance_km / SPEED_KM_H;
					time = Math.ceil(time / 60) * 60.0;
				}

				backwardStops.add(schedule.getFactory().createTransitRouteStop(stop.getStopFacility(), time, time));
				previousStop = stop;
			}
		}

		List<Id<Link>> backwardLinkIds = new LinkedList<>();
		backwardLinkIds.addAll(backwardRouteOlympiades.getRoute().getLinkIds());
		backwardLinkIds.add(originalBackwardRoute.getRoute().getStartLinkId());
		backwardLinkIds.addAll(originalBackwardRoute.getRoute().getLinkIds());
		backwardLinkIds.add(originalBackwardRoute.getRoute().getEndLinkId());
		backwardLinkIds.addAll(backwardRouteSaintLazare.getRoute().getLinkIds());

		NetworkRoute backwardRoute = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(
				backwardRouteOlympiades.getRoute().getStartLinkId(),
				backwardRouteSaintLazare.getRoute().getEndLinkId());
		backwardRoute.setLinkIds(backwardRoute.getStartLinkId(), backwardLinkIds, backwardRoute.getEndLinkId());

		TransitRoute newBackwardRoute = schedule.getFactory().createTransitRoute( //
				Id.create("GPE:L14:backward", TransitRoute.class), //
				backwardRoute, backwardStops, "subway");
		line14.addRoute(newBackwardRoute);

		// Add departures

		for (Departure departure : forwardRouteOlympiades.getDepartures().values()) {
			Id<Departure> forwardDepartureId = Id
					.create("GPE:L14:forward:" + Time.writeTime(departure.getDepartureTime()), Departure.class);

			Id<Departure> backwardDepartureId = Id
					.create("GPE:L14:backward:" + Time.writeTime(departure.getDepartureTime()), Departure.class);

			newForwardRoute.addDeparture(
					schedule.getFactory().createDeparture(forwardDepartureId, departure.getDepartureTime()));
			newBackwardRoute.addDeparture(
					schedule.getFactory().createDeparture(backwardDepartureId, departure.getDepartureTime()));
		}

		new TransitScheduleWriter(schedule).writeFile(cmd.getOptionStrict("output-schedule-path"));
		new NetworkWriter(network).write(cmd.getOptionStrict("output-network-path"));
	}
}
