package org.eqasim.ile_de_france.super_blocks.handlers;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.ile_de_france.super_blocks.defs.SuperBlock;
import org.eqasim.ile_de_france.super_blocks.defs.SuperBlocksLogic;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.population.Person;

import java.util.ArrayList;
import java.util.List;

public class SuperblockViolationHandler implements LinkEnterEventHandler {

    private final Logger logger = LogManager.getLogger(SuperblockViolationHandler.class);

    private final SuperBlocksLogic superBlocksLogic;
    private final List<Event> superblockViolationEvents;

    @Inject
    public SuperblockViolationHandler(SuperBlocksLogic superBlocksLogic) {
        this.superBlocksLogic = superBlocksLogic;
        this.superblockViolationEvents = new ArrayList<>();
    }


    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<SuperBlock> superBlockId = this.superBlocksLogic.getSuperBlockByLink().get(event.getLinkId());
        if(superBlockId != null) {
            Id<Person> personId = Id.createPersonId(event.getVehicleId());
            IdSet<SuperBlock> superBlockIdSet = this.superBlocksLogic.getSuperBlocksByPerson().get(personId);
            if(superBlockIdSet == null || !superBlockIdSet.contains(superBlockId)) {
                this.logger.warn(String.format("illegal access to superblock %s by person %s through link %s at time %f", superBlockId, personId, event.getLinkId(), event.getTime()));
                this.superblockViolationEvents.add(event);
            }
        }
    }

    public void reset(int iteration) {
    }
}
