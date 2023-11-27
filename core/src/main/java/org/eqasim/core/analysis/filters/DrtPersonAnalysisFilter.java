package org.eqasim.core.analysis.filters;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.eqasim.core.analysis.utils.VehicleRegistry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.config.Config;
import org.matsim.core.modal.ModalAnnotationCreator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DrtPersonAnalysisFilter extends DefaultPersonAnalysisFilter {

    private final VehicleRegistry vehicleRegistry;

    @Inject
    public DrtPersonAnalysisFilter(VehicleRegistry vehicleRegistry) {
        this.vehicleRegistry = vehicleRegistry;
    }

    @Override
    public boolean analyzePerson(Id<Person> personId) {
        if(this.vehicleRegistry.isFleet(personId)) {
            return false;
        }
        return super.analyzePerson(personId);
    }
}

