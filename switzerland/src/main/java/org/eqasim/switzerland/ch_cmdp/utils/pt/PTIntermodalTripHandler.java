package org.eqasim.switzerland.ch_cmdp.utils.pt;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

public class PTIntermodalTripHandler
		implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler, ActivityStartEventHandler, AutoCloseable {
	private static final String ACCESS = "access";
	private static final String EGRESS = "egress";

	private final Network network;
	private final TransitSchedule transitSchedule;
	private final Set<String> intermodalModes;
	private final Set<Id<Vehicle>> transitVehicleIds;

	private final Map<Id<Vehicle>, StopContext> vehicleStops = new HashMap<>();
	private final Map<Id<Person>, IntermodalLeg> activeIntermodalLegs = new HashMap<>();
	private final Map<Id<Person>, IntermodalLeg> pendingAccessLegs = new HashMap<>();
	private final Map<Id<Person>, StopContext> pendingPtAlightings = new HashMap<>();

	private BufferedWriter writer;

	public PTIntermodalTripHandler(Network network, TransitSchedule transitSchedule, Set<String> intermodalModes,
			Set<Id<Vehicle>> transitVehicleIds) {
		this.network = network;
		this.transitSchedule = transitSchedule;
		this.intermodalModes = intermodalModes;
		this.transitVehicleIds = transitVehicleIds;
	}

	public void open(String path) throws IOException {
		boolean isGzipped = path.endsWith(".gz");
		OutputStream outputStream = isGzipped ? new GZIPOutputStream(new FileOutputStream(path))
				: new FileOutputStream(path);
		writer = new BufferedWriter(new OutputStreamWriter(outputStream));
		writer.write(
				"person_id;direction;intermodal_mode;origin_x;origin_y;destination_x;destination_y;transfer_stop_x;transfer_stop_y;transfer_stop_id;pt_vehicle_id;time\n");
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (!transitVehicleIds.contains(event.getVehicleId())) {
			return;
		}

		TransitStopFacility stop = transitSchedule.getFacilities().get(event.getFacilityId());
		if (stop != null) {
			vehicleStops.put(event.getVehicleId(),
					new StopContext(event.getTime(), event.getVehicleId(), event.getFacilityId(), stop.getCoord()));
		}
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		vehicleStops.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String mode = event.getLegMode();
		if (!intermodalModes.contains(mode)) {
			return;
		}

		// If the person has just left PT, the next non-walk intermodal leg is the
		// egress vehicle leg. Otherwise, it is a potential access vehicle leg until
		// the person actually boards a PT vehicle.
		StopContext alighting = pendingPtAlightings.remove(event.getPersonId());
		activeIntermodalLegs.put(event.getPersonId(),
				new IntermodalLeg(mode, getLinkCoord(event.getLinkId()), alighting));
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		IntermodalLeg leg = activeIntermodalLegs.remove(event.getPersonId());
		if (leg == null || !leg.mode.equals(event.getLegMode())) {
			return;
		}

		leg.destination = getLinkCoord(event.getLinkId());
		if (leg.ptContext != null) {
			writeRecord(event.getPersonId(), EGRESS, leg, leg.ptContext);
		} else {
			pendingAccessLegs.put(event.getPersonId(), leg);
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		StopContext stopContext = vehicleStops.get(event.getVehicleId());
		if (stopContext == null) {
			return;
		}

		IntermodalLeg accessLeg = pendingAccessLegs.remove(event.getPersonId());
		if (accessLeg != null) {
			writeRecord(event.getPersonId(), ACCESS, accessLeg, stopContext.withTime(event.getTime()));
		}

		pendingPtAlightings.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		StopContext stopContext = vehicleStops.get(event.getVehicleId());
		if (stopContext != null) {
			pendingPtAlightings.put(event.getPersonId(), stopContext.withTime(event.getTime()));
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!TripStructureUtils.isStageActivityType(event.getActType())) {
			activeIntermodalLegs.remove(event.getPersonId());
			pendingAccessLegs.remove(event.getPersonId());
			pendingPtAlightings.remove(event.getPersonId());
		}
	}

	@Override
	public void reset(int iteration) {
		vehicleStops.clear();
		activeIntermodalLegs.clear();
		pendingAccessLegs.clear();
		pendingPtAlightings.clear();
	}

	@Override
	public void close() throws IOException {
		if (writer != null) {
			writer.close();
			writer = null;
		}
	}

	private void writeRecord(Id<Person> personId, String direction, IntermodalLeg leg, StopContext stopContext) {
		if (writer == null || leg.origin == null || leg.destination == null || stopContext.coord == null) {
			return;
		}

		try {
			writer.write(personId.toString());
			writer.write(';');
			writer.write(direction);
			writer.write(';');
			writer.write(leg.mode);
			writer.write(';');
			writeCoord(leg.origin);
			writer.write(';');
			writeCoord(leg.destination);
			writer.write(';');
			writeCoord(stopContext.coord);
			writer.write(';');
			writer.write(stopContext.stopId.toString());
			writer.write(';');
			writer.write(stopContext.vehicleId.toString());
			writer.write(';');
			writer.write(Double.toString(stopContext.time));
			writer.write('\n');
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Coord getLinkCoord(Id<Link> linkId) {
		Link link = network.getLinks().get(linkId);
		return link == null ? null : link.getCoord();
	}

	private void writeCoord(Coord coord) throws IOException {
		writer.write(Double.toString(coord.getX()));
		writer.write(';');
		writer.write(Double.toString(coord.getY()));
	}

	private record StopContext(double time, Id<Vehicle> vehicleId, Id<TransitStopFacility> stopId, Coord coord) {
		StopContext withTime(double time) {
			return new StopContext(time, vehicleId, stopId, coord);
		}
	}

	private static class IntermodalLeg {
		private final String mode;
		private final Coord origin;
		private final StopContext ptContext;
		private Coord destination;

		private IntermodalLeg(String mode, Coord origin, StopContext ptContext) {
			this.mode = mode;
			this.origin = origin;
			this.ptContext = ptContext;
		}
	}
}
