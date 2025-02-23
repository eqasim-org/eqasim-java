package org.eqasim.core.simulation.modes.drt.mode_choice.constraints;

import com.google.inject.Inject;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DrtServiceAreaConstraint extends AbstractTripConstraint {

    private final Map<String, ScenarioExtent> scenarioExtentByMode;

    public DrtServiceAreaConstraint(Map<String, ScenarioExtent> scenarioExtentByMode) {
        this.scenarioExtentByMode = scenarioExtentByMode;
    }

    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        ScenarioExtent scenarioExtent = scenarioExtentByMode.get(mode);
        if(scenarioExtent != null) {
            return scenarioExtent.isInside(trip.getOriginActivity().getCoord()) && scenarioExtent.isInside(trip.getDestinationActivity().getCoord());
        }
        return true;
    }



    public static class Factory implements TripConstraintFactory {

        private final Map<String, ScenarioExtent> scenarioExtentByMode = new HashMap<>();

        @Inject
        public Factory(Config config) {
            if(config.getModules().containsKey(MultiModeDrtConfigGroup.GROUP_NAME)) {
                MultiModeDrtConfigGroup multiModeDrtConfigGroup = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
                multiModeDrtConfigGroup.getModalElements().stream()
                        .filter(drtConfigGroup -> drtConfigGroup.operationalScheme.equals(DrtConfigGroup.OperationalScheme.serviceAreaBased))
                        .forEach(drtConfigGroup -> {
                            String extentPath = ConfigGroup.getInputFileURL(config.getContext(), drtConfigGroup.drtServiceAreaShapeFile).getPath();
                            try {
                                scenarioExtentByMode.put(drtConfigGroup.mode, new ShapeScenarioExtent.Builder(new File(extentPath), Optional.empty(), Optional.empty()).build());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> list, Collection<String> collection) {
            return new DrtServiceAreaConstraint(scenarioExtentByMode);
        }
    }
}
