package org.eqasim.core.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.components.transit.events.PublicTransitEventMapper;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

public class ExportStopTraversals {
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("output-path", "schedule-path") //
				.allowOptions("extent-path", "events-path", "population-path") //
				.build();

		Verify.verify(cmd.hasOption("events-path") ^ cmd.hasOption("population-path"));

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));

		ScenarioExtent extent = null;
		if (cmd.hasOption("extent-path")) {
			extent = new ShapeScenarioExtent.Builder(new File(cmd.getOptionStrict("extent-path")), Optional.empty(),
					Optional.empty()).build();
		}

		List<IndividualRoute> routes = new LinkedList<>();
		if (cmd.hasOption("population-path")) {
			new PopulationReader(scenario).readFile(cmd.getOptionStrict("population-path"));

			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan())) {
					if (leg.getRoute() instanceof TransitPassengerRoute route) {
						routes.add(new IndividualRoute(route.getLineId(), route.getRouteId(), route.getAccessStopId(),
								route.getEgressStopId(), person.getId(), route.getBoardingTime().seconds()));
					}
				}
			}
		} else {
			EventsManager eventsManager = EventsUtils.createEventsManager();

			eventsManager.addHandler(new GenericEventHandler() {
				@Override
				public void handleEvent(GenericEvent event) {
					if (event instanceof PublicTransitEvent pt) {
						routes.add(new IndividualRoute(pt.getTransitLineId(), pt.getTransitRouteId(),
								pt.getAccessStopId(), pt.getEgressStopId(), pt.getPersonId(),
								pt.getVehicleDepartureTime()));
					}
				}
			});

			MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
			reader.addCustomEventMapper(PublicTransitEvent.TYPE, new PublicTransitEventMapper());
			reader.readFile(cmd.getOptionStrict("events-path"));
		}

		BufferedWriter writer = IOUtils.getBufferedWriter(cmd.getOptionStrict("output-path"));
		int routeIndex = 0;

		writer.write(String.join(";", Arrays.asList( //
				"route_index", "person_id", "stop_id", "area_id", "is_access", "is_egress", "transit_mode",
				"arrival_time", "departure_time")) + "\n");

		for (IndividualRoute route : routes) {
			TransitLine transitLine = scenario.getTransitSchedule().getTransitLines().get(route.transitLineId);
			TransitRoute transitRoute = transitLine.getRoutes().get(route.transitRouteId);

			TransitStopFacility accessFacility = scenario.getTransitSchedule().getFacilities().get(route.accessStopId);
			TransitRouteStop accessStop = transitRoute.getStop(accessFacility);
			int accessIndex = transitRoute.getStops().indexOf(accessStop);

			TransitStopFacility egressFacility = scenario.getTransitSchedule().getFacilities().get(route.egressStopId);
			TransitRouteStop egresssStop = transitRoute.getStop(egressFacility);
			int egressIndex = transitRoute.getStops().indexOf(egresssStop);

			double accessOffset = accessStop.getDepartureOffset().seconds();
			double minimumDelta = Double.POSITIVE_INFINITY;
			Departure bestDeparture = null;

			for (Departure departure : transitRoute.getDepartures().values()) {
				double vehicleDepartureTime = departure.getDepartureTime() + accessOffset;

				if (vehicleDepartureTime >= route.referenceTime || bestDeparture == null) {
					double delta = vehicleDepartureTime - route.referenceTime;

					if (delta < minimumDelta || (minimumDelta < 0.0 && delta >= 0.0)) {
						minimumDelta = delta;
						bestDeparture = departure;
					}
				}
			}

			Preconditions.checkState(bestDeparture != null);

			for (int index = accessIndex; index <= egressIndex; index++) {
				TransitRouteStop stop = transitRoute.getStops().get(index);
				TransitStopFacility stopFacility = stop.getStopFacility();

				double stopArrivalTime = bestDeparture.getDepartureTime() + stop.getArrivalOffset().seconds();
				double stopDepartureTime = bestDeparture.getDepartureTime() + stop.getDepartureOffset().seconds();

				if (extent == null || extent.isInside(stopFacility.getCoord())) {
					writer.write(String.join(";", Arrays.asList( //
							String.valueOf(routeIndex), //
							route.personId.toString(), //
							stop.getStopFacility().getId().toString(), //
							stop.getStopFacility().getStopAreaId().toString(), //
							String.valueOf(index == accessIndex), //
							String.valueOf(index == egressIndex), //
							transitRoute.getTransportMode(), //
							String.valueOf(stopArrivalTime),
							String.valueOf(stopDepartureTime) //
					)) + "\n");
				}
			}

			routeIndex++;
		}

		writer.close();
	}

	private record IndividualRoute(Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId,
			Id<TransitStopFacility> accessStopId, Id<TransitStopFacility> egressStopId, Id<Person> personId,
			double referenceTime) {
	}
}
