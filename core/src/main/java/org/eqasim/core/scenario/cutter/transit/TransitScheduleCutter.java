package org.eqasim.core.scenario.cutter.transit;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.MinimalTransferTimes;
import org.matsim.pt.transitSchedule.api.MinimalTransferTimes.MinimalTransferTimesIterator;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TransitScheduleCutter {
	private final static Logger log = Logger.getLogger(TransitScheduleCutter.class);

	private final ScenarioExtent extent;
	private final StopSequenceCrossingPointFinder crossingPointFinder;

	public TransitScheduleCutter(ScenarioExtent extent, StopSequenceCrossingPointFinder crossingPointFinder) {
		this.extent = extent;
		this.crossingPointFinder = crossingPointFinder;
	}

	private List<TransitRouteStop> reduceStopSequence(List<TransitRouteStop> originalSequence) {
		List<StopSequenceCrossingPoint> crossingPoints = crossingPointFinder.findCrossingPoints(originalSequence);

		if (crossingPoints.size() == 0) {
			if (!extent.isInside(originalSequence.get(0).getStopFacility().getCoord())) {
				// The entire route is outside
				return Collections.emptyList();
			} else {
				// The entire route is inside
				return originalSequence;
			}
		} else {
			// There is at least one crossing point.
			StopSequenceCrossingPoint firstCrossingPoint = crossingPoints.get(0);
			StopSequenceCrossingPoint lastCrossingPoint = crossingPoints.get(crossingPoints.size() - 1);

			int firstIndex = firstCrossingPoint.isOutgoing ? 0 : firstCrossingPoint.index + 1;
			int lastIndex = lastCrossingPoint.isOutgoing ? lastCrossingPoint.index : originalSequence.size();

			return originalSequence.subList(firstIndex, lastIndex);
		}
	}

	private NetworkRoute reduceNetworkRoute(NetworkRoute originalRoute, Id<Link> startLinkId, Id<Link> endLinkId) {
		List<Id<Link>> originalLinkIds = new LinkedList<>(originalRoute.getLinkIds());
		originalLinkIds.add(0, originalRoute.getStartLinkId());
		originalLinkIds.add(originalRoute.getEndLinkId());

		int startIndex = -1;
		int endIndex = -1;

		for (int i = 0; i < originalLinkIds.size(); i++) {
			if (originalLinkIds.get(i).equals(startLinkId)) {
				startIndex = i;
				break;
			}
		}

		for (int i = originalLinkIds.size() - 1; i >= 0; i--) {
			if (originalLinkIds.get(i).equals(endLinkId)) {
				endIndex = i;
				break;
			}
		}

		if (startIndex == -1 || endIndex == -1) {
			throw new IllegalStateException();
		}

		Id<Link> reducedStartLinkId = originalLinkIds.get(startIndex);
		Id<Link> reducedEndLinkId = originalLinkIds.get(endIndex);
		List<Id<Link>> reducedLinkIds = startIndex == endIndex ? new LinkedList<>()
				: originalLinkIds.subList(startIndex + 1, endIndex);

		NetworkRoute reducedRoute = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(reducedStartLinkId,
				reducedEndLinkId);
		reducedRoute.setLinkIds(reducedStartLinkId, reducedLinkIds, reducedEndLinkId);

		return reducedRoute;
	}

	private TransitRoute reduceRoute(TransitRoute originalRoute, TransitScheduleFactory factory) {
		List<TransitRouteStop> originalStopSequence = originalRoute.getStops();
		List<TransitRouteStop> reducedStopSequence = reduceStopSequence(originalRoute.getStops());

		if (reducedStopSequence.size() < 2) {
			return null;
		} else {
			double departureOffset = reducedStopSequence.get(0).getDepartureOffset().seconds()
					- originalStopSequence.get(0).getDepartureOffset().seconds();

			Id<Link> routeStartLinkId = reducedStopSequence.get(0).getStopFacility().getLinkId();
			Id<Link> routeEndLinkId = reducedStopSequence.get(reducedStopSequence.size() - 1).getStopFacility()
					.getLinkId();
			NetworkRoute reducedNetworkRoute = reduceNetworkRoute(originalRoute.getRoute(), routeStartLinkId,
					routeEndLinkId);

			TransitRoute reducedRoute = factory.createTransitRoute(originalRoute.getId(), reducedNetworkRoute,
					reducedStopSequence, originalRoute.getTransportMode());

			for (Departure originalDeparture : originalRoute.getDepartures().values()) {
				Departure reducedDeparture = factory.createDeparture(originalDeparture.getId(),
						originalDeparture.getDepartureTime() - departureOffset);
				reducedDeparture.setVehicleId(originalDeparture.getVehicleId());
				reducedRoute.addDeparture(reducedDeparture);
			}

			return reducedRoute;
		}
	}

	private TransitLine reduceLine(TransitLine originalLine, TransitScheduleFactory factory) {
		TransitLine reducedLine = factory.createTransitLine(originalLine.getId());
		reducedLine.setName(originalLine.getName());

		for (TransitRoute originalRoute : originalLine.getRoutes().values()) {
			TransitRoute reducedRoute = reduceRoute(originalRoute, factory);

			if (reducedRoute != null) {
				reducedLine.addRoute(reducedRoute);
			}
		}

		return reducedLine.getRoutes().size() == 0 ? null : reducedLine;
	}

	public void run(TransitSchedule schedule) {
		log.info("Cutting transit schedule ...");
		ScheduleInfo originalInfo = getInfo(schedule);

		TransitScheduleFactory factory = schedule.getFactory();

		List<TransitLine> originalLines = new LinkedList<>(schedule.getTransitLines().values());
		originalLines.forEach(schedule::removeTransitLine);

		for (TransitLine originalLine : originalLines) {
			TransitLine reducedLine = reduceLine(originalLine, factory);

			if (reducedLine != null) {
				schedule.addTransitLine(reducedLine);
			}
		}

		List<TransitStopFacility> originalFacilities = new LinkedList<>(schedule.getFacilities().values());
		Set<TransitStopFacility> reducedFacilities = new HashSet<>();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					reducedFacilities.add(stop.getStopFacility());
				}
			}
		}

		originalFacilities.forEach(schedule::removeStopFacility);
		reducedFacilities.forEach(schedule::addStopFacility);

		MinimalTransferTimes minimalTransferTimes = schedule.getMinimalTransferTimes();
		MinimalTransferTimesIterator iterator = minimalTransferTimes.iterator();
		Set<Id<TransitStopFacility>> reducedIds = reducedFacilities.stream().map(f -> f.getId())
				.collect(Collectors.toSet());

		Set<Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>> removeRelations = new HashSet<>();

		while (iterator.hasNext()) {
			iterator.next();

			if (!reducedIds.contains(iterator.getFromStopId()) || !reducedIds.contains(iterator.getToStopId())) {
				removeRelations.add(new Tuple<>(iterator.getFromStopId(), iterator.getToStopId()));
			}
		}

		removeRelations.forEach(t -> minimalTransferTimes.remove(t.getFirst(), t.getSecond()));

		ScheduleInfo finalInfo = getInfo(schedule);

		log.info("Finished cutting transit schedule.");
		log.info("  Before: " + originalInfo);
		log.info("   After: " + finalInfo);
	}

	private class ScheduleInfo {
		final public int numberOfLines;
		final public int numberOfRoutes;
		final public int numberOfStops;

		public ScheduleInfo(int numberOfLines, int numberOfRoutes, int numberOfStops) {
			this.numberOfLines = numberOfLines;
			this.numberOfRoutes = numberOfRoutes;
			this.numberOfStops = numberOfStops;
		}

		@Override
		public String toString() {
			return String.format("# Lines: % 6d, # Routes: % 6d, # Stops: % 6d", numberOfLines, numberOfRoutes,
					numberOfStops);
		}
	}

	private ScheduleInfo getInfo(TransitSchedule schedule) {
		int numberOfLines = schedule.getTransitLines().size();
		int numberOfRoutes = 0;
		int numberOfStops = schedule.getFacilities().size();

		for (TransitLine line : schedule.getTransitLines().values()) {
			numberOfRoutes += line.getRoutes().size();
		}

		return new ScheduleInfo(numberOfLines, numberOfRoutes, numberOfStops);
	}
}
