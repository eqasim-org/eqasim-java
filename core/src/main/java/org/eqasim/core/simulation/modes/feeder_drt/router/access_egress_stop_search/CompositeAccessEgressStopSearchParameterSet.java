package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_search;

import org.eqasim.core.simulation.modes.feeder_drt.config.AccessEgressStopSearchParams;
import org.matsim.core.config.ConfigGroup;

import java.util.Collection;
import java.util.List;

public class CompositeAccessEgressStopSearchParameterSet extends AccessEgressStopSearchParams  {

    public static final String NAME = "CompositeAccessEgressStopSearchParameterSet";

    public CompositeAccessEgressStopSearchParameterSet() {
        super(NAME);
    }

    @Override
    public ConfigGroup createParameterSet(String name) {
        if(NAME.equals(name)) {
            return new CompositeAccessEgressStopSearchParameterSet();
        } else if(TransitStopByModeAccessEgressStopSearchParameterSet.NAME.equals(name)) {
            return new TransitStopByModeAccessEgressStopSearchParameterSet();
        } else if(TransitStopByIdAccessEgressStopSearchParameterSet.NAME.equals(name)) {
            return new TransitStopByIdAccessEgressStopSearchParameterSet();
        } else {
            throw new IllegalStateException(String.format("Only '%s', '%s' and '%s' parameter sets are allowed in '%s'", TransitStopByModeAccessEgressStopSearchParameterSet.NAME, TransitStopByIdAccessEgressStopSearchParameterSet.NAME, NAME, NAME));
        }
    }

    @Override
    public void addParameterSet(ConfigGroup parameterSet) {
        if(parameterSet instanceof AccessEgressStopSearchParams) {
            super.addParameterSet(parameterSet);
        } else {
            throw new IllegalStateException(String.format("Only '%s', '%s' and '%s' parameter sets are allowed in '%s'", TransitStopByModeAccessEgressStopSearchParameterSet.NAME, TransitStopByIdAccessEgressStopSearchParameterSet.NAME, NAME, NAME));
        }
    }

    public List<AccessEgressStopSearchParams> getDelegateAccessEgressStopSearchParamSets() {
        return this.getParameterSets().values().stream().flatMap(Collection::stream).map(configGroup -> (AccessEgressStopSearchParams) configGroup).toList();
    }
}
