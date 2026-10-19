package org.eqasim.core.simulation.dsim;

import com.google.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.eqasim.core.components.transit.departure.DepartureFinder;
import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.dsim.DistributedDepartureHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.dsim.simulation.AgentSourcesContainer;
import org.matsim.dsim.simulation.PartitionTransfer;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/** Unlimited-capacity timetable PT, including missed departures and partition transfers. */
public final class ScheduledTransitEngine implements DistributedDepartureHandler, DistributedMobsimEngine {
    private final EventsManager events;
    private final TransitSchedule schedule;
    private final DepartureFinder departures;
    private final PartitionTransfer transfer;
    private final AgentSourcesContainer sources;
    private final PriorityQueue<Arrival> arrivals = new PriorityQueue<>(Comparator
            .comparingDouble(Arrival::time).thenComparing(a -> a.agent().getId()));
    private InternalInterface internal;
    private double now;

    @Inject public ScheduledTransitEngine(EventsManager events, TransitSchedule schedule, DepartureFinder departures,
            PartitionTransfer transfer, AgentSourcesContainer sources) {
        this.events = events;
        this.schedule = schedule;
        this.departures = departures;
        this.transfer = transfer;
        this.sources = sources;
    }

    @Override public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
        if (!"pt".equals(agent.getMode())) return false;
        var person = (DistributedMobsimAgent) agent;
        var leg = (Leg) ((PlanAgent) agent).getCurrentPlanElement();
        if (!(leg.getRoute() instanceof TransitPassengerRoute route)) {
            throw new IllegalArgumentException("Scheduled PT requires a TransitPassengerRoute: " + agent.getId());
        }
        if (route.getChainedRoute() != null) {
            throw new IllegalArgumentException("DSim timetable adapter does not support chained/interlined PT routes: "
                    + agent.getId());
        }
        var line = schedule.getTransitLines().get(route.getLineId());
        var service = line.getRoutes().get(route.getRouteId());
        try {
            var departure = departures.findNextDeparture(service, route.getAccessStopId(), route.getEgressStopId(), now);
            double boarding = departure.departure.getDepartureTime() + departure.stop.getDepartureOffset().seconds();
            int boardingIndex = service.getStops().indexOf(departure.stop);
            double arrival = Double.NaN;
            for (int i = boardingIndex + 1; i < service.getStops().size(); i++) {
                var stop = service.getStops().get(i);
                if (stop.getStopFacility().getId().equals(route.getEgressStopId())) {
                    arrival = departure.departure.getDepartureTime()
                            + stop.getArrivalOffset().orElse(stop.getDepartureOffset().orElse(Double.NaN));
                    break;
                }
            }
            if (!Double.isFinite(arrival) || arrival < boarding) {
                throw new IllegalArgumentException("Invalid PT stop sequence/offsets on " + service.getId());
            }
            // Cross-partition messages are delivered at the next simulation tick.
            arrival = Math.max(now + 1.0, arrival);
            var event = new PublicTransitEvent(arrival, agent.getId(), line.getId(), service.getId(),
                    route.getAccessStopId(), route.getEgressStopId(), departure.departure.getId(), boarding,
                    route.getDistance());
            if (transfer.isLocal(agent.getDestinationLinkId())) {
                arrivals.add(new Arrival(person, arrival, event));
            } else {
                int destination = transfer.getPartitionIndex(agent.getDestinationLinkId());
                internal.notifyAgentLeavesPartition(person, destination);
                transfer.collect(new TransitArrival(person.getClass(), person.toMessage(), arrival, event), destination);
            }
        } catch (DepartureFinder.NoDepartureFoundException e) {
            events.processEvent(new PersonStuckEvent(now, agent.getId(), linkId, "pt"));
        }
        return true;
    }

    @Override public Map<Class<? extends Message>, MessageHandler> getMessageHandlers() {
        return Map.of(TransitArrival.class, this::receive);
    }

    private void receive(List<Message> messages, double now) {
        for (Message message : messages) {
            var arrival = (TransitArrival) message;
            if (arrival.time() < now) throw new IllegalStateException("Late DSim transit transfer");
            var agent = sources.agentFromMessage(arrival.type(), arrival.agent());
            internal.notifyAgentEntersPartition(agent);
            arrivals.add(new Arrival(agent, arrival.time(), arrival.event()));
        }
    }

    @Override public void doSimStep(double now) {
        this.now = now;
        while (!arrivals.isEmpty() && arrivals.peek().time() <= now) {
            var arrival = arrivals.remove();
            var agent = arrival.agent();
            agent.notifyArrivalOnLinkByNonNetworkMode(agent.getDestinationLinkId());
            events.processEvent(new PublicTransitEvent(now, arrival.event()));
            events.processEvent(new TeleportationArrivalEvent(now, agent.getId(), arrival.event().getTravelDistance(), "pt"));
            agent.endLegAndComputeNextState(now);
            internal.arrangeNextAgentState(agent);
        }
    }

    @Override public void afterMobsim() {
        for (var arrival : arrivals) {
            events.processEvent(new PersonStuckEvent(now, arrival.agent().getId(),
                    arrival.agent().getDestinationLinkId(), "pt"));
        }
        arrivals.clear();
    }

    @Override public void setInternalInterface(InternalInterface internalInterface) { internal = internalInterface; }
    @Override public double priority() { return 10.0; }
    @Override public double getEnginePriority() { return -9.0; }

    private record Arrival(DistributedMobsimAgent agent, double time, PublicTransitEvent event) { }
    public record TransitArrival(Class<? extends DistributedMobsimAgent> type, Message agent,
            double time, PublicTransitEvent event) implements Message { }
}
