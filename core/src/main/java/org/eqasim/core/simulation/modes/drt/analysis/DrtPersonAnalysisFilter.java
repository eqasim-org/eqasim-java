package org.eqasim.core.simulation.modes.drt.analysis;

import com.google.inject.Inject;
import org.eqasim.core.analysis.DefaultPersonAnalysisFilter;
import org.eqasim.core.simulation.modes.drt.analysis.utils.VehicleRegistry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

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

