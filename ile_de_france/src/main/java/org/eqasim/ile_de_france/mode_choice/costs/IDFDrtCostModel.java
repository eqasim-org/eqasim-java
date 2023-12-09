package org.eqasim.ile_de_france.mode_choice.costs;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.config.Config;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IDFDrtCostModel implements CostModel {

    private final IDFCostParameters costParameters;
    private final Set<String> drtModes;

    @Inject
    public IDFDrtCostModel(Config config, IDFCostParameters costParameters) {
        this.costParameters = costParameters;
        if(config.getModules().containsKey(MultiModeDrtConfigGroup.GROUP_NAME)) {
            MultiModeDrtConfigGroup multiModeDrtConfigGroup = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
            drtModes = multiModeDrtConfigGroup.modes().collect(Collectors.toSet());
        } else {
            throw new IllegalStateException("The IDFDrt cost model cannot be used without a proper drt configuration");
        }
    }

    @Override
    public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double distance = elements.stream().filter(e -> e instanceof Leg).map(e -> (Leg) e).filter(l -> drtModes.contains(l.getMode())).map(l -> l.getRoute().getDistance()).mapToDouble(d -> d).sum();
        distance /= 1000;
        return this.costParameters.drtCost_EUR_km * distance;
    }
}
