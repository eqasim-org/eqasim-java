package org.eqasim.ile_de_france.probing;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.VariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.VariablePredictorWithPreviousTrips;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PredictionWriter {
    private final Population population;
    private final TripRouter tripRouter;
    private final ActivityFacilities facilities;
    private final File outputPath;

    private final Set<String> modes = new HashSet<>();
    private final List<PredictorEntry<?>> predictorEntries = new LinkedList<>();
    private final List<PredictorEntryWithPreviousTrip<?>> predictorEntriesWithPreviousTrips = new LinkedList<>();

    public PredictionWriter(Population population, TripRouter tripRouter, ActivityFacilities facilities,
            File outputPath) {
        this.population = population;
        this.tripRouter = tripRouter;
        this.facilities = facilities;
        this.outputPath = outputPath;
    }

    public <T extends BaseVariables> PredictionWriter addPredictor(String name, String mode,
            VariablePredictor<T> predictor) {
        predictorEntries.add(new PredictorEntry<>(name, mode, predictor));
        modes.add(mode);
        return this;
    }

    public <T extends BaseVariables> PredictionWriter addPredictor(String name, VariablePredictor<T> predictor) {
        predictorEntries.add(new PredictorEntry<>(name, null, predictor));
        return this;
    }

    public <T extends BaseVariables> PredictionWriter addPredictor(String name, String mode,
            VariablePredictorWithPreviousTrips<T> predictor) {
        predictorEntriesWithPreviousTrips.add(new PredictorEntryWithPreviousTrip<>(name, mode, predictor));
        return this;
    }

    public <T extends BaseVariables> PredictionWriter addPredictor(String name,
            VariablePredictorWithPreviousTrips<T> predictor) {
        predictorEntriesWithPreviousTrips.add(new PredictorEntryWithPreviousTrip<>(name, null, predictor));
        return this;
    }

    public void run() {
        List<PersonEntry> result = new LinkedList<>();

        ParallelProgress progress = new ParallelProgress("Writing predictions ...", population.getPersons().size());
        progress.start();

        for (Person person : population.getPersons().values()) {
            DiscreteModeChoiceTrip trip = new TripListConverter().convert(person.getSelectedPlan()).get(0);
            trip.setDepartureTime(trip.getOriginActivity().getEndTime().seconds());

            Facility originFacility = FacilitiesUtils.toFacility(trip.getOriginActivity(), facilities);
            Facility destinationFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), facilities);

            List<PredictionEntry<?>> predictions = new LinkedList<>();

            for (String mode : modes) {
                List<? extends PlanElement> tripElements = tripRouter.calcRoute(mode, originFacility,
                        destinationFacility,
                        trip.getDepartureTime(), person, trip.getTripAttributes());

                for (var entry : predictorEntries) {
                    if (mode.equals(entry.mode)) {
                        predictions.add(new PredictionEntry<>(entry.name, entry.mode,
                                entry.predictor.predictVariables(person, trip, tripElements)));
                    }
                }

                for (var entry : predictorEntriesWithPreviousTrips) {
                    if (mode.equals(entry.mode)) {
                        predictions.add(new PredictionEntry<>(entry.name, entry.mode,
                                entry.predictor.predictVariables(person, trip, tripElements, Collections.emptyList())));
                    }
                }
            }

            for (var entry : predictorEntries) {
                if (entry.mode == null) {
                    predictions.add(new PredictionEntry<>(entry.name, null,
                            entry.predictor.predictVariables(person, trip, null)));
                }
            }

            for (var entry : predictorEntriesWithPreviousTrips) {
                if (entry.mode == null) {
                    predictions.add(new PredictionEntry<>(entry.name, null,
                            entry.predictor.predictVariables(person, trip, null, null)));
                }
            }

            result.add(new PersonEntry(person.getId().toString(), predictions));
            progress.update(1);
        }

        try {
            new ObjectMapper().writeValue(outputPath, result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            progress.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public record PredictorEntryWithPreviousTrip<T extends BaseVariables>(
            String name, String mode, VariablePredictorWithPreviousTrips<T> predictor) {
    }

    public record PredictorEntry<T extends BaseVariables>(
            String name, String mode, VariablePredictor<T> predictor) {
    }

    public record PredictionEntry<T extends BaseVariables>(
            String name, String mode, T variables) {
    }

    public record PersonEntry(String personId, List<PredictionEntry<?>> predictions) {
    }
}
