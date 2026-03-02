package org.eqasim.core.simulation.analysis.travel_time;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

public class TravelTimeComparisionListener
        implements IterationStartsListener, IterationEndsListener, PersonDepartureEventHandler,
        PersonArrivalEventHandler {
    static public final String DETAILED_OUTPUT_NAME = "detailed_travel_time_comparison.csv";
    static public final String HOURLY_OUTPUT_NAME = "hourly_travel_time_comparison.csv";
    static public final String OVERALL_OUTPUT_NAME = "travel_time_comparison.csv";

    private final Population population;
    private final TimeInterpretation timeInterpretation;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final EventsManager eventsManager;

    private final int analysisInterval;
    private final int detailedAnalysisInterval;

    private final Set<String> modes;

    private final Map<String, IdMap<Person, List<FinishedLegItem>>> trackedTimes = new HashMap<>();
    private final IdMap<Person, OngoingLegItem> ongoing = new IdMap<>(Person.class);

    public TravelTimeComparisionListener(Population population, TimeInterpretation timeInterpretation,
            OutputDirectoryHierarchy outputDirectoryHierarchy, EventsManager eventsManager, int analysisInterval,
            int detailedAnalysisInterval, Set<String> modes) {
        this.population = population;
        this.timeInterpretation = timeInterpretation;
        this.outputHierarchy = outputDirectoryHierarchy;
        this.eventsManager = eventsManager;
        this.analysisInterval = analysisInterval;
        this.detailedAnalysisInterval = detailedAnalysisInterval;
        this.modes = modes;
    }

    private record OngoingLegItem(String mode, double departureTime) {
    }

    private record FinishedLegItem(double departureTime, double travelTime) {
    }

    private List<FinishedLegItem> getList(String mode, Id<Person> personId) {
        return trackedTimes.computeIfAbsent(mode, m -> new IdMap<>(Person.class)).computeIfAbsent(personId,
                p -> new LinkedList<>());
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        trackedTimes.clear();
        ongoing.clear();

        if (analysisInterval > 0 && (event.getIteration() % analysisInterval == 0 || event.isLastIteration())) {
            eventsManager.addHandler(this);
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (modes.contains(event.getLegMode())) {
            ongoing.put(event.getPersonId(), new OngoingLegItem(event.getLegMode(), event.getTime()));
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (modes.contains(event.getLegMode())) {
            OngoingLegItem item = ongoing.remove(event.getPersonId());
            getList(item.mode, event.getPersonId())
                    .add(new FinishedLegItem(item.departureTime, event.getTime() - item.departureTime));
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        try {
            if (analysisInterval > 0 && (event.getIteration() % analysisInterval == 0 || event.isLastIteration())) {
                eventsManager.removeHandler(this);

                Map<String, DescriptiveStatistics> overallSummary = new HashMap<>();
                Map<String, DescriptiveStatistics> recentOverallSummary = new HashMap<>();

                Map<String, List<DescriptiveStatistics>> hourlySummary = new HashMap<>();
                Map<String, List<DescriptiveStatistics>> recentHourlySummary = new HashMap<>();

                for (String mode : modes) {
                    overallSummary.put(mode, new DescriptiveStatistics());
                    recentOverallSummary.put(mode, new DescriptiveStatistics());

                    hourlySummary.put(mode, new LinkedList<>());
                    recentHourlySummary.put(mode, new LinkedList<>());

                    for (int hour = 0; hour < 24; hour++) {
                        hourlySummary.get(mode).add(new DescriptiveStatistics());
                        recentHourlySummary.get(mode).add(new DescriptiveStatistics());
                    }
                }

                boolean writeDetails = detailedAnalysisInterval > 0
                        && (event.getIteration() % detailedAnalysisInterval == 0 || event.isLastIteration());

                writeDetails = true;

                BufferedWriter detailsWriter = writeDetails ? IOUtils
                        .getBufferedWriter(
                                outputHierarchy.getIterationFilename(event.getIteration(), DETAILED_OUTPUT_NAME))
                        : null;

                if (detailsWriter != null) {
                    detailsWriter.write(String.join(";", new String[] { //
                            "person_id", "leg_index", "mode", "planned_departure_time", "planned_travel_time",
                            "tracked_departure_time", "tracked_travel_time", "planned_age"
                    }) + "\n");
                }

                for (Person person : population.getPersons().values()) {
                    TimeTracker timeTracker = new TimeTracker(timeInterpretation);
                    int legIndex = 0;

                    Plan plan = person.getSelectedPlan();

                    // START TODO: This can be much simplified if Plan.getIterationCreated works by
                    // default: https://github.com/matsim-org/matsim-libs/issues/4762

                    Integer planHash = (Integer) plan.getAttributes().getAttribute("travelTimeHash");
                    Integer planIteration = (Integer) plan.getAttributes().getAttribute("travelTimeIteration");

                    final int age;
                    if (planHash == null || planHash != plan.hashCode()) {
                        age = 0;
                        plan.getAttributes().putAttribute("travelTimeHash", plan.hashCode());
                        plan.getAttributes().putAttribute("travelTimeIteration", event.getIteration());
                    } else {
                        age = event.getIteration() - planIteration;
                    }

                    // END TODO

                    boolean isRecent = age == 0;

                    for (PlanElement element : plan.getPlanElements()) {
                        if (element instanceof Leg leg) {
                            if (modes.contains(leg.getMode())) {
                                List<FinishedLegItem> finished = getList(leg.getMode(), person.getId());

                                double trackedDepartureTime = Double.NaN;
                                double trackedTravelTime = Double.NaN;

                                if (legIndex < finished.size()) {
                                    FinishedLegItem item = finished.get(legIndex);
                                    trackedDepartureTime = item.departureTime;
                                    trackedTravelTime = item.travelTime;
                                }

                                double plannedDepartureTime = timeTracker.getTime().seconds();
                                double plannedTravelTime = leg.getTravelTime().seconds();

                                if (detailsWriter != null) {
                                    detailsWriter.write(String.join(";", new String[] { //
                                            person.getId().toString(), //
                                            String.valueOf(legIndex), //
                                            leg.getMode(), //
                                            String.valueOf(plannedDepartureTime), //
                                            String.valueOf(plannedTravelTime), //
                                            String.valueOf(trackedDepartureTime), //
                                            String.valueOf(trackedTravelTime), //
                                            String.valueOf(age) //
                                    }) + "\n");
                                }

                                if (plannedTravelTime > 0.0 || trackedTravelTime > 0.0) {
                                    int hour = (int) Math.floor(plannedDepartureTime / 3600.0);
                                    if (hour < 24 && hour >= 0) {
                                        hourlySummary.get(leg.getMode()).get(hour)
                                                .addValue(plannedTravelTime - trackedTravelTime);

                                        if (isRecent) {
                                            recentHourlySummary.get(leg.getMode()).get(hour)
                                                    .addValue(plannedTravelTime - trackedTravelTime);
                                        }
                                    }

                                    overallSummary.get(leg.getMode()).addValue(plannedTravelTime - trackedTravelTime);

                                    if (isRecent) {
                                        recentOverallSummary.get(leg.getMode())
                                                .addValue(plannedTravelTime - trackedTravelTime);
                                    }
                                }
                            }

                            legIndex++;
                        }

                        timeTracker.addElement(element);
                    }
                }

                if (detailsWriter != null) {
                    detailsWriter.close();
                }

                BufferedWriter hourlyWriter = IOUtils.getAppendingBufferedWriter(
                        outputHierarchy.getIterationFilename(event.getIteration(), HOURLY_OUTPUT_NAME));

                hourlyWriter.write(String.join(";", new String[] {
                        "hour", "mode", "obs", "mean", "median", "q10", "q90", "std", "is_recent"
                }) + "\n");

                for (String mode : modes) {
                    for (int hour = 0; hour < 24; hour++) {
                        DescriptiveStatistics summary = hourlySummary.get(mode).get(hour);

                        hourlyWriter.write(String.join(";", new String[] {
                                String.valueOf(hour), mode, //
                                String.valueOf(summary.getN()), //
                                String.valueOf(summary.getMean()), //
                                String.valueOf(summary.getPercentile(50)), //
                                String.valueOf(summary.getPercentile(10)), //
                                String.valueOf(summary.getPercentile(90)), //
                                String.valueOf(summary.getStandardDeviation()), //
                                "false" //
                        }) + "\n");

                        DescriptiveStatistics recentSummary = recentHourlySummary.get(mode).get(hour);

                        hourlyWriter.write(String.join(";", new String[] {
                                String.valueOf(hour), mode, //
                                String.valueOf(recentSummary.getN()), //
                                String.valueOf(recentSummary.getMean()), //
                                String.valueOf(recentSummary.getPercentile(50)), //
                                String.valueOf(recentSummary.getPercentile(10)), //
                                String.valueOf(recentSummary.getPercentile(90)), //
                                String.valueOf(recentSummary.getStandardDeviation()), //
                                "true" //
                        }) + "\n");
                    }
                }

                hourlyWriter.close();

                boolean writeHeader = !new File(outputHierarchy.getOutputFilename(OVERALL_OUTPUT_NAME)).exists();
                BufferedWriter overallWriter = IOUtils
                        .getAppendingBufferedWriter(outputHierarchy.getOutputFilename(OVERALL_OUTPUT_NAME));

                if (writeHeader) {
                    overallWriter.write(String.join(";", new String[] {
                            "iteration", "mode", "observations", "mean", "median", "q10", "q90", "std", "is_recent"
                    }) + "\n");
                }

                for (String mode : modes) {
                    DescriptiveStatistics summary = overallSummary.get(mode);

                    overallWriter.write(String.join(";", new String[] { //
                            String.valueOf(event.getIteration()), //
                            mode, //
                            String.valueOf(summary.getN()), //
                            String.valueOf(summary.getMean()), //
                            String.valueOf(summary.getPercentile(50)), //
                            String.valueOf(summary.getPercentile(10)), //
                            String.valueOf(summary.getPercentile(90)), //
                            String.valueOf(summary.getStandardDeviation()), //
                            "false" //
                    }) + "\n");

                    DescriptiveStatistics recentSummary = recentOverallSummary.get(mode);

                    overallWriter.write(String.join(";", new String[] { //
                            String.valueOf(event.getIteration()), //
                            mode, //
                            String.valueOf(recentSummary.getN()), //
                            String.valueOf(recentSummary.getMean()), //
                            String.valueOf(recentSummary.getPercentile(50)), //
                            String.valueOf(recentSummary.getPercentile(10)), //
                            String.valueOf(recentSummary.getPercentile(90)), //
                            String.valueOf(recentSummary.getStandardDeviation()), //
                            "true" //
                    }) + "\n");
                }

                overallWriter.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
