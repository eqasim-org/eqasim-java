package org.eqasim.core.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Verify;

public class ExportLinkTraversals {
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("events-path", "output-path") //
				.allowOptions("network-path", "extent-path", "modes") //
				.build();

		ScenarioExtent extent = null;
		Network network = null;

		if (cmd.hasOption("extent-path")) {
			Verify.verify(cmd.hasOption("network-path"), "Network path must be given");

			network = NetworkUtils.createNetwork();
			new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));

			extent = new ShapeScenarioExtent.Builder(new File(cmd.getOptionStrict("extent-path")), Optional.empty(),
					Optional.empty()).build();
		}

		Set<String> modes = new HashSet<>();
		if (cmd.hasOption("modes")) {
			modes = Arrays.asList(cmd.getOptionStrict("modes").split(",")).stream().map(String::trim)
					.collect(Collectors.toSet());
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(cmd.getOptionStrict("output-path"));

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new TraversalExporter(writer, extent, network, modes));
		new MatsimEventsReader(eventsManager).readFile(cmd.getOptionStrict("events-path"));

		writer.close();
	}

	private static class TraversalExporter implements VehicleEntersTrafficEventHandler,
			VehicleLeavesTrafficEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, ActivityEndEventHandler {
		private final BufferedWriter writer;
		private final ScenarioExtent extent;
		private final Network network;

		private final IdMap<Vehicle, Id<Person>> drivers = new IdMap<>(Vehicle.class);
		private final IdMap<Person, LinkEnterEvent> enterEvents = new IdMap<>(Person.class);
		private final Map<Id<Person>, Integer> tripIndex = new HashMap<>();
		private final Map<Id<Person>, Integer> legIndex = new HashMap<>();
		private final Set<String> modes;

		TraversalExporter(BufferedWriter writer, ScenarioExtent extent, Network network, Set<String> modes) {
			this.writer = writer;
			this.extent = extent;
			this.network = network;
			this.modes = modes;

			try {
				writer.write(String.join(";", Arrays.asList( //
						"person_id", "vehicle_id", "link_id", "enter_time", "leave_time", "trip_index", "leg_index"))
						+ "\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			if (checkMode(event.getNetworkMode())) {
				drivers.put(event.getVehicleId(), event.getPersonId());
			}
		}

		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
			if (drivers.remove(event.getVehicleId()) != null) {
				writeTraversal(event.getPersonId(), enterEvents.remove(event.getPersonId()), null);
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Id<Person> personId = drivers.get(event.getVehicleId());

			if (personId != null) {
				enterEvents.put(personId, event);
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Id<Person> personId = drivers.get(event.getVehicleId());

			if (personId != null) {
				// enter event can be null (first leave event on trip)
				writeTraversal(personId, enterEvents.remove(personId), event);
			}
		}

		private void writeTraversal(Id<Person> personId, LinkEnterEvent enterEvent, LinkLeaveEvent leaveEvent) {
			final Id<Vehicle> vehicleId;
			final Id<Link> linkId;

			if (enterEvent != null) {
				vehicleId = enterEvent.getVehicleId();
				linkId = enterEvent.getLinkId();
			} else if (leaveEvent != null) {
				vehicleId = leaveEvent.getVehicleId();
				linkId = leaveEvent.getLinkId();
			} else {
				return; // no actual driving happened
			}

			final double enterTime = enterEvent != null ? enterEvent.getTime() : Double.NaN;
			final double leaveTime = leaveEvent != null ? leaveEvent.getTime() : Double.NaN;

			if (extent != null) {
				Link link = network.getLinks().get(linkId);
				Coord fromCoord = link.getFromNode().getCoord();
				Coord toCoord = link.getToNode().getCoord();

				if (!extent.isInside(fromCoord) && !extent.isInside(toCoord)) {
					return; // ignore this one as it doesn't touch the requested extent
				}
			}

			int localTripIndex = tripIndex.getOrDefault(personId, 0);
			int localLegIndex = legIndex.getOrDefault(personId, 0);

			try {
				writer.write(String.join(";", new String[] { //
						personId.toString(), //
						vehicleId.toString(), //
						linkId.toString(), //
						String.valueOf(enterTime), //
						String.valueOf(leaveTime), //
						String.valueOf(localTripIndex), //
						String.valueOf(localLegIndex) //
				}) + "\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			Integer localLegIndex = legIndex.get(event.getPersonId());

			if (localLegIndex == null) {
				localLegIndex = 0;
			} else {
				localLegIndex = localLegIndex + 1;
			}

			Integer personTripIndex = tripIndex.get(event.getPersonId());

			if (!TripStructureUtils.isStageActivityType(event.getActType())) {
				if (personTripIndex == null) {
					personTripIndex = 0;
				} else {
					personTripIndex = personTripIndex + 1;
				}
			}

			tripIndex.put(event.getPersonId(), personTripIndex);
			legIndex.put(event.getPersonId(), localLegIndex);
		}

		private boolean checkMode(String mode) {
			return modes.isEmpty() || modes.contains(mode);
		}
	}
}
