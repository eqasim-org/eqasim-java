package org.eqasim.ile_de_france.super_blocks.handlers;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.ile_de_france.super_blocks.defs.SuperBlock;
import org.eqasim.ile_de_france.super_blocks.defs.SuperBlocksLogic;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import com.google.inject.Inject;
import com.opencsv.CSVWriter;

public class SuperblockViolationHandler implements LinkEnterEventHandler, ShutdownListener, IterationEndsListener {

    private final Logger logger = LogManager.getLogger(SuperblockViolationHandler.class);
    private final SuperBlocksLogic superBlocksLogic;
    private final List<Event> superblockViolationEvents;
    private final Map<Integer, Integer> violationsCountPerIteration;
    private final IdMap<Person, Integer> currentIterationViolationsPerPerson;
    private int count;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;

    @Inject
    public SuperblockViolationHandler(SuperBlocksLogic superBlocksLogic, OutputDirectoryHierarchy outputDirectoryHierarchy) {
        this.superBlocksLogic = superBlocksLogic;
        this.superblockViolationEvents = new ArrayList<>();
        this.violationsCountPerIteration = new LinkedHashMap<>();
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.count = -1;
        this.currentIterationViolationsPerPerson = new IdMap<>(Person.class);
    }


    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<SuperBlock> superBlockId = this.superBlocksLogic.getSuperBlockByLink().get(event.getLinkId());
        if(superBlockId != null) {
            Id<Person> personId = Id.createPersonId(event.getVehicleId());
            IdSet<SuperBlock> superBlockIdSet = this.superBlocksLogic.getSuperBlocksByPerson().get(personId);
            if(superBlockIdSet == null || !superBlockIdSet.contains(superBlockId)) {
                this.superblockViolationEvents.add(event);
                this.count++;
                this.currentIterationViolationsPerPerson.merge(Id.createPersonId(event.getVehicleId()), 1, Integer::sum);
            }
        }
    }

    private void writeCurrentIterationViolationsPerPerson(int iteration) {
        try {
            Writer writer = new FileWriter(this.outputDirectoryHierarchy.getIterationFilename(iteration, "superblock-violations_persons.csv"));
            CSVWriter csvWriter = new CSVWriter(writer, ';', '"', '"', "\n");
            csvWriter.writeNext(new String[]{"person_id", "violations"});
            this.currentIterationViolationsPerPerson.entrySet().stream()
                    .map(entry -> new String[]{entry.getKey().toString(), String.valueOf(entry.getValue())})
                    .forEach(csvWriter::writeNext);
            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.currentIterationViolationsPerPerson.clear();
    }

    private void writeViolationCountPerIteration() {
        Writer writer;
        try {
            writer = new FileWriter(this.outputDirectoryHierarchy.getOutputFilename("superblock_violation.csv"));
            CSVWriter csvWriter = new CSVWriter(writer, ';', '"', '"', "\n");
            csvWriter.writeNext(new String[]{"iteration", "violations"});
            this.violationsCountPerIteration.keySet().stream()
                    .map(it -> new String[]{String.valueOf(it), String.valueOf(this.violationsCountPerIteration.get(it))})
                    .forEach(csvWriter::writeNext);
            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset(int iteration) {
        if(iteration == 0) {
            return;
        }
        this.violationsCountPerIteration.put(iteration-1, this.count);
        this.count = 0;
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        Writer writer;
        try {
            writer = new FileWriter(this.outputDirectoryHierarchy.getOutputFilename("superblock_violation.csv"));
            CSVWriter csvWriter = new CSVWriter(writer, ';', '"', '"', "\n");
            csvWriter.writeNext(new String[]{"iteration", "violations"});
            this.violationsCountPerIteration.keySet().stream()
                    .map(iteration -> new String[]{String.valueOf(iteration), String.valueOf(this.violationsCountPerIteration.get(iteration))})
                    .forEach(csvWriter::writeNext);
            csvWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        this.writeViolationCountPerIteration();
        this.writeCurrentIterationViolationsPerPerson(event.getIteration());
    }
}
