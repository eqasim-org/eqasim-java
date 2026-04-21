package org.eqasim.core.simulation.vdf.engine;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import org.eqasim.core.simulation.vdf.FlowEquivalentProvider;
import org.eqasim.core.simulation.vdf.handlers.VDFTrafficHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * This class handles transit vehicles in the VDF-based traffic emulator. It is
 * more complex than normal vehicle traffic since the QSim/Netsim also handles
 * stop interaction of transit vehicles. This is emulate here by basically
 * quickly traversing the schedule of a transit vehicle. When not generating
 * events, this means that we can quickly go through the link sequence until the
 * next step without having to wait for the next time step.
 * 
 * @author sebhoerl / IRT SystemX
 */
public class VDFTransitEngine implements DepartureHandler, MobsimEngine {
    private final TraversalTime traversalTime;
    private final FlowEquivalentProvider flowEquivalentProvider;
    private final Network network;

    private final PriorityQueue<ActiveRecord> queue = new PriorityQueue<>(
            Comparator.comparing(a -> a.triggerTime));

    private final List<StopRecord> stopQueue = new LinkedList<>();
    private final List<ActiveRecord> arrivalQueue = new LinkedList<>();

    private InternalInterface internalInterface;

    private final VDFTrafficHandler handler;
    private final boolean generateNetworkEvents;
    private final double simulationStep;

    private EventsManager eventsManager;

    public VDFTransitEngine(TraversalTime traversalTime, FlowEquivalentProvider flowEquivalentProvider, Network network,
            VDFTrafficHandler handler, boolean generateNetworkEvents, double simulationStep) {
        this.traversalTime = traversalTime;
        this.network = network;
        this.handler = handler;
        this.generateNetworkEvents = generateNetworkEvents;
        this.simulationStep = simulationStep;
        this.flowEquivalentProvider = flowEquivalentProvider;
    }

    @Override
    public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
        // prepare the agent for processing in sim step
        ActiveRecord record = new ActiveRecord();
        record.agent = (TransitDriverAgent) agent;
        record.triggerTime = Double.NEGATIVE_INFINITY;
        record.distance = 0.0;

        if (generateNetworkEvents) {
            eventsManager.processEvent(
                    new PersonEntersVehicleEvent(now, agent.getId(), record.agent.getPlannedVehicleId()));

            eventsManager.processEvent(
                    new VehicleEntersTrafficEvent(now, record.agent.getId(), record.agent.getCurrentLinkId(),
                            record.agent.getPlannedVehicleId(), record.agent.getMode(), 1.0));
        }

        if (!queueStop(now, record) && !queueArrival(now, record)) {
            queue.add(record);
        }

