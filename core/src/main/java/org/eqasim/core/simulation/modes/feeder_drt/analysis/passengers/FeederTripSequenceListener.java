package org.eqasim.core.simulation.modes.feeder_drt.analysis.passengers;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.components.transit.events.PublicTransitEventMapper;
import org.eqasim.core.simulation.modes.drt.analysis.utils.VehicleRegistry;
import org.eqasim.core.simulation.modes.feeder_drt.config.FeederDrtConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeederTripSequenceListener implements PersonDepartureEventHandler, PersonArrivalEventHandler,
        PersonEntersVehicleEventHandler, ActivityEndEventHandler, GenericEventHandler, ActivityStartEventHandler {

    private final PublicTransitEventMapper publicTransitEventMapper = new PublicTransitEventMapper();
    private final VehicleRegistry vehicleRegistry;
    private final Network network;
    private final IdMap<Person, FeederTripSequenceItem> currentItems = new IdMap<>(Person.class);
    private final IdMap<Person, ActivityEndEvent> lastNonInteractionActivity = new IdMap<>(Person.class);
    private final IdMap<Person, List<String>> interactionActivitiesSequences = new IdMap<>(Person.class);
    private final IdMap<Person, Id<Vehicle>> lastPersonVehicles = new IdMap<>(Person.class);
    private final List<FeederTripSequenceItem> itemsList = new ArrayList<>();
    private final Map<String, FeederDrtConfigGroup> modeConfigs;


    public FeederTripSequenceListener(Map<String, FeederDrtConfigGroup> modeConfigs, VehicleRegistry vehicleRegistry, Network network) {
        this.vehicleRegistry = vehicleRegistry;
        this.network = network;
        this.modeConfigs = modeConfigs;
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        List<String> personInteractionActivitiesSequence = interactionActivitiesSequences.get(event.getPersonId());
        if(personInteractionActivitiesSequence == null) {
            personInteractionActivitiesSequence = new ArrayList<>();
            interactionActivitiesSequences.put(event.getPersonId(), personInteractionActivitiesSequence);
        }
        if(event.getActType().endsWith(" interaction")) {
            personInteractionActivitiesSequence.add(event.getActType());
        } else {
            personInteractionActivitiesSequence.clear();
            lastNonInteractionActivity.put(event.getPersonId(), event);
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        String routingMode = event.getRoutingMode();
        if(!this.modeConfigs.containsKey(routingMode)) {
            return;
        }
        FeederDrtConfigGroup currentModeConfig = this.modeConfigs.get(routingMode);
        FeederTripSequenceItem currentPersonItem = currentItems.get(event.getPersonId());
        String stageActivityType = currentModeConfig.mode + " interaction";
        if(event.getLegMode().equals(currentModeConfig.accessEgressModeName)) {
            if(currentPersonItem != null) {
                if(!interactionActivitiesSequences.get(event.getPersonId()).contains(stageActivityType)) {
                    throw new IllegalStateException(String.format("Found a drt trip for person %s with an existing trip before but no feeder interaction activity", event.getPersonId()));
                }
                if(currentPersonItem.egressTransitLineId == null) {
                    throw new IllegalStateException(String.format("Found a drt trip for person %s with an existing trip but without pt info. This means that two drt sub-trips appear one after the other", event.getPersonId()));
                }
                // So this drt trip will be considered to be the egress one
                currentPersonItem.egressDepartureTime = event.getTime();
            } else {
                currentPersonItem = new FeederTripSequenceItem();
                currentPersonItem.personId = event.getPersonId();
                currentPersonItem.accessDepartureTime = event.getTime();
                currentPersonItem.originLink = network.getLinks().get(this.lastNonInteractionActivity.get(event.getPersonId()).getLinkId());
                currentPersonItem.operator = routingMode;
                currentItems.put(event.getPersonId(), currentPersonItem);
            }
        } else if (event.getLegMode().equals(currentModeConfig.ptModeName)) {
            if(interactionActivitiesSequences.get(event.getPersonId()).contains(stageActivityType) && currentPersonItem == null) {
                throw new IllegalStateException(String.format("Found a pt trip following a feeder interaction activity but without a drt trip before for person %s", event.getPersonId()));
            }
            if(currentPersonItem == null) {
                currentPersonItem = new FeederTripSequenceItem();
                currentPersonItem.personId = event.getPersonId();
                currentPersonItem.originLink = network.getLinks().get(this.lastNonInteractionActivity.get(event.getPersonId()).getLinkId());
                currentPersonItem.operator = routingMode;
                currentItems.put(event.getPersonId(), currentPersonItem);
            }
            currentPersonItem.ptDepartureTime = event.getTime();
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        FeederTripSequenceItem currentPersonItem = currentItems.get(event.getPersonId());
        if(currentPersonItem == null) {
            return;
        }
        FeederDrtConfigGroup currentModeConfig = this.modeConfigs.get(currentPersonItem.operator);
        if(event.getLegMode().equals(currentModeConfig.ptModeName)) {
            currentPersonItem.ptArrivalTime = event.getTime();
        } else if(event.getLegMode().equals(currentModeConfig.accessEgressModeName)) {
            Id<Vehicle> lastPersonVehicle = this.lastPersonVehicles.get(event.getPersonId());
            if(lastPersonVehicle == null) {
                throw new IllegalStateException(String.format("A DRT trip ended and no drt vehicle recorded for person %s", event.getPersonId()));
            }
            if(Double.isNaN(currentPersonItem.accessDepartureTime) && Double.isNaN(currentPersonItem.egressDepartureTime)) {
                throw new IllegalStateException(String.format("Drt trip end detected while no access or egress departure time is set for person %s", event.getPersonId()));
            }
            if(!Double.isNaN(currentPersonItem.egressDepartureTime)) {
                currentPersonItem.egressArrivalTime = event.getTime();
                currentPersonItem.egressVehicleId = lastPersonVehicle;
            }
            else if(!Double.isNaN(currentPersonItem.accessDepartureTime)) {
                currentPersonItem.accessArrivalTime = event.getTime();
                currentPersonItem.accessVehicleId = lastPersonVehicle;
            }
        }
    }


    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if(vehicleRegistry.isFleet(event.getVehicleId())) {
            this.lastPersonVehicles.put(event.getPersonId(), event.getVehicleId());
        }
    }

    @Override
    public void handleEvent(GenericEvent event) {
        if(!event.getEventType().equals(PublicTransitEvent.TYPE)) {
            return;
        }
        PublicTransitEvent transitEvent = this.publicTransitEventMapper.apply(event);
        FeederTripSequenceItem personItem = this.currentItems.get(transitEvent.getPersonId());
        if(personItem != null) {
            if(personItem.accessTransitStopId == null) {
                personItem.accessTransitStopId = transitEvent.getAccessStopId();
                personItem.accessTransitRouteId = transitEvent.getTransitRouteId();
                personItem.accessTransitLineId = transitEvent.getTransitLineId();
            }
            personItem.egressTransitStopId = transitEvent.getEgressStopId();
            personItem.egressTransitRouteId = transitEvent.getTransitRouteId();
            personItem.egressTransitLineId = transitEvent.getTransitLineId();
        }

    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if(event.getActType().endsWith(" interaction")) {
            return;
        }
        Id<Person> personId = event.getPersonId();
        FeederTripSequenceItem item = currentItems.remove(personId);
        if(item == null) {
            return;
        }
        if(interactionActivitiesSequences.containsKey(personId) && !interactionActivitiesSequences.get(personId).contains(item.operator + " interaction")) {
            return;
        }
        if (Double.isNaN(item.accessDepartureTime) && Double.isNaN(item.egressDepartureTime)) {
            throw new IllegalStateException(String.format("Encountered a Feeder Sequence with no drt trips for person %s", event.getPersonId()));
        }
        item.destinationLink = network.getLinks().get(event.getLinkId());
        //There seems to be a bug with two drt trips following each other with no PT in the middle
        if(item.egressTransitLineId != null) {
            itemsList.add(item);
        }

    }

    @Override
    public void reset(int iteration) {
        this.currentItems.clear();
        this.itemsList.clear();
        this.interactionActivitiesSequences.clear();
        this.lastPersonVehicles.clear();
        this.lastNonInteractionActivity.clear();
    }

    public List<FeederTripSequenceItem> getItemsList() {
        return new ArrayList<>(this.itemsList);
    }
}
