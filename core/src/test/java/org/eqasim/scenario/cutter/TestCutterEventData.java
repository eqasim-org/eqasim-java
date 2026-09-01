package org.eqasim.scenario.cutter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eqasim.core.scenario.cutter.CutterEventData;
import org.eqasim.core.scenario.cutter.extent.LinkRegionClassifier;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.timing.LinkTimingData;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

public class TestCutterEventData {
	@Test
	public void readsTravelTimesAndCrossingTimesInOnePass() throws Exception {
		Network network = NetworkUtils.createNetwork();
		Node outsideNode = network.getFactory().createNode(Id.createNodeId("outside"), new Coord(0.0, 0.0));
		Node insideNode = network.getFactory().createNode(Id.createNodeId("inside"), new Coord(1.0, 0.0));
		network.addNode(outsideNode);
		network.addNode(insideNode);

		Link link = network.getFactory().createLink(Id.createLinkId("crossing"), outsideNode, insideNode);
		link.setAllowedModes(Set.of("car"));
		network.addLink(link);

		ScenarioExtent extent = new ScenarioExtent() {
			@Override
			public boolean isInside(Coord coord) {
				return coord.getX() > 0.0;
			}

			@Override
			public List<Coord> computeEuclideanIntersections(Coord from, Coord to) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Coord getInteriorPoint() {
				throw new UnsupportedOperationException();
			}
		};
		LinkRegionClassifier.classify(network, extent);

		Id<Person> personId = Id.createPersonId("person");
		Id<Vehicle> vehicleId = Id.createVehicleId("vehicle");
		Path eventsPath = Files.createTempFile("eqasim-cutter-events", ".xml");

		try {
			EventWriterXML writer = new EventWriterXML(eventsPath.toString());
			writer.handleEvent(new PersonDepartureEvent(0.0, personId, link.getId(), "car", "car"));
			writer.handleEvent(new VehicleEntersTrafficEvent(0.0, personId, link.getId(), vehicleId, "car", 0.0));
			writer.handleEvent(new LinkEnterEvent(1.0, vehicleId, link.getId()));
			writer.handleEvent(new LinkLeaveEvent(11.0, vehicleId, link.getId()));
			writer.closeFile();

			Config config = ConfigUtils.createConfig();
			CutterEventData data = CutterEventData.read(Optional.of(eventsPath.toString()),
					new RoadNetwork(network, Set.of("car")), network, config);

			Assert.assertTrue(data.getTravelTime().isPresent());
			Assert.assertEquals(10.0, data.getTravelTime().get().getLinkTravelTime(link, 1.0, null, null), 1e-3);

			Optional<LinkTimingData> timing = data.getLinkTimingRegistry().getTimingData(personId, 0, link.getId());
			Assert.assertTrue(timing.isPresent());
			Assert.assertEquals(1.0, timing.get().enterTime, 1e-3);
			Assert.assertEquals(11.0, timing.get().leaveTime, 1e-3);
		} finally {
			Files.deleteIfExists(eventsPath);
		}
	}
}