        return true;
    }

    @Override
    public void doSimStep(double now) {
        // handle ongoing trips
        while (!queue.isEmpty() && queue.peek().triggerTime <= now) {
            ActiveRecord record = queue.poll();

            if (!queueStop(now, record) && !queueArrival(now, record)) {
                processRecord(now, record);
            }
        }

        // handle the stop queue
        Iterator<StopRecord> stopQueueIterator = stopQueue.iterator();
        while (stopQueueIterator.hasNext()) {
            StopRecord record = stopQueueIterator.next();

            TransitStopFacility stopFacility = record.active.agent.getNextTransitStop();
            double stopDuration = record.active.agent.handleTransitStop(stopFacility, now);

            if (stopDuration > 0.0) {
                // we stay on the stop
                record.endTime += stopDuration;
            } else if (record.endTime <= now) {
                // requeue agent for advancing the route
                if (!queueArrival(now, record.active)) {
                    queue.add(record.active);
                }

                stopQueueIterator.remove();
            }
        }

        // handle arrivals
        arrivalQueue.forEach(r -> processArrival(now, r));
        arrivalQueue.clear();
    }

    private boolean checkStop(double now, ActiveRecord record) {
        Id<Link> currentLinkId = record.agent.getCurrentLinkId();
        TransitStopFacility stopFacility = record.agent.getNextTransitStop();
        return stopFacility != null && stopFacility.getLinkId().equals(currentLinkId);
    }

    private boolean queueStop(double now, ActiveRecord record) {
        if (checkStop(now, record)) {
            // agent wants to do a stop (possibly on the last link of the route)
            StopRecord stop = new StopRecord();
            stop.active = record;
            stop.endTime = Double.NEGATIVE_INFINITY;
            stopQueue.add(stop);
            return true;
        }

        return false;
    }

    private boolean checkArrival(double now, ActiveRecord record) {
        return record.agent.isWantingToArriveOnCurrentLink();
    }

    private boolean queueArrival(double now, ActiveRecord record) {
        if (checkArrival(now, record)) {
            // agent wants to arrive
            arrivalQueue.add(record);
            return true;
        }

        return false;
    }

    private void processRecord(double now, ActiveRecord record) {
        while (true) {
            if (checkArrival(now, record)) {

            }

            Id<Link> currentLinkId = record.agent.getCurrentLinkId();

            // we traverse the route
            Id<Link> nextLinkId = record.agent.chooseNextLinkId();
            record.agent.notifyMoveOverNode(nextLinkId);
            handler.processEnterLink(now, nextLinkId,
                    flowEquivalentProvider.getFlowEquivalent(record.agent.getPlannedVehicleId()));

            if (generateNetworkEvents) {
                eventsManager.processEvent(
                        new LinkLeaveEvent(now, record.agent.getPlannedVehicleId(),
                                currentLinkId));

                eventsManager.processEvent(
                        new LinkEnterEvent(now, record.agent.getPlannedVehicleId(), nextLinkId));
            }

            // prepare next trigger
            record.distance += network.getLinks().get(nextLinkId).getLength();

            // advance in time
            now += traversalTime.getTraversalTime(record.triggerTime, nextLinkId, record.agent);

            if (checkStop(now, record) || checkArrival(now, record)) {
                record.triggerTime = now;
                queue.add(record);
                return; // will be picked up after the traversal time
            } else if (generateNetworkEvents) {
                record.triggerTime = now + simulationStep;
                queue.add(record);
                return; // stop traversing
            }

            now += simulationStep; // qsim correction
        }
    }

    private void processArrival(double now, ActiveRecord record) {
        // standard procedure of handling a leg end
        // a transit agent may now do a turn and start another leg

        if (generateNetworkEvents) {
            eventsManager
                    .processEvent(new VehicleLeavesTrafficEvent(now, record.agent.getId(),
                            record.agent.getCurrentLinkId(),
                            Id.createVehicleId(record.agent.getId()), record.agent.getMode(), 1.0));

            eventsManager.processEvent(new PersonLeavesVehicleEvent(now, record.agent.getId(),
                    Id.createVehicleId(record.agent.getId())));
        } else {
            eventsManager.processEvent(new TeleportationArrivalEvent(now, record.agent.getId(),
                    record.distance, record.agent.getMode()));

            record.agent.notifyArrivalOnLinkByNonNetworkMode(record.agent.getCurrentLinkId());
        }

        record.agent.endLegAndComputeNextState(now);
        internalInterface.arrangeNextAgentState(record.agent);
    }

    @Override
    public void onPrepareSim() {
        // Nothing to do
    }

    @Override
    public void afterSim() {
        // not sure if having transit agents stuck is a thing in standard MATSim
        // we do it anyways here

        double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

        for (ActiveRecord record : queue) {
            eventsManager.processEvent(new PersonStuckEvent(now, record.agent.getId(),
                    record.agent.getCurrentLinkId(), record.agent.getMode()));
        }
    }

    @Override
    public void setInternalInterface(InternalInterface internalInterface) {
        this.internalInterface = internalInterface;
        this.eventsManager = internalInterface.getMobsim().getEventsManager();
    }

    private class ActiveRecord {
        TransitDriverAgent agent;
        double triggerTime;
        double distance;
    }

    private class StopRecord {
        ActiveRecord active;
        double endTime;
    }
}
