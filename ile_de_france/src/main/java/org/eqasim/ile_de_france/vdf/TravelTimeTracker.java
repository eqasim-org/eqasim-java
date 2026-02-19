package org.eqasim.ile_de_france.vdf;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import com.google.common.base.Verify;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class TravelTimeTracker
        implements IterationStartsListener, IterationEndsListener, PersonDepartureEventHandler,
        PersonArrivalEventHandler {
    private final IdMap<Person, Double> departureTimes = new IdMap<>(Person.class);
    private final IdMap<Person, List<MeasureLeg>> measuredLegs = new IdMap<>(Person.class);

    private record MeasureLeg(double departureTime, double travelTime) {
    }

    private final EventsManager eventsManager;
    private final Population population;
    private final TimeInterpretation timeInterpretation;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final CompressionType compressionType;

    public TravelTimeTracker(EventsManager eventsManager, Population population,
            TimeInterpretation timeInterpretation, OutputDirectoryHierarchy outputDirectoryHierarchy,
            CompressionType compressionType) {
        this.eventsManager = eventsManager;
        this.population = population;
        this.timeInterpretation = timeInterpretation;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.compressionType = compressionType;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        departureTimes.clear();
        measuredLegs.clear();
        eventsManager.addHandler(this);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        eventsManager.removeHandler(this);

        try {
            String outputPath = outputDirectoryHierarchy.getIterationFilename(event.getIteration(),
                    "travel_time_comparison.csv", compressionType);
            BufferedWriter writer = IOUtils.getBufferedWriter(outputPath);

            writer.write(String.join(",", new String[] {
                    "person_id", "car_leg_index", "planned_departure_time", "planned_travel_time",
                    "observed_departure_time", "observed_travel_time"
            }) + "\n");

            for (Person person : population.getPersons().values()) {
                TimeTracker timeTracker = new TimeTracker(timeInterpretation);

                int carLegIndex = 0;
                List<MeasureLeg> measured = measuredLegs.get(person.getId());

                for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                    if (element instanceof Leg leg && leg.getMode().equals(TransportMode.car)) {
                        double departureTime = timeTracker.getTime().seconds();
                        double travelTime = leg.getTravelTime().seconds();

                        MeasureLeg measuredLeg = null;
                        if (measured != null && carLegIndex < measured.size()) {
                            measuredLeg = measured.get(carLegIndex);
                        }

                        writer.write(String.join(",", new String[] {
                                person.getId().toString(), String.valueOf(carLegIndex), String.valueOf(departureTime),
                                String.valueOf(travelTime),
                                measuredLeg == null ? "null" : String.valueOf(measuredLeg.departureTime),
                                measuredLeg == null ? "null" : String.valueOf(measuredLeg.travelTime)
                        }) + "\n");

                        carLegIndex++;
                    }
                }
            }

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals(TransportMode.car)) {
            Verify.verify(departureTimes.put(event.getPersonId(), event.getTime()) == null);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getLegMode().equals(TransportMode.car)) {
            double departureTime = Objects.requireNonNull(departureTimes.remove(event.getPersonId()));
            double travelTime = event.getTime() - departureTime;

            measuredLegs.computeIfAbsent(event.getPersonId(), pid -> new LinkedList<>())
                    .add(new MeasureLeg(departureTime, travelTime));
        }
    }

    static public void install(Controller controller) {
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addControllerListenerBinding().to(TravelTimeTracker.class);
            }

            @Provides
            @Singleton
            TravelTimeTracker provideTravelTimeTracker(EventsManager eventsManager, Population population,
                    TimeInterpretation timeInterpretation, OutputDirectoryHierarchy outputDirectoryHierarchy,
                    ControllerConfigGroup config) {
                return new TravelTimeTracker(eventsManager, population, timeInterpretation, outputDirectoryHierarchy,
                        config.getCompressionType());
            }
        });
    }
}
